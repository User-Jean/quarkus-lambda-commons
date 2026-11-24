package com.example.app.api.item;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.example.api.ItemResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GET /item/{id} */
@QuarkusTest
@TestHTTPEndpoint(ItemResource.class)
public class ItemResourceGetErrorTest {

  @Test
  @DisplayName("return status NOT_FOUND when the item doesn't exist")
  public void returnStatusNotFoundWhenTheItemDoesNotExist() {
    given()
        .when()
        .get("/{id}", 999_999)
        .then()
        .statusCode(NOT_FOUND.getStatusCode())
        .body("code", equalTo(NOT_FOUND.getStatusCode()))
        .body("errors[0].message", containsString("Item not found"));
  }

  @Test
  @DisplayName("return status NOT_FOUND when the id is not a number")
  public void returnStatusNotFoundWhenTheIdIsNotANumber() {
    given()
        .when()
        .get("/{id}", "abc")
        .then()
        .statusCode(NOT_FOUND.getStatusCode())
        .body("code", equalTo(NOT_FOUND.getStatusCode()));
  }
}
