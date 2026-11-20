package com.xdyan.commons.http.error;

import com.xdyan.commons.xray.ClientError;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/**
 * 409: the request is valid but clashes with the current state (e.g. a name already taken).
 */
public class ConflictException extends ClientErrorException implements ClientError {
  public ConflictException(String message) {
    super(message, Response.Status.CONFLICT);
  }
}
