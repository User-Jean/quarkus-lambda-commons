package com.xdyan.commons.http;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Each error response leaves its exception in the log, with the MDC of the request. application.properties sets the
 * global level to ERROR, like the apps: the INFO and WARN of the library still show by its own default.
 */
@QuarkusTest
public class ErrorLogTest {

  private ExtLogRecord find(LogCapture capture, String start) {
    return capture.records.stream().filter(r -> r.getFormattedMessage().startsWith(start)).findFirst()
        .orElseThrow(() -> new AssertionError("No log starting with '" + start + "' in " + capture.messages()));
  }

  @Test
  @DisplayName("log a 4xx as WARN with the exception, its message and the status of the response")
  public void logA4xx() {
    try (var capture = LogCapture.start()) {
      given().when().get("/test/not-found").then().statusCode(404);

      ExtLogRecord error = find(capture, "[ERROR 404] NotFoundException: Item not found");
      assertEquals("WARN", error.getLevel().getName());
      assertNull(error.getThrown(), "a 4xx has no stack trace");
      assertEquals("/test/not-found", error.getMdc("path").replaceFirst("^https?://[^/]+", ""));
      assertTrue(capture.messages().stream().anyMatch(m -> m.startsWith("INFO Response 404: Body: {")),
          capture.messages().toString());
    }
  }

  @Test
  @DisplayName("return 500 without the message of the exception and log it as ERROR with the stack trace")
  public void logA5xx() {
    try (var capture = LogCapture.start()) {
      given().when().get("/test/boom").then()
          .statusCode(500)
          .body("message", equalTo("Internal server error"))
          .body("errors[0].message", equalTo("Unexpected error, see the log of the request"));

      ExtLogRecord error = find(capture, "[ERROR 500] IllegalStateException: database password is wrong");
      assertEquals("ERROR", error.getLevel().getName());
      assertInstanceOf(IllegalStateException.class, error.getThrown());
    }
  }

  /**
   * RESTEasy answers a body that is not JSON itself, with a text body (RESTEASY-JACKSON000100, logged as ERROR by
   * RESTEasy): JAX-RS doesn't pass to the providers an exception whose response already has a body.
   */
  @Test
  @DisplayName("return 400 when the body is not valid JSON")
  public void invalidJson() {
    given().contentType(JSON).body("{\"name\": ").when().post("/test/body").then()
        .statusCode(400);
  }

  @Test
  @DisplayName("keep the status of a WebApplicationException without its own provider (405)")
  public void methodNotAllowed() {
    given().when().put("/test/not-found").then()
        .statusCode(405)
        .body("code", equalTo(405));
  }

  @Test
  @DisplayName("log a response without body and clear the MDC, so the next request doesn't inherit it")
  public void responseWithoutBody() {
    try (var capture = LogCapture.start()) {
      given().when().delete("/test/no-content").then().statusCode(204);

      find(capture, "Response 204: Body is empty");
      assertNull(org.slf4j.MDC.get("path"));
    }
  }
}
