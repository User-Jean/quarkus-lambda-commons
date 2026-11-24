package com.xdyan.commons.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NullFieldsTest {

  @Test
  @DisplayName("don't write a null field in the response, but keep an empty list")
  public void nullFieldNotWritten() {
    given()
        .when()
        .get("/test/fields")
        .then()
        .statusCode(200)
        .body("name", equalTo("x"))
        .body("$", not(hasKey("note")))
        .body("tags", empty());
  }
}
