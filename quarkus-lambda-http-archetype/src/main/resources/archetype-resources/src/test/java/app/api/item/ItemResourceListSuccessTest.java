package com.example.app.api.item;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.example.api.ItemResource;
import com.example.app.utils.DatabaseTestUtils;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GET /item. An endpoint without possible errors has only a Success class. */
@QuarkusTest
@TestHTTPEndpoint(ItemResource.class)
public class ItemResourceListSuccessTest {

  @Inject EntityManager em;

  @Transactional
  void insertItems() {
    em.createNativeQuery(
            """
            INSERT INTO items (id, name, description, unit_price, created_at) VALUES
              (101, 'Mouse',   NULL, 59.90, '2025-12-01 10:00:00'),
              (102, 'Monitor', NULL, 899.00, '2025-12-02 10:00:00')
            """)
        .executeUpdate();
  }

  @AfterEach
  @Transactional
  public void tearDown() {
    DatabaseTestUtils.cleanDatabase(em);
  }

  @Test
  @DisplayName("return the items ordered by name")
  public void returnTheItemsOrderedByName() {
    insertItems();

    given()
        .when()
        .get()
        .then()
        .statusCode(OK.getStatusCode())
        .body("name", contains("Monitor", "Mouse"))
        .body("id", contains(102, 101));
  }

  @Test
  @DisplayName("return an empty list when there are no items")
  public void returnAnEmptyList() {
    given().when().get().then().statusCode(OK.getStatusCode()).body("$", empty());
  }
}
