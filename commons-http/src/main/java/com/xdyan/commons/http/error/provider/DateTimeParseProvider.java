package com.xdyan.commons.http.error.provider;

import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.format.DateTimeParseException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Dates that match the pattern of the API but don't exist, like "2025-02-30 10:00:00".
 */
@Provider
public class DateTimeParseProvider implements ExceptionMapper<DateTimeParseException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(DateTimeParseException exception) {
    var errorDetail = new ErrorMessage.Detail();
    errorDetail.setMessage("The value '" + exception.getParsedString() + "' is not a valid date or hour");
    errorDetail.setComponent(artifactId);

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Validation error occurred");
    error.setCode(BAD_REQUEST.getStatusCode());
    error.setErrors(List.of(errorDetail));


    ErrorLog.log(BAD_REQUEST.getStatusCode(), exception);

    return Response.status(BAD_REQUEST)
        .entity(error)
        .build();
  }
}
