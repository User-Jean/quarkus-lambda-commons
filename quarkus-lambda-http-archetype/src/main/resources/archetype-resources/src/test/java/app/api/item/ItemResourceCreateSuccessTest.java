package com.example.app.api.item;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.api.ItemResource;
import com.example.app.business.entity.ItemEntity;
import com.example.app.utils.DatabaseTestUtils;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** POST /item. "Now" is Monday 15/12/2025 12:00 (FixedClockProducer). */
@QuarkusTest
@TestHTTPEndpoint(ItemResource.class)
public class ItemResourceCreateSuccessTest {

  @Inject EntityManager em;

  @AfterEach
  @Transactional
  public void tearDown() {
    DatabaseTestUtils.cleanDatabase(em);
  }

  private ItemEntity item(long id) {
    em.clear();
    return em.find(ItemEntity.class, id);
  }

  @Test
  @DisplayName("create the item with the date of now and return it")
  public void createTheItem() {
    Integer id =
        given()
            .contentType(JSON)
            .body(
                """
                {"name": "Teclado mecánico", "description": "Switches marrones", "unit_price": 149.9}
                """)
            .when()
            .post()
            .then()
            .statusCode(CREATED.getStatusCode())
            .body("id", notNullValue())
            .body("name", equalTo("Teclado mecánico"))
            .body("created_at", equalTo("15/12/2025 12:00"))
            .extract()
            .path("id");

    ItemEntity saved = item(id);
    assertEquals("Switches marrones", saved.getDescription());
    assertEquals(new BigDecimal("149.90"), saved.getUnitPrice());
    assertEquals(LocalDateTime.of(2025, 12, 15, 12, 0), saved.getCreatedAt());
  }

  @Test
  @DisplayName("create the item without the optional fields")
  public void createWithoutTheOptionalFields() {
    Integer id =
        given()
            .contentType(JSON)
            .body(
                """
                {"name": "Mouse", "unit_price": 0}
                """)
            .when()
            .post()
            .then()
            .statusCode(CREATED.getStatusCode())
            .extract()
            .path("id");

    assertNull(item(id).getDescription());
  }

  @Test
  @DisplayName("return the price with 2 decimals, like a later GET")
  public void returnThePriceWithTwoDecimals() {
    String body =
        given()
            .contentType(JSON)
            .body(
                """
                {"name": "Mouse", "unit_price": 59.9}
                """)
            .when()
            .post()
            .then()
            .statusCode(CREATED.getStatusCode())
            .extract()
            .asString();

    assertTrue(body.contains("\"unit_price\":59.90"), body);
  }
}
