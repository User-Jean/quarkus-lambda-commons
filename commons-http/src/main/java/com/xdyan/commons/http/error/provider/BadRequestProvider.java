package com.xdyan.commons.http.error.provider;

import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import com.xdyan.commons.http.error.InvalidFieldsException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@Provider
public class BadRequestProvider implements ExceptionMapper<BadRequestException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(BadRequestException exception) {
    // One error per field when there are several (InvalidFieldsException)
    List<String> messages = exception instanceof InvalidFieldsException invalidFields
        ? invalidFields.getMessages()
        : List.of(exception.getMessage());
    List<ErrorMessage.Detail> details = messages.stream().map(message -> {
      var errorDetail = new ErrorMessage.Detail();
      errorDetail.setMessage(message);
      errorDetail.setComponent(artifactId);
      return errorDetail;
    }).toList();

    var status = exception.getResponse().getStatus();

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Validation error occurred");
    error.setCode(status);
    error.setErrors(details);

    ErrorLog.log(status, exception);

    return Response.status(status)
        .entity(error)
        .build();
  }
}
