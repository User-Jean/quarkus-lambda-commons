package com.example.app.infrastructure.mysql;

import com.example.app.business.entity.ItemEntity;
import com.example.app.business.repository.ItemRepository;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@XRayTraced
@ApplicationScoped
public class ItemRepositoryImpl implements ItemRepository {

  @Inject EntityManager em;

  @Override
  @Transactional
  public void create(ItemEntity entity) {
    em.persist(entity);
    em.flush();
  }

  @Override
  public Optional<ItemEntity> findById(Long id) {
    return Optional.ofNullable(em.find(ItemEntity.class, id));
  }

  @Override
  public List<ItemEntity> findAll() {
    return em.createQuery("FROM ItemEntity ORDER BY name", ItemEntity.class).getResultList();
  }

  @Override
  public boolean existsByName(String name) {
    return em.createQuery("SELECT COUNT(i) FROM ItemEntity i WHERE i.name = :name", Long.class)
            .setParameter("name", name)
            .getSingleResult()
        > 0;
  }
}
