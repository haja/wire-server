package at.sbaresearch.mqtt4android.integration.wire;

import at.sbaresearch.mqtt4android.integration.wire.AppResource.AppRegistrationRequest.Token;
import at.sbaresearch.mqtt4android.integration.wire.AppResource.PushRequest.Message;
import at.sbaresearch.mqtt4android.pinning.PinningSslFactory;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString.Exclude;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
public class AppResource {

  @NonFinal
  Map<String, Tuple2<String, byte[]>> registrations = HashMap.empty();
  RestTemplateBuilder templateBuilder;

  public AppResource(RestTemplateBuilder builder) {
    this.templateBuilder = builder;
  }

  @PostMapping(value = "/push/tokens",
      consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity postPush(@RequestBody AppRegistrationRequest reg,
      @RequestHeader HttpHeaders headers) {
    log.info("postPush called, req: {}", reg);

    // TODO store in DB
    if (reg.token.relayUrl != null && reg.token.relayCert != null) {
      registrations =
          registrations.put(reg.token.token, Tuple.of(reg.token.relayUrl, fromBase64(reg.token.relayCert)));
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

  @RequestMapping(value = "/send", method = RequestMethod.POST)
  public void sendMessage(@RequestBody String tokenJson) {
    val token = tokenJson.replace("\"", "");
    log.info("sending message for token: {}", token);
    registrations.get(token).peek(pushMessage(token))
        .onEmpty(() -> log.warn("not registered, cannot send message. registrations: {}", registrations));
  }

  private Consumer<Tuple2<String, byte[]>> pushMessage(String token) {
    return regTuple -> {
      val data = HashMap.of("message", "dummy");
      val req = createPushRequest(data, token);

      val cert = regTuple._2;
      try {
        val requestFactory = setupRequestFactory(cert);
        templateBuilder
            .requestFactory(() -> requestFactory)
            .build()
            .postForLocation(regTuple._1, req);
      } catch (Exception e) {
        log.error("cannot create ssl connection", e);
      }
    };
  }

  private PushRequest createPushRequest(HashMap<String, String> data, String token) {
    return new PushRequest(false, new Message(data.toJavaMap(), token));
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
}
