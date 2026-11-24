package com.example.app.business.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Example entity of the archetype, one per table. The columns are the fields in snake_case
 * (PhysicalNamingStrategySnakeCaseImpl): unitPrice is unit_price.
 */
@Entity
@Getter
@Setter
@Table(name = "items")
public class ItemEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;
  private BigDecimal unitPrice;
  private LocalDateTime createdAt;
}
