package com.xdyan.commons.http.error.provider;

import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Provider
public class NotFoundProvider implements ExceptionMapper<NotFoundException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(NotFoundException exception) {
    var errorDetail = new ErrorMessage.Detail();
    errorDetail.setMessage(exception.getMessage());
    errorDetail.setComponent(artifactId);

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Resource not found");
    error.setCode(NOT_FOUND.getStatusCode());
    error.setErrors(List.of(errorDetail));

    ErrorLog.log(NOT_FOUND.getStatusCode(), exception);

    return Response.status(NOT_FOUND)
        .entity(error)
        .build();
  }
}
