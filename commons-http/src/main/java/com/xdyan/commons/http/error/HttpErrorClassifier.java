package com.xdyan.commons.http.error;

import com.xdyan.commons.xray.ClientError;
import com.xdyan.commons.xray.ErrorClassifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

/**
 * The 4xx (not found, validation, conflict) are errors of the client in X-Ray, not faults of the
 * lambda. Replaces the default ErrorClassifier of quarkus-lambda-commons-core.
 */
@ApplicationScoped
public class HttpErrorClassifier implements ErrorClassifier {

  @Override
  public boolean isClientError(Throwable error) {
    return error instanceof ClientError
        || error instanceof WebApplicationException wae && wae.getResponse().getStatus() < 500;
  }
}
