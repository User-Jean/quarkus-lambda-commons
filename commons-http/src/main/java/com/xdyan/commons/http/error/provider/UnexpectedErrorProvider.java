package com.xdyan.commons.http.error.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Every exception without a more specific provider, with the same body as the others:
 * - a WebApplicationException (e.g. 405, 415) keeps its status and message
 * - a Jackson exception thrown by the app is a 400 (a request body that is not JSON never gets here: RESTEasy answers
 *   it with its own 400 and logs it)
 * - anything else is a 500 whose body doesn't show the message of the exception (it can have internal details):
 *   the stack trace is in the log of the request
 */
@Provider
public class UnexpectedErrorProvider implements ExceptionMapper<Exception> {

  static final String INTERNAL_ERROR = "Unexpected error, see the log of the request";

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(Exception exception) {
    int status;
    String title;
    String message;
    if (exception instanceof WebApplicationException wae) {
      status = wae.getResponse().getStatus();
      title = status >= 500 ? "Internal server error" : "Request error";
      message = exception.getMessage();
    } else if (exception instanceof JsonProcessingException) {
      status = BAD_REQUEST.getStatusCode();
      title = "Validation error occurred";
      message = "The JSON is not valid";
    } else {
      status = INTERNAL_SERVER_ERROR.getStatusCode();
      title = "Internal server error";
      message = INTERNAL_ERROR;
    }

    var errorDetail = new ErrorMessage.Detail();
    errorDetail.setMessage(message);
    errorDetail.setComponent(artifactId);

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage(title);
    error.setCode(status);
    error.setErrors(List.of(errorDetail));

    ErrorLog.log(status, exception);

    return Response.status(status)
        .entity(error)
        .build();
  }
}
