package com.xdyan.commons.clock;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

/**
 * The Clock of the app: inject it instead of calling LocalDateTime.now(), so the tests can fix
 * "now" with a {@code @Mock} producer. An app that produces its own Clock replaces this one
 * (@DefaultBean).
 */
@ApplicationScoped
public class ClockProducer {

  @Produces
  @Singleton
  @DefaultBean
  Clock clock(ClockConfig config) {
    return Clock.system(config.zone());
  }
}
