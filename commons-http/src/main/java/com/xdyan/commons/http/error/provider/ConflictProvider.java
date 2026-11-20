package com.xdyan.commons.http.error.provider;

import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import com.xdyan.commons.http.error.ConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;

@Provider
public class ConflictProvider implements ExceptionMapper<ConflictException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(ConflictException exception) {
    var errorDetail = new ErrorMessage.Detail();
    errorDetail.setMessage(exception.getMessage());
    errorDetail.setComponent(artifactId);

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Conflict with the current state");
    error.setCode(CONFLICT.getStatusCode());
    error.setErrors(List.of(errorDetail));

    ErrorLog.log(CONFLICT.getStatusCode(), exception);

    return Response.status(CONFLICT)
        .entity(error)
        .build();
  }
}
