package com.example.app.business.service;

import com.example.app.business.entity.ItemEntity;
import com.example.app.business.repository.ItemRepository;
import com.xdyan.commons.http.error.ConflictException;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * POST /item. The rules the OpenAPI can't express (the decimals of a price, a name already taken)
 * are checked here.
 */
@XRayTraced
@ApplicationScoped
public class CreateItemService {

  @Inject ItemRepository repository;

  @Inject Clock clock;

  public ItemEntity createItem(ItemEntity item) {
    if (item.getUnitPrice().stripTrailingZeros().scale() > 2) {
      throw new BadRequestException("The field 'unit_price' must have at most 2 decimals");
    }
    if (repository.existsByName(item.getName())) {
      throw new ConflictException("An item named '" + item.getName() + "' already exists");
    }
    // Same scale as the DECIMAL(10, 2) column, so the 201 shows 59.90 like a later GET, not 59.9
    item.setUnitPrice(item.getUnitPrice().setScale(2));
    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    item.setCreatedAt(now);
    repository.create(item);
    return item;
  }
}
