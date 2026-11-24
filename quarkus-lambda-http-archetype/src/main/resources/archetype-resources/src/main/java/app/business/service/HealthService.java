package com.example.app.business.service;

import com.example.app.business.repository.HealthRepository;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@XRayTraced
@ApplicationScoped
public class HealthService {

  @Inject HealthRepository repository;

  public boolean isDatabaseUp() {
    try {
      repository.ping();
      return true;
      // Any failure (connection, timeout, driver) means the database is down: 503
    } catch (
        @SuppressWarnings("PMD.AvoidCatchingGenericException")
        Exception e) {
      log.error("Database health check failed: {}", e.getMessage());
      return false;
    }
  }
}
