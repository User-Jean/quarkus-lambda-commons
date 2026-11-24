package com.example.app.api;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import com.example.api.HealthResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(HealthResource.class)
public class HealthResourceTest {

  @Test
  @DisplayName("return status OK and database UP when request endpoint GET hello")
  public void returnStatusOkWhenRequestHello() {
    given()
        .when()
        .get()
        .then()
        .statusCode(OK.getStatusCode())
        .body("message", equalTo("Hello"))
        .body("database", equalTo("UP"));
  }
}
