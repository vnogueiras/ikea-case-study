package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Returns a 400 with the validation message when a warehouse business rule fails. */
@Provider
public class WarehouseValidationExceptionMapper
    implements ExceptionMapper<WarehouseValidationException> {

  @Override
  public Response toResponse(WarehouseValidationException exception) {
    return Response.status(400)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("code", 400, "error", exception.getMessage()))
        .build();
  }
}
