package com.fulfilment.application.monolith.fulfilment;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Translates fulfilment errors into HTTP responses with a JSON body. */
@Provider
public class FulfilmentExceptionMapper implements ExceptionMapper<FulfilmentException> {

  @Override
  public Response toResponse(FulfilmentException exception) {
    int status = statusFor(exception);
    return Response.status(status)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("code", status, "error", exception.getMessage()))
        .build();
  }

  static int statusFor(FulfilmentException exception) {
    if (exception instanceof FulfilmentException.NotFound) {
      return 404;
    }
    if (exception instanceof FulfilmentException.Conflict) {
      return 409;
    }
    return 400;
  }
}
