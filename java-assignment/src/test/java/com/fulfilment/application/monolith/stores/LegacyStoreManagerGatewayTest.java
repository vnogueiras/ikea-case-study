package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/** The gateway is mocked in StoreResourceTest, so its own behaviour is tested here. */
public class LegacyStoreManagerGatewayTest {

  private final LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();

  @Test
  void createShouldSendStoreToLegacySystem() {
    Store store = new Store("TEST-STORE");
    store.quantityProductsInStock = 3;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void updateShouldSendStoreToLegacySystem() {
    Store store = new Store("TEST-STORE");
    store.quantityProductsInStock = 5;

    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }

  @Test
  void shouldNotThrowWhenLegacyCallFails() {
    // a "/" in the name is not allowed in a temp file name, so writing fails inside the gateway
    Store store = new Store("INVALID/NAME");

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }
}
