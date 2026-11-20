package com.xdyan.commons.http.error.provider;

import com.xdyan.commons.http.error.ErrorLog;
import com.xdyan.commons.http.error.ErrorMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

import java.util.Locale;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class ResteasyViolationProvider implements ExceptionMapper<ResteasyViolationException> {

  @ConfigProperty(name = "quarkus.application.name")
  String artifactId;

  @Override
  public Response toResponse(ResteasyViolationException exception) {
    var errors = exception.getConstraintViolations().stream()
        .map(constraintViolation -> {
          var detail = new ErrorMessage.Detail();
          var field = extractField(constraintViolation.getPropertyPath().toString());
          detail.setMessage("The field '" + field + "' " + constraintViolation.getMessage());
          detail.setComponent(artifactId);
          return detail;
        }).toList();

    var error = new ErrorMessage();
    error.setName(artifactId);
    error.setMessage("Validation failed");
    error.setCode(BAD_REQUEST.getStatusCode());
    error.setErrors(errors);


    ErrorLog.log(BAD_REQUEST.getStatusCode(), exception);

    return Response.status(BAD_REQUEST)
        .entity(error)
        .build();
  }

  /**
   * "createItem.itemCreateRequest.unitPrice" → "unit_price", the name of the field in the JSON.
   */
  private String extractField(String path) {
    int firstDot = path.indexOf('.');
    int secondDot = path.indexOf('.', firstDot + 1);
    var field = secondDot < 0 ? path.substring(firstDot + 1) : path.substring(secondDot + 1);
    return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }
}
