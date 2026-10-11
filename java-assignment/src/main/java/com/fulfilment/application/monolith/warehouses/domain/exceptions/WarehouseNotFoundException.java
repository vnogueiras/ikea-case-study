package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** No active warehouse exists for the given identifier. */
public class WarehouseNotFoundException extends RuntimeException {

  public WarehouseNotFoundException(String message) {
    super(message);
  }
}
