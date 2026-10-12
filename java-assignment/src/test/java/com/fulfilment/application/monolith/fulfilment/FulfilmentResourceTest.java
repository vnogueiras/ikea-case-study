package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Each test creates its own store, product and warehouses, so the limits (2 warehouses per product
 * per store, 3 warehouses per store, 5 products per warehouse) never leak between tests. The
 * warehouses are created in AMSTERDAM-001 and archived after each test to free the location again.
 */
@QuarkusTest
public class FulfilmentResourceTest {

  private static final String PATH = "fulfilment";

  private long storeId;
  private long productId;
  private final List<String> createdWarehouseIds = new ArrayList<>();

  @BeforeEach
  void setUp() {
    storeId = createStore();
    productId = createProduct();
  }

  @AfterEach
  void archiveCreatedWarehouses() {
    createdWarehouseIds.forEach(id -> given().when().delete("warehouse/" + id));
    createdWarehouseIds.clear();
  }

  // ---------- assign / list / remove ----------

  @Test
  public void assignShouldLinkWarehouseToProductForStore() {
    String warehouse = createWarehouse();

    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, warehouse))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("storeId", equalTo((int) storeId))
        .body("productId", equalTo((int) productId))
        .body("warehouseBusinessUnitCode", equalTo(warehouse));

    given()
        .when()
        .get(PATH + "?storeId=" + storeId + "&productId=" + productId)
        .then()
        .statusCode(200)
        .body("$", hasSize(1));
  }

  @Test
  public void listShouldFilterByStoreOrProduct() {
    assign(storeId, productId, createWarehouse());

    given().when().get(PATH + "?storeId=" + storeId).then().statusCode(200).body("$", hasSize(1));
    given()
        .when()
        .get(PATH + "?productId=" + productId)
        .then()
        .statusCode(200)
        .body("$", hasSize(1));
    given().when().get(PATH).then().statusCode(200);
  }

  @Test
  public void removingAnAssignmentFreesASlot() {
    String w1 = createWarehouse();
    String w2 = createWarehouse();
    String w3 = createWarehouse();
    long id = assign(storeId, productId, w1);
    assign(storeId, productId, w2);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    assign(storeId, productId, w3);
  }

  @Test
  public void removeShouldReturn404ForUnknownAssignment() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  // ---------- validation ----------

  @Test
  public void sameWarehouseCannotBeAssignedTwice() {
    String warehouse = createWarehouse();
    assign(storeId, productId, warehouse);

    assertStatus(json(storeId, productId, warehouse), 409);
  }

  @Test
  public void assignShouldReturn404ForUnknownStoreProductOrWarehouse() {
    String warehouse = createWarehouse();

    assertStatus(json(999999L, productId, warehouse), 404);
    assertStatus(json(storeId, 999999L, warehouse), 404);
    assertStatus(json(storeId, productId, "MWH.UNKNOWN"), 404);
  }

  @Test
  public void assignShouldReturn400ForMissingFields() {
    assertStatus("{\"storeId\": " + storeId + ", \"productId\": " + productId + "}", 400);
    assertStatus("{\"productId\": " + productId + ", \"warehouseBusinessUnitCode\": \"X\"}", 400);
  }

  // ---------- constraint 1: max 2 warehouses per product per store ----------

  @Test
  public void productCanHaveTwoWarehousesPerStoreButNotThree() {
    String w1 = createWarehouse();
    String w2 = createWarehouse();
    String w3 = createWarehouse();
    assign(storeId, productId, w1);
    assign(storeId, productId, w2);

    assertRejected(json(storeId, productId, w3), "maximum of 2 warehouses");
  }

  @Test
  public void productLimitIsPerStore() {
    String w1 = createWarehouse();
    String w2 = createWarehouse();
    assign(storeId, productId, w1);
    assign(storeId, productId, w2);

    // the same product can still get 2 warehouses for another store
    long otherStoreId = createStore();
    assign(otherStoreId, productId, w1);
    assign(otherStoreId, productId, w2);
  }

  // ---------- constraint 2: max 3 warehouses per store ----------

  @Test
  public void storeCanBeFulfilledByMaxThreeDifferentWarehouses() {
    String w1 = createWarehouse();
    String w2 = createWarehouse();
    String w3 = createWarehouse();
    String w4 = createWarehouse();
    long product2 = createProduct();
    long product3 = createProduct();
    long product4 = createProduct();

    assign(storeId, productId, w1);
    assign(storeId, product2, w2);
    assign(storeId, product3, w3);

    assertRejected(json(storeId, product4, w4), "maximum of 3 warehouses");

    // a warehouse the store already uses is fine for another product
    assign(storeId, product4, w1);
  }

  // ---------- constraint 3: max 5 product types per warehouse ----------

  @Test
  public void warehouseCanStoreMaxFiveProductTypes() {
    String warehouse = createWarehouse();
    for (int i = 0; i < 4; i++) {
      assign(storeId, createProduct(), warehouse);
    }
    assign(storeId, productId, warehouse); // 5th product type

    assertRejected(json(storeId, createProduct(), warehouse), "maximum of 5 product types");

    // a product the warehouse already stores is fine for another store
    assign(createStore(), productId, warehouse);
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

  private void assertRejected(String body, String messagePart) {
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString(messagePart));
  }

  private void assertStatus(String body, int status) {
    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(status);
  }

  private static String json(Long store, Long product, String warehouse) {
    return String.format(
        "{\"storeId\": %d, \"productId\": %d, \"warehouseBusinessUnitCode\": \"%s\"}",
        store, product, warehouse);
  }

  /** Creates an active warehouse in AMSTERDAM-001 and remembers it for clean-up. */
  private String createWarehouse() {
    String buCode = "MWH.F" + UUID.randomUUID().toString().substring(0, 6);
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"businessUnitCode\": \""
                    + buCode
                    + "\", \"location\": \"AMSTERDAM-001\", \"capacity\": 5, \"stock\": 1}")
            .when()
            .post("warehouse")
            .then()
            .statusCode(200)
            .extract()
            .path("id");
    createdWarehouseIds.add(id);
    return buCode;
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
