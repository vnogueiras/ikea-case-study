package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
public class StoreResourceTest {

  private static final String PATH = "store";

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  // ---------- create ----------

  @Test
  public void createShouldCallLegacySystemOnlyAfterStoreIsCommitted() {
    String name = uniqueName();
    AtomicReference<Store> storedWhenLegacyWasCalled = new AtomicReference<>();

    // When the legacy system is called, read the store back in a separate transaction:
    // it can only be found if our transaction was already committed.
    doAnswer(
            invocation -> {
              storedWhenLegacyWasCalled.set(findByNameInNewTransaction(name));
              return null;
            })
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any());

    given()
        .contentType(ContentType.JSON)
        .body(storeJson(name, 7))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo(name));

    ArgumentCaptor<Store> sent = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(sent.capture());
    assertNotNull(sent.getValue().id);
    assertEquals(name, sent.getValue().name);
    assertEquals(7, sent.getValue().quantityProductsInStock);

    assertNotNull(storedWhenLegacyWasCalled.get(), "Store was not committed before legacy call");
  }

  @Test
  public void createShouldNotCallLegacySystemWhenCommitFails() {
    // TONSTAD already exists (import.sql) and the name column is unique, so the commit fails
    given()
        .contentType(ContentType.JSON)
        .body(storeJson("TONSTAD", 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(greaterThanOrEqualTo(400));

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
  }

  @Test
  public void createShouldRejectRequestWithId() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\": 99, \"name\": \"" + uniqueName() + "\"}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
  }

  // ---------- update (PUT) ----------

  @Test
  public void updateShouldCallLegacySystemWithCommittedStore() {
    long id = createStore(uniqueName(), 5);
    String newName = uniqueName();
    AtomicReference<Store> storedWhenLegacyWasCalled = new AtomicReference<>();

    doAnswer(
            invocation -> {
              storedWhenLegacyWasCalled.set(findByNameInNewTransaction(newName));
              return null;
            })
        .when(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(any());

    given()
        .contentType(ContentType.JSON)
        .body(storeJson(newName, 12))
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(newName))
        .body("quantityProductsInStock", equalTo(12));

    ArgumentCaptor<Store> sent = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(sent.capture());
    assertEquals(id, sent.getValue().id);
    assertEquals(newName, sent.getValue().name);
    assertEquals(12, sent.getValue().quantityProductsInStock);

    assertNotNull(storedWhenLegacyWasCalled.get(), "Update was not committed before legacy call");
  }

  @Test
  public void updateShouldNotCallLegacySystemWhenCommitFails() {
    long id = createStore(uniqueName(), 5);

    // Renaming to an existing name violates the unique constraint on commit
    given()
        .contentType(ContentType.JSON)
        .body(storeJson("KALLAX", 5))
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(greaterThanOrEqualTo(400));

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  public void updateShouldReturn404ForUnknownStore() {
    given()
        .contentType(ContentType.JSON)
        .body(storeJson(uniqueName(), 1))
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  public void updateShouldReturn422WhenNameIsMissing() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\": 1}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  // ---------- patch ----------

  @Test
  public void patchShouldUpdateGivenFieldsAndCallLegacySystem() {
    long id = createStore(uniqueName(), 5);
    String newName = uniqueName();

    given()
        .contentType(ContentType.JSON)
        .body(storeJson(newName, 20))
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(newName))
        .body("quantityProductsInStock", equalTo(20));

    ArgumentCaptor<Store> sent = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(sent.capture());
    assertEquals(id, sent.getValue().id);
    assertEquals(newName, sent.getValue().name);
  }

  @Test
  public void patchShouldKeepQuantityWhenNotProvided() {
    long id = createStore(uniqueName(), 5);
    String newName = uniqueName();

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"" + newName + "\"}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(newName))
        .body("quantityProductsInStock", equalTo(5));
  }

  @Test
  public void patchShouldReturn404ForUnknownStore() {
    given()
        .contentType(ContentType.JSON)
        .body(storeJson(uniqueName(), 1))
        .when()
        .patch(PATH + "/999999")
        .then()
        .statusCode(404);

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  public void patchShouldReturn422WhenNameIsMissing() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\": 1}")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  // ---------- get / delete ----------

  @Test
  public void listShouldReturnStoresSortedByName() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body("name", hasItems("KALLAX", "BESTÅ"));
  }

  @Test
  public void getSingleShouldReturnExistingStore() {
    long id = createStore(uniqueName(), 4);

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", equalTo(4));
  }

  @Test
  public void getSingleShouldReturn404ForUnknownStore() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void deleteShouldRemoveStore() {
    long id = createStore(uniqueName(), 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void deleteShouldReturn404ForUnknownStore() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  // ---------- helpers ----------

  /** Creates a store through the API and clears the mock so tests only see their own calls. */
  private long createStore(String name, int quantity) {
    long id =
        given()
            .contentType(ContentType.JSON)
            .body(storeJson(name, quantity))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    Mockito.clearInvocations(legacyStoreManagerGateway);
    return id;
  }

  private static Store findByNameInNewTransaction(String name) {
    return QuarkusTransaction.requiringNew()
        .call(() -> Store.<Store>find("name", name).firstResult());
  }

  private static String storeJson(String name, int quantity) {
    return "{\"name\": \"" + name + "\", \"quantityProductsInStock\": " + quantity + "}";
  }

  private static String uniqueName() {
    return "STORE-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
