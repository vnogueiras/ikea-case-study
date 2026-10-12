package com.fulfilment.application.monolith.fulfilment;

/**
 * Base class for fulfilment errors. The service only says what went wrong; {@link
 * FulfilmentExceptionMapper} decides which HTTP status that is.
 */
public abstract class FulfilmentException extends RuntimeException {

  protected FulfilmentException(String message) {
    super(message);
  }

  /** A store, product, warehouse or assignment does not exist. */
  public static class NotFound extends FulfilmentException {
    public NotFound(String message) {
      super(message);
    }
  }

  /** The assignment already exists. */
  public static class Conflict extends FulfilmentException {
    public Conflict(String message) {
      super(message);
    }
  }

  /** The request is incomplete or breaks one of the fulfilment limits. */
  public static class Invalid extends FulfilmentException {
    public Invalid(String message) {
      super(message);
    }
  }
}
