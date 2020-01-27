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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@SpringBootApplication
public class PushIntegrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(PushIntegrationApplication.class, args);
  }

  /* is this needed?
  @Bean
  public FilterRegistrationBean requestDumperFilter() {
    val registration = new FilterRegistrationBean();
    val requestDumperFilter = new RequestDumperFilter();
    registration.setFilter(requestDumperFilter);
    registration.addUrlPatterns("/*");
    return registration;
  }
  */

  @Bean
  public CommonsRequestLoggingFilter logFilter() {
    CommonsRequestLoggingFilter filter
        = new CommonsRequestLoggingFilter();
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(10000);
    filter.setIncludeHeaders(false);
    filter.setAfterMessagePrefix("REQUEST DATA : ");
    return filter;
  }
}
