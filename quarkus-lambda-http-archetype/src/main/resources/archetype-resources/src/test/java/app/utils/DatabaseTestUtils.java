package com.example.app.utils;

import jakarta.persistence.EntityManager;
import java.util.List;

public class DatabaseTestUtils {

  /** Deletes in the order of the foreign keys (children first). Must run inside a transaction. */
  public static void cleanDatabase(EntityManager em) {
    List.of("ItemEntity")
        .forEach(entity -> em.createQuery("DELETE FROM " + entity).executeUpdate());
  }
}
