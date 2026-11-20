package com.xdyan.commons.http.error.provider;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class ValueInstantiationProvider implements ExceptionMapper<ValueInstantiationException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(ValueInstantiationException exception) {
    var errors = exception.getPath().stream().map(e -> {
      var error = new ErrorMessage.Detail();
      error.setMessage("The field " + e.getFieldName() + " " + exception.getCause().getMessage());
      error.setComponent(artifactId);
      return  error;
    }).toList();

    var status = BAD_REQUEST.getStatusCode();

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Validation error occurred");
    error.setCode(status);
    error.setErrors(errors);

    ErrorLog.log(status, exception);

    return Response.status(status)
        .entity(error)
        .build();
  }
}
