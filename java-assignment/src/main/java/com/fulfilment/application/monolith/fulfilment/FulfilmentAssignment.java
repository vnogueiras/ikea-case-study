package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A Warehouse that fulfils a Product for a Store. The warehouse is referenced by its Business Unit
 * Code, so the assignment stays valid when the warehouse is replaced (the code is re-used).
 */
@Entity
@Table(
    name = "fulfilment_assignment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"store_id", "product_id", "warehouseBusinessUnitCode"}))
public class FulfilmentAssignment extends PanacheEntity {

  @ManyToOne(optional = false)
  public Store store;

  @ManyToOne(optional = false)
  public Product product;

  @Column(nullable = false)
  public String warehouseBusinessUnitCode;
}
