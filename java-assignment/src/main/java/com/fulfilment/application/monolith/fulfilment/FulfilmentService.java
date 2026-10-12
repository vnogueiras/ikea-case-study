package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

/** Assigns warehouses as fulfilment units of products for stores, enforcing the limits. */
@ApplicationScoped
public class FulfilmentService {

  /** Constraint 1: each Product can be fulfilled by max 2 different Warehouses per Store. */
  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;

  /** Constraint 2: each Store can be fulfilled by max 3 different Warehouses. */
  static final int MAX_WAREHOUSES_PER_STORE = 3;

  @Inject FulfilmentAssignmentRepository assignmentRepository;
  @Inject StoreRepository storeRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseStore warehouseStore;

  @Transactional
  public FulfilmentAssignment assign(Long storeId, Long productId, String warehouseBuCode) {
    if (storeId == null
        || productId == null
        || warehouseBuCode == null
        || warehouseBuCode.isBlank()) {
      throw new WebApplicationException(
          "storeId, productId and warehouseBusinessUnitCode are required.", 400);
    }

    Store store = storeRepository.findById(storeId);
    if (store == null) {
      throw new WebApplicationException("Store with id " + storeId + " does not exist.", 404);
    }
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new WebApplicationException("Product with id " + productId + " does not exist.", 404);
    }
    if (warehouseStore.findByBusinessUnitCode(warehouseBuCode) == null) {
      throw new WebApplicationException(
          "Active warehouse with business unit code " + warehouseBuCode + " does not exist.", 404);
    }

    if (assignmentRepository.exists(storeId, productId, warehouseBuCode)) {
      throw new WebApplicationException(
          "Warehouse " + warehouseBuCode + " already fulfils this product for this store.", 409);
    }

    if (assignmentRepository.countWarehousesFor(storeId, productId)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "Product "
              + productId
              + " already has the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses for store "
              + storeId
              + ".",
          400);
    }

    // a warehouse that already serves this store (for another product) is not a new one
    if (!assignmentRepository.storeUsesWarehouse(storeId, warehouseBuCode)
        && assignmentRepository.countDistinctWarehousesFor(storeId) >= MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "Store "
              + storeId
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses.",
          400);
    }

    FulfilmentAssignment assignment = new FulfilmentAssignment();
    assignment.store = store;
    assignment.product = product;
    assignment.warehouseBusinessUnitCode = warehouseBuCode;
    assignmentRepository.persist(assignment);
    return assignment;
  }

  @Transactional
  public void remove(Long id) {
    if (!assignmentRepository.deleteById(id)) {
      throw new WebApplicationException("Fulfilment assignment " + id + " does not exist.", 404);
    }
  }
}
