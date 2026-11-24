package com.xdyan.commons.http.error.provider;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 404 with the message of the NotFoundException (e.g. "Item not found"). The 404 of RESTEasy itself
 * (no endpoint for the path, a path parameter that can't be converted, like /item/abc) has internal
 * details in its message (the whole URL, the parameter): the client gets "Resource not found" and
 * the log keeps the details.
 */
@Provider
public class NotFoundProvider implements ExceptionMapper<NotFoundException> {

  private static final String GENERIC_MESSAGE = "Resource not found";

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(NotFoundException exception) {
    var errorDetail = new ErrorMessage.Detail();
    errorDetail.setMessage(messageOf(exception));
    errorDetail.setComponent(artifactId);

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage(GENERIC_MESSAGE);
    error.setCode(NOT_FOUND.getStatusCode());
    error.setErrors(List.of(errorDetail));

    ErrorLog.log(NOT_FOUND.getStatusCode(), exception);

    return Response.status(NOT_FOUND).entity(error).build();
  }

  private static String messageOf(NotFoundException exception) {
    String message = exception.getMessage();
    if (message == null || message.startsWith("RESTEASY") || message.startsWith("HTTP 404")) {
      return GENERIC_MESSAGE;
    }
    return message;
  }
}
