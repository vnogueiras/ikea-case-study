package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    validateRequiredFields(warehouse);

    // Business Unit Code Verification
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseValidationException(
          "Warehouse with business unit code " + warehouse.businessUnitCode + " already exists.");
    }

    // Location Validation
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + warehouse.location + " is not a valid location.");
    }

    // Warehouse Creation Feasibility
    List<Warehouse> warehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> location.identification.equals(w.location))
            .toList();
    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Location "
              + location.identification
              + " already has the maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").");
    }

    // Capacity and Stock Validation
    int usedCapacity = warehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Capacity "
              + warehouse.capacity
              + " exceeds the remaining capacity of location "
              + location.identification
              + " ("
              + (location.maxCapacity - usedCapacity)
              + ").");
    }
    if (warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
          "Stock " + warehouse.stock + " exceeds the warehouse capacity " + warehouse.capacity + ".");
    }

    // if all went well, create the warehouse
    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;
    warehouseStore.create(warehouse);
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
