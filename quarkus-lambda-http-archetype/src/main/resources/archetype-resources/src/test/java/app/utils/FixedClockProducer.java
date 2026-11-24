package com.example.app.utils;

import io.quarkus.test.Mock;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Replaces the Clock of quarkus-lambda-commons in tests so "now" is always Monday 2025/12/15 12:00
 * in xdyan.commons.clock.zone.
 */
public class FixedClockProducer {

  public static final LocalDateTime NOW = LocalDateTime.of(2025, 12, 15, 12, 0);

  @Produces
  @Mock
  @Singleton
  Clock clock() {
    ZoneId zone = ZoneId.of("America/Lima");
    return Clock.fixed(NOW.atZone(zone).toInstant(), zone);
  }
}
