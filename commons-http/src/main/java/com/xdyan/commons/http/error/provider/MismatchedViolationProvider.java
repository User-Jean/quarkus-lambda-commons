package com.xdyan.commons.http.error.provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class MismatchedViolationProvider implements ExceptionMapper<MismatchedInputException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(MismatchedInputException exception) {
    var errors =
        exception.getPath().stream()
            .map(e -> {
              var error = new ErrorMessage.Detail();
              error.setMessage(
                  String.format(
                      "The field '%s' is mandatory or has invalid format", e.getFieldName()));
              error.setComponent(artifactId);
              return error;
            })
            .toList();

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Mismatched input failed");
    error.setCode(BAD_REQUEST.getStatusCode());
    error.setErrors(errors);

    ErrorLog.log(BAD_REQUEST.getStatusCode(), exception);

    return Response.status(BAD_REQUEST).entity(error).build();
  }
}
