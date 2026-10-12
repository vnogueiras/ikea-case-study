package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository repository;

  @Test
  public void removeShouldDeleteTheWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.REPO-REMOVE";
    warehouse.location = "HELMOND-001";
    warehouse.capacity = 10;
    warehouse.stock = 1;
    warehouse.createdAt = LocalDateTime.now();
    QuarkusTransaction.requiringNew().run(() -> repository.create(warehouse));
    assertNotNull(warehouse.id);

    QuarkusTransaction.requiringNew().run(() -> repository.remove(warehouse));

    assertNull(QuarkusTransaction.requiringNew().call(() -> repository.findById(warehouse.id)));
  }

  @Test
  public void removeShouldFailForUnknownWarehouse() {
    Warehouse unknown = new Warehouse();
    unknown.id = 999999L;

    assertThrows(
        IllegalArgumentException.class,
        () -> QuarkusTransaction.requiringNew().run(() -> repository.remove(unknown)));
  }
}
