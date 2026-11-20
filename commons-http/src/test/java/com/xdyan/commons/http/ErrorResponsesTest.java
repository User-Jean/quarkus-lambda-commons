package com.xdyan.commons.http;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

/**
 * The providers of the library are found by the app without declaring them. name and component are
 * quarkus.application.name: the artifactId of the app, here the one of this module.
 */
@QuarkusTest
public class ErrorResponsesTest {

  private static final String APP = "quarkus-lambda-commons-http";

  @Test
  @DisplayName("return 404 with the message of the NotFoundException")
  public void notFound() {
    given().when().get("/test/not-found").then()
        .statusCode(404)
        .body("name", equalTo(APP))
        .body("message", equalTo("Resource not found"))
        .body("code", equalTo(404))
        .body("errors[0].message", equalTo("Item not found"))
        .body("errors[0].component", equalTo(APP));
  }

  @Test
  @DisplayName("return 409 with the message of the ConflictException")
  public void conflict() {
    given().when().get("/test/conflict").then()
        .statusCode(409)
        .body("message", equalTo("Conflict with the current state"))
        .body("errors[0].message", equalTo("An item named 'x' already exists"));
  }

  @Test
  @DisplayName("return 400 with the message of the BadRequestException")
  public void badRequest() {
    given().when().get("/test/bad-request").then()
        .statusCode(400)
        .body("message", equalTo("Validation error occurred"))
        .body("errors[0].message", equalTo("The field 'unit_price' must have at most 2 decimals"));
  }

  @Test
  @DisplayName("return 400 with one error per field of the InvalidFieldsException")
  public void invalidFields() {
    given().when().get("/test/invalid-fields").then()
        .statusCode(400)
        .body("errors.message", contains("The field 'a' is required", "The field 'b' is required"));
  }

  @Test
  @DisplayName("return 400 with the field in snake_case when the bean validation fails")
  public void beanValidation() {
    given().contentType(JSON).body("{\"name\": \"<b>\", \"unitPrice\": 1}").when().post("/test/body").then()
        .statusCode(400)
        .body("message", equalTo("Validation failed"))
        .body("errors[0].message", startsWith("The field 'name' must match"));
  }

  @Test
  @DisplayName("return 400 when a date matches the format but doesn't exist (31/02)")
  public void dateThatDoesNotExist() {
    given().when().get("/test/date/31-02-2025").then()
        .statusCode(400)
        .body("errors[0].message", equalTo("The value '31/02/2025' is not a valid date or hour"));
  }

  @Test
  @DisplayName("format and parse the dates as dd/MM/yyyy")
  public void validDate() {
    given().when().get("/test/date/28-02-2025").then()
        .statusCode(200)
        .body("date", equalTo("28/02/2025"));
  }
}
