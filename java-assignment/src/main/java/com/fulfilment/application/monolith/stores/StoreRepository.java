package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** Repository access to stores, so other services can use (and mock) it instead of static calls. */
@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {}
