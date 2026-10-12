package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

public class FulfilmentServiceTest {

  private static final Long STORE_ID = 1L;
  private static final Long PRODUCT_ID = 2L;
  private static final String WAREHOUSE = "MWH.001";

  private FulfilmentService service;
  private FulfilmentAssignmentRepository assignmentRepository;
  private StoreRepository storeRepository;
  private ProductRepository productRepository;
  private WarehouseStore warehouseStore;

  private final Store store = new Store("TONSTAD");
  private final Product product = new Product("KALLAX");

  @BeforeEach
  void setUp() {
    assignmentRepository = mock(FulfilmentAssignmentRepository.class);
    storeRepository = mock(StoreRepository.class);
    productRepository = mock(ProductRepository.class);
    warehouseStore = mock(WarehouseStore.class);

    service = new FulfilmentService();
    service.assignmentRepository = assignmentRepository;
    service.storeRepository = storeRepository;
    service.productRepository = productRepository;
    service.warehouseStore = warehouseStore;

    // by default: everything exists and there are no assignments yet
    store.id = STORE_ID;
    product.id = PRODUCT_ID;
    when(storeRepository.findById(STORE_ID)).thenReturn(store);
    when(productRepository.findById(PRODUCT_ID)).thenReturn(product);
    when(warehouseStore.findByBusinessUnitCode(WAREHOUSE)).thenReturn(new Warehouse());
    when(assignmentRepository.countWarehousesFor(STORE_ID, PRODUCT_ID)).thenReturn(0L);
  }

  // ---------- assign ----------

  @Test
  void shouldAssignWarehouseToProductForStore() {
    FulfilmentAssignment result = service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE);

    ArgumentCaptor<FulfilmentAssignment> saved = ArgumentCaptor.forClass(FulfilmentAssignment.class);
    verify(assignmentRepository).persist(saved.capture());
    assertSame(store, saved.getValue().store);
    assertSame(product, saved.getValue().product);
    assertEquals(WAREHOUSE, saved.getValue().warehouseBusinessUnitCode);
    assertSame(saved.getValue(), result);
  }

  @Test
  void shouldAllowSecondWarehouse() {
    when(assignmentRepository.countWarehousesFor(STORE_ID, PRODUCT_ID)).thenReturn(1L);

    service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE);

    verify(assignmentRepository).persist(any(FulfilmentAssignment.class));
  }

  @Test
  void shouldRejectThirdWarehouseForSameProductAndStore() {
    when(assignmentRepository.countWarehousesFor(STORE_ID, PRODUCT_ID)).thenReturn(2L);

    assertRejected(400, () -> service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE));
  }

  @Test
  void shouldRejectSameWarehouseTwice() {
    when(assignmentRepository.exists(STORE_ID, PRODUCT_ID, WAREHOUSE)).thenReturn(true);

    assertRejected(409, () -> service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE));
  }

  @Test
  void shouldReturn404ForUnknownStore() {
    assertRejected(404, () -> service.assign(99L, PRODUCT_ID, WAREHOUSE));
  }

  @Test
  void shouldReturn404ForUnknownProduct() {
    assertRejected(404, () -> service.assign(STORE_ID, 99L, WAREHOUSE));
  }

  @Test
  void shouldReturn404ForUnknownOrArchivedWarehouse() {
    // findByBusinessUnitCode only returns active warehouses, so archived ones are "unknown" too
    assertRejected(404, () -> service.assign(STORE_ID, PRODUCT_ID, "MWH.ARCHIVED"));
  }

  @Test
  void shouldReturn400ForMissingFields() {
    assertRejected(400, () -> service.assign(null, PRODUCT_ID, WAREHOUSE));
    assertRejected(400, () -> service.assign(STORE_ID, null, WAREHOUSE));
    assertRejected(400, () -> service.assign(STORE_ID, PRODUCT_ID, null));
    assertRejected(400, () -> service.assign(STORE_ID, PRODUCT_ID, " "));
  }

  @Test
  void shouldRejectFourthDifferentWarehouseForStore() {
    when(assignmentRepository.countDistinctWarehousesFor(STORE_ID)).thenReturn(3L);
    when(assignmentRepository.storeUsesWarehouse(STORE_ID, WAREHOUSE)).thenReturn(false);

    assertRejected(400, () -> service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE));
  }

  @Test
  void shouldAllowWarehouseTheStoreAlreadyUsesEvenAtTheLimit() {
    // the store has 3 warehouses, but this one is one of them (used for another product)
    when(assignmentRepository.countDistinctWarehousesFor(STORE_ID)).thenReturn(3L);
    when(assignmentRepository.storeUsesWarehouse(STORE_ID, WAREHOUSE)).thenReturn(true);

    service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE);

    verify(assignmentRepository).persist(any(FulfilmentAssignment.class));
  }

  @Test
  void shouldAllowThirdDifferentWarehouseForStore() {
    when(assignmentRepository.countDistinctWarehousesFor(STORE_ID)).thenReturn(2L);

    service.assign(STORE_ID, PRODUCT_ID, WAREHOUSE);

    verify(assignmentRepository).persist(any(FulfilmentAssignment.class));
  }

  // ---------- remove ----------

  @Test
  void shouldRemoveExistingAssignment() {
    when(assignmentRepository.deleteById(5L)).thenReturn(true);

    assertDoesNotThrow(() -> service.remove(5L));
    verify(assignmentRepository).deleteById(5L);
  }

  @Test
  void shouldReturn404WhenRemovingUnknownAssignment() {
    when(assignmentRepository.deleteById(5L)).thenReturn(false);

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> service.remove(5L));
    assertEquals(404, e.getResponse().getStatus());
  }

  // ---------- helpers ----------

  /** The call fails with the given HTTP status and nothing is saved. */
  private void assertRejected(int expectedStatus, Executable call) {
    WebApplicationException e = assertThrows(WebApplicationException.class, call);
    assertEquals(expectedStatus, e.getResponse().getStatus());
    verify(assignmentRepository, never()).persist(any(FulfilmentAssignment.class));
  }
}
