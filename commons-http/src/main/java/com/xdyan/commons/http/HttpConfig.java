package com.xdyan.commons.http;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * xdyan.commons.http.* in the application.yaml of the app.
 */
@ConfigMapping(prefix = "xdyan.commons.http")
public interface HttpConfig {

  Log log();

  interface Log {
    /**
     * Characters of the request and response bodies written to the log; the rest is cut. Don't send in the bodies
     * secrets or personal data that can't be in the logs.
     */
    @WithDefault("500")
    int maxBodyLength();
  }
}
