package com.example.app.api.item;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.api.ItemResource;
import com.example.app.utils.DatabaseTestUtils;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * POST /item. Each test sends a valid item with one field changed or removed, and checks nothing
 * was created.
 */
@QuarkusTest
@TestHTTPEndpoint(ItemResource.class)
public class ItemResourceCreateErrorTest {

  private static final String VALID_ITEM =
      """
      {"name": "Teclado", "description": "Switches marrones", "unit_price": 149.9}
      """;

  @Inject EntityManager em;

  @AfterEach
  @Transactional
  public void tearDown() {
    DatabaseTestUtils.cleanDatabase(em);
  }

  private static String validItemWith(String field, Object value) {
    return new JsonObject(VALID_ITEM).put(field, value).encode();
  }

  private long countItems() {
    return em.createQuery("SELECT COUNT(i) FROM ItemEntity i", Long.class).getSingleResult();
  }

  @ParameterizedTest
  @ValueSource(strings = {"name", "unit_price"})
  @DisplayName("return status BAD_REQUEST when a required field is missing")
  public void returnStatusBadRequestWhenARequiredFieldIsMissing(String field) {
    JsonObject body = new JsonObject(VALID_ITEM);
    body.remove(field);

    given()
        .contentType(JSON)
        .body(body.encode())
        .when()
        .post()
        .then()
        .statusCode(BAD_REQUEST.getStatusCode())
        .body("code", equalTo(BAD_REQUEST.getStatusCode()))
        .body("errors[0].message", containsString(field));

    assertEquals(0L, countItems());
  }

  static Stream<Arguments> invalidValues() {
    return Stream.of(
        Arguments.of("name", "", "name"),
        Arguments.of("name", "<script>alert(1)</script>", "name"),
        Arguments.of("name", "x".repeat(101), "name"),
        Arguments.of("description", "<b>", "description"),
        Arguments.of("unit_price", -1, "unit_price"),
        Arguments.of("unit_price", 10.123, "unit_price"),
        Arguments.of("unit_price", "abc", "unit_price"));
  }

  @ParameterizedTest(name = "{0} = {1}")
  @MethodSource("invalidValues")
  @DisplayName("return status BAD_REQUEST when a field has an invalid value")
  public void returnStatusBadRequestWhenAFieldIsInvalid(
      String field, Object value, String message) {
    given()
        .contentType(JSON)
        .body(validItemWith(field, value))
        .when()
        .post()
        .then()
        .statusCode(BAD_REQUEST.getStatusCode())
        .body("errors[0].message", containsString(message));

    assertEquals(0L, countItems());
  }

  @Test
  @DisplayName("return status CONFLICT when an item with that name already exists")
  public void returnStatusConflictWhenTheNameIsTaken() {
    given().contentType(JSON).body(VALID_ITEM).when().post();

    given()
        .contentType(JSON)
        .body(VALID_ITEM)
        .when()
        .post()
        .then()
        .statusCode(CONFLICT.getStatusCode())
        .body("errors[0].message", containsString("An item named 'Teclado' already exists"));

    assertEquals(1L, countItems());
  }
}
