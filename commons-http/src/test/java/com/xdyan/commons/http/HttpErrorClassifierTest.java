package com.xdyan.commons.http;

import com.xdyan.commons.http.error.ConflictException;
import com.xdyan.commons.xray.ErrorClassifier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With commons-http the ErrorClassifier of X-Ray is HttpErrorClassifier, not the default one of commons-core.
 */
@QuarkusTest
public class HttpErrorClassifierTest {

  @Inject
  ErrorClassifier errorClassifier;

  @Test
  @DisplayName("mark the 4xx as errors of the client and the rest as faults")
  public void classifyByStatus() {
    assertTrue(errorClassifier.isClientError(new NotFoundException()));
    assertTrue(errorClassifier.isClientError(new ConflictException("taken")));
    assertFalse(errorClassifier.isClientError(new InternalServerErrorException()));
    assertFalse(errorClassifier.isClientError(new IllegalStateException()));
  }
}
