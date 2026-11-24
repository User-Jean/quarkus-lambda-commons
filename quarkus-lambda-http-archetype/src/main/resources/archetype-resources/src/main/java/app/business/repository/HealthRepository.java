package com.example.app.business.repository;

/** The database for the health check (GET /hello). */
public interface HealthRepository {
  /** Throws if the database doesn't answer. */
  void ping();
}
