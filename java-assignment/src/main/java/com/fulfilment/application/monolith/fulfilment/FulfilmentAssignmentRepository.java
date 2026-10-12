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

  /** Number of different warehouses fulfilling any product for this store. */
  public long countDistinctWarehousesFor(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct a.warehouseBusinessUnitCode) from FulfilmentAssignment a"
                + " where a.store.id = ?1",
            Long.class)
        .setParameter(1, storeId)
        .getSingleResult();
  }

  /** Whether this warehouse already fulfils any product for this store. */
  public boolean storeUsesWarehouse(Long storeId, String warehouseBusinessUnitCode) {
    return count(
            "store.id = ?1 and warehouseBusinessUnitCode = ?2", storeId, warehouseBusinessUnitCode)
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
