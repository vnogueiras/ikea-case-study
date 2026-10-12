package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Assigns warehouses as fulfilment units of products for stores, enforcing the limits. */
@ApplicationScoped
public class FulfilmentService {

  /** Constraint 1: each Product can be fulfilled by max 2 different Warehouses per Store. */
  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;

  /** Constraint 2: each Store can be fulfilled by max 3 different Warehouses. */
  static final int MAX_WAREHOUSES_PER_STORE = 3;

  /** Constraint 3: each Warehouse can store max 5 types of Products. */
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject FulfilmentAssignmentRepository assignmentRepository;
  @Inject StoreRepository storeRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseStore warehouseStore;

  @Transactional
  public FulfilmentAssignment assign(Long storeId, Long productId, String warehouseBuCode) {
    validateRequiredFields(storeId, productId, warehouseBuCode);

    Store store = findStore(storeId);
    Product product = findProduct(productId);
    validateWarehouseIsActive(warehouseBuCode);

    validateNotAlreadyAssigned(storeId, productId, warehouseBuCode);
    validateWarehousesPerProductPerStore(storeId, productId);
    validateWarehousesPerStore(storeId, warehouseBuCode);
    validateProductsPerWarehouse(warehouseBuCode, productId);

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
      throw new FulfilmentException.NotFound("Fulfilment assignment " + id + " does not exist.");
    }
  }

  // ---------- input and lookups ----------

  private void validateRequiredFields(Long storeId, Long productId, String warehouseBuCode) {
    if (storeId == null
        || productId == null
        || warehouseBuCode == null
        || warehouseBuCode.isBlank()) {
      throw new FulfilmentException.Invalid(
          "storeId, productId and warehouseBusinessUnitCode are required.");
    }
  }

  private Store findStore(Long storeId) {
    Store store = storeRepository.findById(storeId);
    if (store == null) {
      throw new FulfilmentException.NotFound("Store with id " + storeId + " does not exist.");
    }
    return store;
  }

  private Product findProduct(Long productId) {
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new FulfilmentException.NotFound("Product with id " + productId + " does not exist.");
    }
    return product;
  }

  /** Only active warehouses can be assigned; archived ones are not found by business unit code. */
  private void validateWarehouseIsActive(String warehouseBuCode) {
    if (warehouseStore.findByBusinessUnitCode(warehouseBuCode) == null) {
      throw new FulfilmentException.NotFound(
          "Active warehouse with business unit code " + warehouseBuCode + " does not exist.");
    }
  }

  // ---------- business rules ----------

  private void validateNotAlreadyAssigned(Long storeId, Long productId, String warehouseBuCode) {
    if (assignmentRepository.exists(storeId, productId, warehouseBuCode)) {
      throw new FulfilmentException.Conflict(
          "Warehouse " + warehouseBuCode + " already fulfils this product for this store.");
    }
  }

  /** Constraint 1. */
  private void validateWarehousesPerProductPerStore(Long storeId, Long productId) {
    if (assignmentRepository.countWarehousesFor(storeId, productId)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new FulfilmentException.Invalid(
          "Product "
              + productId
              + " already has the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses for store "
              + storeId
              + ".");
    }
  }

  /** Constraint 2. A warehouse the store already uses (for another product) is not a new one. */
  private void validateWarehousesPerStore(Long storeId, String warehouseBuCode) {
    if (!assignmentRepository.storeUsesWarehouse(storeId, warehouseBuCode)
        && assignmentRepository.countDistinctWarehousesFor(storeId) >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfilmentException.Invalid(
          "Store "
              + storeId
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses.");
    }
  }

  /** Constraint 3. A product the warehouse already stores (for another store) is not a new type. */
  private void validateProductsPerWarehouse(String warehouseBuCode, Long productId) {
    if (!assignmentRepository.warehouseStoresProduct(warehouseBuCode, productId)
        && assignmentRepository.countDistinctProductsFor(warehouseBuCode)
            >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfilmentException.Invalid(
          "Warehouse "
              + warehouseBuCode
              + " already stores the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE
              + " product types.");
    }
  }
}
