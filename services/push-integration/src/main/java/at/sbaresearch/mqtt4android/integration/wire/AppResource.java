/*
 * Wire Push Integration for Push Adapter
 * Copyright (c) 2020 Harald Jagenteufel.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package at.sbaresearch.mqtt4android.integration.wire;

import at.sbaresearch.mqtt4android.integration.wire.AppResource.AppRegistrationRequest.Token;
import at.sbaresearch.mqtt4android.pinning.PinningSslFactory;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import lombok.*;
import lombok.ToString.Exclude;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;

@RestController
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class AppResource {

  RegistrationRepository registrationRepository;
  RestTemplateBuilder templateBuilder;

  @PostMapping(value = "/push/tokens",
      consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity postPush(@RequestBody AppRegistrationRequest reg,
      @RequestHeader HttpHeaders headers) {
    log.info("postPush called, req: {}", reg);

    if (reg.token.relayUrl != null && reg.token.relayCert != null) {
      registrationRepository.addDeviceRelay(reg.token.token,
          new RelayConnection(reg.token.relayUrl, fromBase64(reg.token.relayCert)));
    } else {
      log.warn("registration cancelled, relayUrl or cert is null!");
    }

    return handleGundeck(reg, headers);
  }


  private ResponseEntity<AppRegistrationRequest> handleGundeck(AppRegistrationRequest reg, HttpHeaders headers) {
    val request = new GundeckPushRequest(reg.token.token, reg.app, reg.transport, reg.client);

    val downstream = new HttpEntity<>(request, headers);
    val resp = templateBuilder.build().postForEntity(
        "http://gundeck:8086/push/tokens", downstream, GundeckPushRequest.class);
    log.info("** gundeck response headers: {}", resp.getHeaders());
    val body = buildCompatBody(resp, reg);

    return new ResponseEntity<>(body, resp.getHeaders(), resp.getStatusCode());
  }

  private AppRegistrationRequest buildCompatBody(ResponseEntity<GundeckPushRequest> resp, AppRegistrationRequest reg) {
    val body = resp.getBody();
    return new AppRegistrationRequest(new Token(body.token, reg.token.relayUrl, reg.token.relayCert), body.app, body.transport, body.client);
  }

  @GetMapping("/push/tokens")
  public ResponseEntity proxyGetPush(RequestEntity req) {
    log.info("proxyPush called {}", req);
    // TODO cache template, should be created only once
    // TODO gundeck config hardcoded here
    return templateBuilder.build()
        .exchange("http://gundeck:8086/push/tokens", req.getMethod(), req, Object.class);
  }

  @DeleteMapping("/push/tokens/{pid}")
  public ResponseEntity proxyDeletePush(RequestEntity req, @PathVariable String pid) {
    log.info("proxyPush called {}", req);
    // TODO cache template, should be created only once
    // TODO gundeck config hardcoded here
    return templateBuilder.build()
        .exchange("http://gundeck:8086/push/tokens/{pid}", req.getMethod(), req, Object.class, pid);
  }

  @PostMapping(value = "/send")
  public void sendMessage(@RequestBody PushRequest request) {
    val token = request.message.token;
    log.info("sending message for token: {}", token);
    registrationRepository.getDeviceRelay(token).peek(pushMessage(request))
        .onEmpty(() -> {
          log.error("not registered, cannot send message. req: {}", request);
          throw new NotRegisteredException();
        });
  }

  private Consumer<RelayConnection> pushMessage(PushRequest req) {
    return conn -> {
      try {
        val requestFactory = setupRequestFactory(conn.getRelayCert());
        templateBuilder
            .requestFactory(() -> requestFactory)
            .build()
            .postForLocation(conn.getRelayUrl(), req);
      } catch (Exception e) {
        log.error("cannot create ssl connection", e);
      }
    };
  }

  private HttpComponentsClientHttpRequestFactory setupRequestFactory(byte[] cert) throws Exception {
    val ssl = new PinningSslFactory(new ByteArrayInputStream(cert));

    val sslSF = new SSLConnectionSocketFactory(ssl.getSslContext(), NoopHostnameVerifier.INSTANCE);
    val httpClient = HttpClients.custom()
        .setSSLSocketFactory(sslSF)
        .build();

    val requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);
    return requestFactory;
  }

  private byte[] fromBase64(String relayCert) {
    return Base64.getDecoder().decode(relayCert);
  }

  @Value
  public static class AppRegistrationRequest {
    Token token;
    /*
    these are needed for gundeck compat
     */
    String app;
    String transport;
    String client;

    @Value
    public static class Token {
      String token;
      String relayUrl;
      @Exclude
      String relayCert;
    }
  }

  /**
   * compatible with gundeck POST /push/token
   */
  @Value
  @Builder
  public static class GundeckPushRequest {
    String token;
    String app;
    String transport;
    String client;
  }

  /**
   * included from relay
   */
  @Value
  @Builder
  public static class PushRequest {
    Boolean validate_only;
    Message message;

    @Value
    @Builder
    public static class Message {
      java.util.Map<String, String> data;
      String token;
    }
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public static class NotRegisteredException extends RuntimeException {
  }
}
