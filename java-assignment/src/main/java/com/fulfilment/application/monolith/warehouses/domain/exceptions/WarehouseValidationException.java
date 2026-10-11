package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** A business rule for a warehouse operation was not met. */
public class WarehouseValidationException extends RuntimeException {

  public WarehouseValidationException(String message) {
    super(message);
  }
}
