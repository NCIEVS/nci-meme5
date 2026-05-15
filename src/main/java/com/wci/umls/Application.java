/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls;

import java.io.File;

import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import com.wci.umls.server.rest.impl.ApiOriginFilter;
import com.wci.umls.server.rest.impl.SessionFactoryShutdownListener;
import com.wci.umls.server.rest.impl.TermServerApplication;
import com.wci.umls.server.rest.impl.UserActivityLoggingFilter;

/**
 * Spring Boot application entry point.
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class Application extends SpringBootServletInitializer {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(Application.class);

  /**
   * Application entry point.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    configureCatalinaBase();
    ConfigurableApplicationContext app = null;
    try {
      app = SpringApplication.run(Application.class, args);
    } catch (Exception e) {
      logger.error("Unexpected exception", e);
      int exitCode = SpringApplication.exit(app, () -> 1);
      System.exit(exitCode);
    }
  }

  /**
   * Provides a Tomcat-style base directory before Log4j initializes appenders.
   */
  private static void configureCatalinaBase() {
    if (System.getProperty("catalina.base") != null
        && !System.getProperty("catalina.base").isBlank()) {
      return;
    }
    String base = System.getenv("CATALINA_BASE");
    if (base == null || base.isBlank()) {
      base = System.getenv("APP_DIR");
    }
    if (base == null || base.isBlank()) {
      base = System.getProperty("user.dir");
    }
    System.setProperty("catalina.base", base);
    new File(base, "logs").mkdirs();
  }

  /**
   * Returns the Jersey resource configuration.
   *
   * @return the resource configuration
   * @throws Exception if the application cannot initialize
   */
  @Bean
  public ResourceConfig jerseyConfig() throws Exception {
    return new TermServerApplication()
        .property(ServletProperties.FILTER_FORWARD_ON_404, true);
  }

  /**
   * Registers the Hibernate shutdown listener used by the WAR deployment.
   *
   * @return the listener registration
   * @throws Exception if the listener cannot initialize
   */
  @Bean
  public ServletListenerRegistrationBean<SessionFactoryShutdownListener>
      sessionFactoryShutdownListener() throws Exception {
    return new ServletListenerRegistrationBean<>(new SessionFactoryShutdownListener());
  }

  /**
   * Registers the CORS filter.
   *
   * @return the filter registration
   */
  @Bean
  public FilterRegistrationBean<ApiOriginFilter> apiOriginFilter() {
    FilterRegistrationBean<ApiOriginFilter> bean =
        new FilterRegistrationBean<>(new ApiOriginFilter());
    bean.addUrlPatterns("/*");
    bean.setOrder(1);
    return bean;
  }

  /**
   * Registers the user activity logging filter.
   *
   * @return the filter registration
   */
  @Bean
  public FilterRegistrationBean<UserActivityLoggingFilter> userActivityLoggingFilter() {
    FilterRegistrationBean<UserActivityLoggingFilter> bean =
        new FilterRegistrationBean<>(new UserActivityLoggingFilter());
    bean.addUrlPatterns("/*");
    bean.setOrder(2);
    return bean;
  }

  /**
   * Customizes embedded Tomcat to match the reference Boot service behavior.
   *
   * @return the Tomcat customizer
   */
  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
    return factory -> factory.addConnectorCustomizers(connector -> {
      connector.setAllowBackslash(true);
      connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue());
    });
  }
}
