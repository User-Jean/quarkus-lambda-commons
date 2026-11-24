package com.xdyan.commons.http;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** xdyan.commons.http.* in the application.yaml of the app. */
@ConfigMapping(prefix = "xdyan.commons.http")
public interface HttpConfig {

  Log log();

  Openapi openapi();

  /** xdyan.commons.http.log.*: the request and response bodies in the log (CorrelationFilter). */
  interface Log {
    /**
     * Characters of the request and response bodies written to the log; the rest is cut. Don't send
     * in the bodies secrets or personal data that can't be in the logs.
     */
    @WithDefault("500")
    int maxBodyLength();
  }

  /** xdyan.commons.http.openapi.*: the contract of the app (ContractFirstCheck). */
  interface Openapi {
    /**
     * The HTTP lambdas are contract-first: in dev and tests the app doesn't start without
     * src/main/resources/openapi.yaml (ContractFirstCheck). false only for a lambda without a
     * contract, e.g. a webhook.
     */
    @WithDefault("true")
    boolean required();
  }
}
