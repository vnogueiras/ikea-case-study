package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);

    // AMSTERDAM-001: max 3 warehouses, max capacity 100
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 3, 100));
    when(warehouseStore.getAll()).thenReturn(List.of());
  }

  @Test
  void shouldCreateValidWarehouse() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 50, 10);

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
  }

  @Test
  void shouldRejectExistingBusinessUnitCode() {
    when(warehouseStore.findByBusinessUnitCode("MWH.100"))
        .thenReturn(warehouse("MWH.100", "AMSTERDAM-001", 10, 1));

    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 50, 10));
  }

  @Test
  void shouldRejectUnknownLocation() {
    assertRejected(warehouse("MWH.100", "UNKNOWN-001", 50, 10));
  }

  @Test
  void shouldRejectWhenLocationHasMaxNumberOfWarehouses() {
    when(warehouseStore.getAll())
        .thenReturn(
            List.of(
                warehouse("MWH.1", "AMSTERDAM-001", 10, 0),
                warehouse("MWH.2", "AMSTERDAM-001", 10, 0),
                warehouse("MWH.3", "AMSTERDAM-001", 10, 0)));

    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 10, 0));
  }

  @Test
  void shouldRejectCapacityAboveRemainingLocationCapacity() {
    when(warehouseStore.getAll()).thenReturn(List.of(warehouse("MWH.1", "AMSTERDAM-001", 80, 0)));

    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 21, 0));
  }

  @Test
  void shouldAcceptCapacityThatFillsLocationExactly() {
    when(warehouseStore.getAll()).thenReturn(List.of(warehouse("MWH.1", "AMSTERDAM-001", 80, 0)));

    useCase.create(warehouse("MWH.100", "AMSTERDAM-001", 20, 0));

    verify(warehouseStore).create(any());
  }

  @Test
  void shouldRejectStockAboveCapacity() {
    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 10, 11));
  }

  @Test
  void shouldRejectMissingOrInvalidFields() {
    assertRejected(warehouse(null, "AMSTERDAM-001", 10, 1));
    assertRejected(warehouse("MWH.100", null, 10, 1));
    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", null, 1));
    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 10, null));
    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 0, 0));
    assertRejected(warehouse("MWH.100", "AMSTERDAM-001", 10, -1));
  }

  private void assertRejected(Warehouse warehouse) {
    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
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
