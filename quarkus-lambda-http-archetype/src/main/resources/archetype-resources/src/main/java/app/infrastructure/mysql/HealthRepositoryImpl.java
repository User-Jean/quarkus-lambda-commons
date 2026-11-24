package com.example.app.infrastructure.mysql;

import com.example.app.business.repository.HealthRepository;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Uses the DataSource instead of the EntityManager so the check doesn't depend on Hibernate. */
@XRayTraced
@ApplicationScoped
public class HealthRepositoryImpl implements HealthRepository {

  private static final int TIMEOUT_SECONDS = 2;

  @Inject DataSource dataSource;

  @Override
  public void ping() {
    try (var connection = dataSource.getConnection()) {
      if (!connection.isValid(TIMEOUT_SECONDS)) {
        throw new IllegalStateException(
            "Connection is not valid after " + TIMEOUT_SECONDS + " seconds");
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
