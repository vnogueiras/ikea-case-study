package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the mapper directly: StoreResource has an identical ExceptionMapper<Exception>,
 * and JAX-RS only uses one of them at runtime, so this one is never reached by endpoint tests.
 */
public class ProductErrorMapperTest {

  private ProductResource.ErrorMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();
  }

  @Test
  void shouldUseStatusOfWebApplicationException() {
    Response response = mapper.toResponse(new WebApplicationException("Not here", 404));

    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(404, response.getStatus());
    assertEquals(404, body.get("code").asInt());
    assertEquals("Not here", body.get("error").asText());
  }

  @Test
  void shouldReturn500ForOtherExceptions() {
    Response response = mapper.toResponse(new IllegalStateException("Boom"));

    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(500, response.getStatus());
    assertEquals(IllegalStateException.class.getName(), body.get("exceptionType").asText());
  }

  @Test
  void shouldOmitErrorWhenExceptionHasNoMessage() {
    Response response = mapper.toResponse(new RuntimeException());

    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(500, response.getStatus());
    assertFalse(body.has("error"));
  }
}
