package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentAssignmentRepository implements PanacheRepository<FulfilmentAssignment> {

  /** Number of warehouses fulfilling this product for this store. */
  public long countWarehousesFor(Long storeId, Long productId) {
    return count("store.id = ?1 and product.id = ?2", storeId, productId);
  }

  public boolean exists(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouseBusinessUnitCode = ?3",
            storeId,
            productId,
            warehouseBusinessUnitCode)
        > 0;
  }

  /** All assignments, optionally filtered by store and/or product. */
  public List<FulfilmentAssignment> search(Long storeId, Long productId) {
    if (storeId != null && productId != null) {
      return list("store.id = ?1 and product.id = ?2", storeId, productId);
    }
    if (storeId != null) {
      return list("store.id", storeId);
    }
    if (productId != null) {
      return list("product.id", productId);
    }
    return listAll();
  }
}
