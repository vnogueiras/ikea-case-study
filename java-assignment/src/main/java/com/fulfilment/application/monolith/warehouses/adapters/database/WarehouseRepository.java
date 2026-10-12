package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  /** Returns the active (not archived) warehouses. */
  @Override
  public List<Warehouse> getAll() {
    return this.list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
    this.persist(entity);
    warehouse.id = entity.id;
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity = this.findById(warehouse.id);
    if (entity == null) {
      throw new IllegalArgumentException("Warehouse with id " + warehouse.id + " does not exist.");
    }
    // the entity is managed, so the changes are saved when the transaction commits
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
  }

  /**
   * Physically deletes the warehouse row. The API never does this: warehouses are archived to keep
   * their history (see ArchiveWarehouseUseCase).
   */
  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = this.findById(warehouse.id);
    if (entity == null) {
      throw new IllegalArgumentException("Warehouse with id " + warehouse.id + " does not exist.");
    }
    this.delete(entity);
  }

  /** Returns the active warehouse with this business unit code, or null. */
  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return this.find("businessUnitCode = ?1 and archivedAt is null", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  /** Returns the active warehouse with this database id, or null. */
  public Warehouse findActiveById(Long id) {
    return this.find("id = ?1 and archivedAt is null", id)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }
}
