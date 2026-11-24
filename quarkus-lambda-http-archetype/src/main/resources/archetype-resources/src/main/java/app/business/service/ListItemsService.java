package com.example.app.business.service;

import com.example.app.business.entity.ItemEntity;
import com.example.app.business.repository.ItemRepository;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** GET /item */
@XRayTraced
@ApplicationScoped
public class ListItemsService {

  @Inject ItemRepository repository;

  public List<ItemEntity> listItems() {
    return repository.findAll();
  }
}
