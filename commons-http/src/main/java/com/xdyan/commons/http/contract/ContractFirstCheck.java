package com.xdyan.commons.http.contract;

import com.xdyan.commons.http.HttpConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * The HTTP lambdas are contract-first: the API is written in src/main/resources/openapi.yaml and quarkus-lambda-parent
 * generates its JAX-RS interfaces and DTOs. In dev and tests an app with commons-http and without openapi.yaml doesn't
 * start, so its tests fail with this message. Not checked in the lambda (prod): the contract was already checked by the
 * tests, and the native image doesn't include the file. xdyan.commons.http.openapi.required=false turns it off.
 */
@ApplicationScoped
public class ContractFirstCheck {

  static final String CONTRACT = "openapi.yaml";

  void onStart(@Observes StartupEvent event, HttpConfig config) {
    if (LaunchMode.current() != LaunchMode.NORMAL) {
      check(Thread.currentThread().getContextClassLoader(), config.openapi().required());
    }
  }

  static void check(ClassLoader classLoader, boolean required) {
    if (required && classLoader.getResource(CONTRACT) == null) {
      throw new IllegalStateException("quarkus-lambda-commons-http is contract-first: add the API to "
          + "src/main/resources/" + CONTRACT + " (quarkus-lambda-parent generates its interfaces and DTOs), or set "
          + "xdyan.commons.http.openapi.required=false for a lambda without a contract");
    }
  }
}
