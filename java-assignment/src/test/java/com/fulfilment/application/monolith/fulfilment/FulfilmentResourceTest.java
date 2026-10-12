package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Each test creates its own store and product, so tests don't affect each other. The warehouses
 * come from import.sql: MWH.001, MWH.012 and MWH.023.
 */
@QuarkusTest
public class FulfilmentResourceTest {

  private static final String PATH = "fulfilment";

  private long storeId;
  private long productId;

  @BeforeEach
  void setUp() {
    storeId = createStore();
    productId = createProduct();
  }

  @Test
  public void assignShouldLinkWarehouseToProductForStore() {
    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("storeId", equalTo((int) storeId))
        .body("productId", equalTo((int) productId))
        .body("warehouseBusinessUnitCode", equalTo("MWH.001"));

    given()
        .when()
        .get(PATH + "?storeId=" + storeId + "&productId=" + productId)
        .then()
        .statusCode(200)
        .body("$", hasSize(1));
  }

  @Test
  public void productCanHaveTwoWarehousesPerStoreButNotThree() {
    assign(storeId, productId, "MWH.001");
    assign(storeId, productId, "MWH.012");

    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, "MWH.023"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 2 warehouses"));
  }

  @Test
  public void limitIsPerStore() {
    assign(storeId, productId, "MWH.001");
    assign(storeId, productId, "MWH.012");

    // the same product can still get 2 warehouses for another store
    long otherStoreId = createStore();
    assign(otherStoreId, productId, "MWH.012");
    assign(otherStoreId, productId, "MWH.023");
  }

  @Test
  public void sameWarehouseCannotBeAssignedTwice() {
    assign(storeId, productId, "MWH.001");

    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409);
  }

  @Test
  public void removingAnAssignmentFreesASlot() {
    long id = assign(storeId, productId, "MWH.001");
    assign(storeId, productId, "MWH.012");

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    assign(storeId, productId, "MWH.023");
  }

  @Test
  public void removeShouldReturn404ForUnknownAssignment() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void assignShouldReturn404ForUnknownStoreProductOrWarehouse() {
    assertStatus(json(999999L, productId, "MWH.001"), 404);
    assertStatus(json(storeId, 999999L, "MWH.001"), 404);
    assertStatus(json(storeId, productId, "MWH.UNKNOWN"), 404);
  }

  @Test
  public void assignShouldReturn400ForMissingFields() {
    assertStatus("{\"storeId\": " + storeId + ", \"productId\": " + productId + "}", 400);
    assertStatus("{\"productId\": " + productId + ", \"warehouseBusinessUnitCode\": \"MWH.001\"}", 400);
  }

  @Test
  public void listShouldFilterByStoreOrProduct() {
    assign(storeId, productId, "MWH.001");

    given().when().get(PATH + "?storeId=" + storeId).then().statusCode(200).body("$", hasSize(1));
    given().when().get(PATH + "?productId=" + productId).then().statusCode(200).body("$", hasSize(1));
    given().when().get(PATH).then().statusCode(200);
  }

  // ---------- helpers ----------

  private long assign(long store, long product, String warehouse) {
    return given()
        .contentType(ContentType.JSON)
        .body(json(store, product, warehouse))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private void assertStatus(String body, int status) {
    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(status);
  }

  private static String json(Long store, Long product, String warehouse) {
    return String.format(
        "{\"storeId\": %d, \"productId\": %d, \"warehouseBusinessUnitCode\": \"%s\"}",
        store, product, warehouse);
  }

  private static long createStore() {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"STORE-" + UUID.randomUUID().toString().substring(0, 8) + "\"}")
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static long createProduct() {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"PRODUCT-" + UUID.randomUUID().toString().substring(0, 8) + "\"}")
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }
}
