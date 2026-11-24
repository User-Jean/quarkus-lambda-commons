package com.example.app.api.item;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.example.api.ItemResource;
import com.example.app.utils.DatabaseTestUtils;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GET /item/{id}. The data is inline SQL with fixed IDs from 101, so they don't collide with the
 * ones generated when a test creates through the API.
 */
@QuarkusTest
@TestHTTPEndpoint(ItemResource.class)
public class ItemResourceGetSuccessTest {

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  public void setUp() {
    em.createNativeQuery(
            """
            INSERT INTO items (id, name, description, unit_price, created_at) VALUES
              (101, 'Teclado', 'Switches marrones', 149.90, '2025-12-01 10:30:00'),
              (102, 'Mouse',   NULL,                59.00,  '2025-12-02 08:00:00')
            """)
        .executeUpdate();
  }

  @AfterEach
  @Transactional
  public void tearDown() {
    DatabaseTestUtils.cleanDatabase(em);
  }

  @Test
  @DisplayName("return the item with its dates as dd/MM/yyyy HH:mm")
  public void returnTheItem() {
    given()
        .when()
        .get("/{id}", 101)
        .then()
        .statusCode(OK.getStatusCode())
        .body("id", equalTo(101))
        .body("name", equalTo("Teclado"))
        .body("description", equalTo("Switches marrones"))
        .body("unit_price", equalTo(149.90f))
        .body("created_at", equalTo("01/12/2025 10:30"));
  }

  @Test
  @DisplayName("leave out the optional fields that are empty")
  public void leaveOutTheEmptyFields() {
    given()
        .when()
        .get("/{id}", 102)
        .then()
        .statusCode(OK.getStatusCode())
        .body("$", not(hasKey("description")));
  }
}
