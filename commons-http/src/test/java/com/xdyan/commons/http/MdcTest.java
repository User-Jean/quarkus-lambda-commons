package com.xdyan.commons.http;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The MDC of every log line of a request: the fields of the library and the ones of the app (TestMdcContributor).
 */
@QuarkusTest
public class MdcTest {

  private ExtLogRecord requestLog(LogCapture capture) {
    return capture.records.stream()
        .filter(r -> r.getFormattedMessage().startsWith("Request Body"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No request log in " + capture.messages()));
  }

  @Test
  @DisplayName("put the fields of the library in snake_case, without X-Amzn-Trace-Id")
  public void fieldsOfTheLibrary() {
    try (var capture = LogCapture.start()) {
      given().header("x-agw-request-id", "Yx2hTgxyIAMEbXw=")
          .header("X-Amzn-Trace-Id", "Root=1-6af171c1-4e05ad585ad8350922ad93f4")
          .header("X-Client-Name", "web-admin")
          .header("X-Client-Version", "1.2.0")
          .when().get("/test/number/1").then().statusCode(200);

      ExtLogRecord log = requestLog(capture);
      assertEquals("Yx2hTgxyIAMEbXw=", log.getMdc("request_id"));
      assertEquals("", log.getMdc("trace_id"), "no X-Ray trace outside Lambda");
      assertEquals("GET", log.getMdc("method"));
      assertTrue(log.getMdc("path").endsWith("/test/number/1"), log.getMdc("path"));
      assertEquals("web-admin", log.getMdc("client_name"));
      assertEquals("1.2.0", log.getMdc("client_version"));
      assertNull(log.getMdc("requestId"));
      assertNull(log.getMdc("X-Amzn-Trace-Id"));
    }
  }

  @Test
  @DisplayName("put the fields of an MdcContributor in every log line of the request and clear them after it")
  public void fieldsOfTheApp() {
    try (var capture = LogCapture.start()) {
      given().header("X-Tenant-Id", "acme").when().get("/test/not-found").then().statusCode(404);

      assertFalse(capture.records.isEmpty());
      capture.records.forEach(r -> assertEquals("acme", r.getMdc("tenant_id"), r.getFormattedMessage()));
      assertNull(org.slf4j.MDC.get("tenant_id"));
    }
  }

  @Test
  @DisplayName("skip a null value of an MdcContributor")
  public void nullValue() {
    try (var capture = LogCapture.start()) {
      given().when().get("/test/number/1").then().statusCode(200);

      assertNull(requestLog(capture).getMdc("tenant_id"));
    }
  }

  @Test
  @DisplayName("not let an MdcContributor replace a field of the library")
  public void reservedField() {
    try (var capture = LogCapture.start()) {
      given().header("X-Replace-Path", "yes").when().get("/test/number/1").then().statusCode(200);

      assertTrue(requestLog(capture).getMdc("path").endsWith("/test/number/1"));
      assertTrue(capture.messages().stream().anyMatch(m -> m.startsWith("WARN MDC field 'path'")),
          capture.messages().toString());
    }
  }

  @Test
  @DisplayName("answer the request when an MdcContributor fails, and log the failure")
  public void failingContributor() {
    try (var capture = LogCapture.start()) {
      given().header("X-Fail", "yes").when().get("/test/number/1").then().statusCode(200);

      assertTrue(capture.records.stream().anyMatch(r -> r.getFormattedMessage().startsWith("MDC fields of")
          && r.getThrown() instanceof IllegalStateException), capture.messages().toString());
    }
  }

  @Test
  @DisplayName("not log a malformed X-Ray trace header outside Lambda")
  public void noTraceHeaderErrorOutsideLambda() {
    try (var capture = LogCapture.start("com.amazonaws.xray")) {
      given().when().get("/test/number/1").then().statusCode(200);

      assertTrue(capture.messages().isEmpty(), capture.messages().toString());
    }
  }
}
