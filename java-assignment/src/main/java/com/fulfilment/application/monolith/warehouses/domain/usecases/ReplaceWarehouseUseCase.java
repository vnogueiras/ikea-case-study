package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Archives the active warehouse with the given business unit code and creates the new one with the
 * same code, so the history of the business unit is kept.
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    validateRequiredFields(newWarehouse);

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new WarehouseNotFoundException(
          "Warehouse with business unit code " + newWarehouse.businessUnitCode + " not found.");
    }

    // Capacity Accommodation
    if (newWarehouse.capacity < current.stock) {
      throw new WarehouseValidationException(
          "New capacity "
              + newWarehouse.capacity
              + " cannot accommodate the current stock "
              + current.stock
              + ".");
    }

    // Stock Matching
    if (!newWarehouse.stock.equals(current.stock)) {
      throw new WarehouseValidationException(
          "New stock "
              + newWarehouse.stock
              + " must match the stock of the warehouse being replaced ("
              + current.stock
              + ").");
    }

    // Location Validation
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + newWarehouse.location + " is not a valid location.");
    }

    // Feasibility and capacity at the location, without the warehouse being replaced
    List<Warehouse> othersAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> location.identification.equals(w.location))
            .filter(w -> !w.businessUnitCode.equals(current.businessUnitCode))
            .toList();
    if (othersAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Location "
              + location.identification
              + " already has the maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").");
    }
    int usedCapacity = othersAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (usedCapacity + newWarehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Capacity "
              + newWarehouse.capacity
              + " exceeds the remaining capacity of location "
              + location.identification
              + " ("
              + (location.maxCapacity - usedCapacity)
              + ").");
    }

    // Archive the current warehouse and create the new one with the same business unit code
    LocalDateTime now = LocalDateTime.now();
    current.archivedAt = now;
    warehouseStore.update(current);

    newWarehouse.createdAt = now;
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }

  private void validateRequiredFields(Warehouse warehouse) {
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WarehouseValidationException("Business unit code is required.");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WarehouseValidationException("Location is required.");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WarehouseValidationException("Capacity must be greater than 0.");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WarehouseValidationException("Stock must be 0 or more.");
    }
  }
}
