package com.xdyan.commons.xray;

/**
 * Decides whether an exception is an error of the client (error) or of the lambda (fault). The
 * default one only knows {@link ClientError}; quarkus-lambda-commons-http adds the 4xx of JAX-RS.
 */
public interface ErrorClassifier {

  boolean isClientError(Throwable error);
}
