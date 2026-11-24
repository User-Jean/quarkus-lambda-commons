package com.xdyan.commons.clock;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.ZoneId;

/** xdyan.commons.clock.* in the application.yaml of the app. */
@ConfigMapping(prefix = "xdyan.commons.clock")
public interface ClockConfig {

  /**
   * Zone of "now" and of the dates stored, while Lambda runs in UTC. An unknown zone fails at
   * startup.
   */
  @WithDefault("America/Lima")
  ZoneId zone();
}
