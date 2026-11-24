package com.example.app.business.repository;

import com.example.app.business.entity.ItemEntity;
import java.util.List;
import java.util.Optional;

/** The table items: the services use it, the MySQL implementation is in infrastructure.mysql. */
public interface ItemRepository {
  void create(ItemEntity entity);

  Optional<ItemEntity> findById(Long id);

  List<ItemEntity> findAll();

  boolean existsByName(String name);
}
