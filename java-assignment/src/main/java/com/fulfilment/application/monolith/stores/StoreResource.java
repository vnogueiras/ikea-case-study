package com.fulfilment.application.monolith.stores;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  public Response create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    // Commit first, so the legacy system only receives data that is stored in our database
    QuarkusTransaction.requiringNew().run(store::persist);

    legacyStoreManagerGateway.createStoreOnLegacySystem(store);

    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity =
        QuarkusTransaction.requiringNew()
            .call(
                () -> {
                  Store existing = findExisting(id);
                  existing.name = updatedStore.name;
                  existing.quantityProductsInStock = updatedStore.quantityProductsInStock;
                  return existing;
                });

    legacyStoreManagerGateway.updateStoreOnLegacySystem(entity);

    return entity;
  }

  @PATCH
  @Path("{id}")
  public Store patch(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity =
        QuarkusTransaction.requiringNew()
            .call(
                () -> {
                  Store existing = findExisting(id);
                  if (updatedStore.name != null) {
                    existing.name = updatedStore.name;
                  }
                  if (updatedStore.quantityProductsInStock != 0) {
                    existing.quantityProductsInStock = updatedStore.quantityProductsInStock;
                  }
                  return existing;
                });

    legacyStoreManagerGateway.updateStoreOnLegacySystem(entity);

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    return Response.status(204).build();
  }

  private Store findExisting(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }
}
