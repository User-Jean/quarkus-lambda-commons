package com.xdyan.commons.http;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * xdyan.commons.http.* in the application.yaml of the app.
 */
@ConfigMapping(prefix = "xdyan.commons.http")
public interface HttpConfig {

  Log log();

  Openapi openapi();

  interface Log {
    /**
     * Characters of the request and response bodies written to the log; the rest is cut. Don't send in the bodies
     * secrets or personal data that can't be in the logs.
     */
    @WithDefault("500")
    int maxBodyLength();
  }

  interface Openapi {
    /**
     * The HTTP lambdas are contract-first: in dev and tests the app doesn't start without
     * src/main/resources/openapi.yaml (ContractFirstCheck). false only for a lambda without a contract, e.g. a webhook.
     */
    @WithDefault("true")
    boolean required();
  }
}
