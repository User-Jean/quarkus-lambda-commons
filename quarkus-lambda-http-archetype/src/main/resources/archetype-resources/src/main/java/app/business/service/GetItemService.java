package com.example.app.business.service;

import com.example.app.business.entity.ItemEntity;
import com.example.app.business.repository.ItemRepository;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

/** GET /item/{id} */
@XRayTraced
@ApplicationScoped
public class GetItemService {

  static final String NOT_FOUND = "Item not found";

  @Inject ItemRepository repository;

  public ItemEntity getItem(Long id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
  }
}
