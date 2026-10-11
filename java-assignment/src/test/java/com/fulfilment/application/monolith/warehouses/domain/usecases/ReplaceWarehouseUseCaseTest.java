package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;
  private Warehouse current;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);

    current = warehouse("MWH.001", "AMSTERDAM-001", 50, 20);
    current.id = 1L;
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    when(warehouseStore.getAll()).thenReturn(List.of(current));

    // AMSTERDAM-001: max 1 warehouse, max capacity 100
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 1, 100));
    // TILBURG-001: max 1 warehouse, max capacity 40
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40));
  }

  @Test
  void shouldArchiveCurrentAndCreateNewWithSameBusinessUnitCode() {
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 80, 20);

    useCase.replace(replacement);

    InOrder order = inOrder(warehouseStore);
    order.verify(warehouseStore).update(current);
    order.verify(warehouseStore).create(replacement);
    assertNotNull(current.archivedAt);
    assertNotNull(replacement.createdAt);
    assertNull(replacement.archivedAt);
    assertEquals("MWH.001", replacement.businessUnitCode);
  }

  @Test
  void shouldNotCountReplacedWarehouseForLocationLimits() {
    // AMSTERDAM-001 allows only 1 warehouse, and it's the one being replaced
    useCase.replace(warehouse("MWH.001", "AMSTERDAM-001", 100, 20));

    verify(warehouseStore).create(any());
  }

  @Test
  void shouldReturnNotFoundWhenNoActiveWarehouse() {
    Warehouse replacement = warehouse("MWH.999", "AMSTERDAM-001", 50, 20);

    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(replacement));
    verifyNothingSaved();
  }

  @Test
  void shouldRejectCapacityThatCannotAccommodateCurrentStock() {
    assertRejected(warehouse("MWH.001", "AMSTERDAM-001", 19, 20));
  }

  @Test
  void shouldRejectStockThatDoesNotMatch() {
    assertRejected(warehouse("MWH.001", "AMSTERDAM-001", 50, 21));
  }

  @Test
  void shouldRejectUnknownLocation() {
    assertRejected(warehouse("MWH.001", "UNKNOWN-001", 50, 20));
  }

  @Test
  void shouldRejectWhenTargetLocationIsFull() {
    when(warehouseStore.getAll())
        .thenReturn(List.of(current, warehouse("MWH.023", "TILBURG-001", 30, 5)));

    assertRejected(warehouse("MWH.001", "TILBURG-001", 30, 20));
  }

  @Test
  void shouldRejectCapacityAboveLocationMaxCapacity() {
    assertRejected(warehouse("MWH.001", "TILBURG-001", 41, 20));
  }

  @Test
  void shouldRejectMissingFields() {
    assertRejected(warehouse("MWH.001", null, 50, 20));
    assertRejected(warehouse("MWH.001", "AMSTERDAM-001", null, 20));
    assertRejected(warehouse("MWH.001", "AMSTERDAM-001", 50, null));
  }

  private void assertRejected(Warehouse replacement) {
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
    verifyNothingSaved();
    assertNull(current.archivedAt);
  }

  private void verifyNothingSaved() {
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  private static Warehouse warehouse(
      String buCode, String location, Integer capacity, Integer stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }
}
