package com.xdyan.commons.http.error;

import lombok.extern.slf4j.Slf4j;

/**
 * One line per error response, with the MDC of the request (requestId, trace_id, path): a 4xx is a
 * WARN with the exception and its message, a 5xx an ERROR with the stack trace. The response body
 * is logged apart by CorrelationFilter.
 */
@Slf4j
public final class ErrorLog {

  private ErrorLog() {}

  public static void log(int status, Throwable exception) {
    if (status >= 500) {
      log.error(
          "[ERROR {}] {}: {}",
          status,
          exception.getClass().getSimpleName(),
          exception.getMessage(),
          exception);
    } else {
      log.warn(
          "[ERROR {}] {}: {}",
          status,
          exception.getClass().getSimpleName(),
          exception.getMessage());
    }
  }
}
