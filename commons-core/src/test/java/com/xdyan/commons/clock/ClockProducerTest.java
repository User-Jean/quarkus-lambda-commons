package com.xdyan.commons.clock;

import com.xdyan.commons.xray.ErrorClassifier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The beans of the library are found without declaring anything: this module is the "app" of the test.
 * src/test/resources/application.properties sets xdyan.commons.clock.zone.
 */
@QuarkusTest
public class ClockProducerTest {

  @Inject
  Clock clock;

  @Inject
  ErrorClassifier errorClassifier;

  @Test
  @DisplayName("produce the Clock with the zone of xdyan.commons.clock.zone")
  public void produceTheClockWithTheConfiguredZone() {
    assertEquals(ZoneId.of("America/Bogota"), clock.getZone());
  }

  @Test
  @DisplayName("treat any exception that is not a ClientError as a fault")
  public void treatOtherExceptionsAsFaults() {
    assertFalse(errorClassifier.isClientError(new IllegalStateException("boom")));
  }
}
