package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void shouldSetArchivedAtAndSave() {
    Warehouse warehouse = warehouse();

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    verify(warehouseStore).update(warehouse);
  }

  @Test
  void shouldRejectAlreadyArchivedWarehouse() {
    Warehouse warehouse = warehouse();
    warehouse.archivedAt = LocalDateTime.now().minusDays(1);

    assertThrows(WarehouseValidationException.class, () -> useCase.archive(warehouse));
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void shouldReturnNotFoundForMissingWarehouse() {
    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(null));
    verify(warehouseStore, never()).update(any());
  }

  private static Warehouse warehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.id = 1L;
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 10;
    return warehouse;
  }
}
