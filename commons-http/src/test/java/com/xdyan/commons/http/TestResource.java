package com.xdyan.commons.http;

import com.xdyan.commons.http.error.ConflictException;
import com.xdyan.commons.http.error.InvalidFieldsException;
import com.xdyan.commons.http.mapper.DateTimeMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Endpoints of the tests only: each one throws one of the errors the library maps.
 */
@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResource {

  public record Body(@NotNull @Pattern(regexp = "^[a-z]+$") String name, @NotNull Integer unitPrice) {
  }

  @Inject
  DateTimeMapper dateTimeMapper;

  @GET
  @Path("/not-found")
  public String notFound() {
    throw new NotFoundException("Item not found");
  }

  @GET
  @Path("/conflict")
  public String conflict() {
    throw new ConflictException("An item named 'x' already exists");
  }

  @GET
  @Path("/bad-request")
  public String badRequest() {
    throw new BadRequestException("The field 'unit_price' must have at most 2 decimals");
  }

  @GET
  @Path("/invalid-fields")
  public String invalidFields() {
    throw new InvalidFieldsException(List.of("The field 'a' is required", "The field 'b' is required"));
  }

  @GET
  @Path("/boom")
  public String boom() {
    throw new IllegalStateException("database password is wrong");
  }

  @DELETE
  @Path("/no-content")
  public void noContent() {
  }

  @GET
  @Path("/date/{value}")
  public Map<String, String> date(@PathParam("value") String value) {
    return Map.of("date", dateTimeMapper.format(dateTimeMapper.parseDate(value.replace('-', '/'))));
  }

  @POST
  @Path("/body")
  public Body body(@Valid Body body) {
    return body;
  }
}
