package com.fulfilment.application.monolith.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  /** Request and response body: which warehouse fulfils which product for which store. */
  public record FulfilmentDto(
      Long id, Long storeId, Long productId, String warehouseBusinessUnitCode) {

    static FulfilmentDto from(FulfilmentAssignment assignment) {
      return new FulfilmentDto(
          assignment.id,
          assignment.store.id,
          assignment.product.id,
          assignment.warehouseBusinessUnitCode);
    }
  }

  @Inject FulfilmentService fulfilmentService;
  @Inject FulfilmentAssignmentRepository assignmentRepository;

  @GET
  public List<FulfilmentDto> list(
      @QueryParam("storeId") Long storeId, @QueryParam("productId") Long productId) {
    return assignmentRepository.search(storeId, productId).stream()
        .map(FulfilmentDto::from)
        .toList();
  }

  @POST
  public Response assign(FulfilmentDto request) {
    if (request == null) {
      throw new WebApplicationException("Request body is required.", 400);
    }
    var assignment =
        fulfilmentService.assign(
            request.storeId(), request.productId(), request.warehouseBusinessUnitCode());
    return Response.status(201).entity(FulfilmentDto.from(assignment)).build();
  }

  @DELETE
  @Path("{id}")
  public Response remove(@PathParam("id") Long id) {
    fulfilmentService.remove(id);
    return Response.status(204).build();
  }
}
