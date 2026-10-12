package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Fulfilment assignments against the packaged app. Uses MWH.012 and MWH.023 from the seed data
 * (MWH.001 is archived by WarehouseEndpointIT) and creates extra warehouses in EINDHOVEN-001
 * and AMSTERDAM-001.
 */
@QuarkusIntegrationTest
public class FulfilmentEndpointIT {

  private static final String PATH = "fulfilment";

  @Test
  public void productCanBeFulfilledByMaxTwoWarehousesPerStore() {
    long storeId = create("store", "{\"name\": \"STORE-IT-" + shortId() + "\"}");
    long productId = create("product", "{\"name\": \"PRODUCT-IT-" + shortId() + "\"}");
    String thirdWarehouse = createWarehouse();

    long firstId = assign(storeId, productId, "MWH.012");
    assign(storeId, productId, "MWH.023");

    // a third warehouse for the same product and store is rejected
    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, thirdWarehouse))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum"));

    given()
        .when()
        .get(PATH + "?storeId=" + storeId + "&productId=" + productId)
        .then()
        .statusCode(200)
        .body("$", hasSize(2));

    // after removing one, the third warehouse can be assigned
    given().when().delete(PATH + "/" + firstId).then().statusCode(204);
    assign(storeId, productId, thirdWarehouse);
  }

  @Test
  public void archivedWarehouseCannotBeAssigned() {
    long storeId = create("store", "{\"name\": \"STORE-IT-" + shortId() + "\"}");
    long productId = create("product", "{\"name\": \"PRODUCT-IT-" + shortId() + "\"}");
    String buCode = createWarehouse();
    String warehouseId =
        given().when().get("warehouse").then().statusCode(200).extract()
            .path("find { it.businessUnitCode == '" + buCode + "' }.id");
    given().when().delete("warehouse/" + warehouseId).then().statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, buCode))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void storeCanBeFulfilledByMaxThreeDifferentWarehouses() {
    long storeId = create("store", "{\"name\": \"STORE-IT-" + shortId() + "\"}");
    long[] products = new long[4];
    for (int i = 0; i < products.length; i++) {
      products[i] = create("product", "{\"name\": \"PRODUCT-IT-" + shortId() + "\"}");
    }
    String thirdWarehouse = createWarehouse("AMSTERDAM-001");
    String fourthWarehouse = createWarehouse("AMSTERDAM-001");

    assign(storeId, products[0], "MWH.012");
    assign(storeId, products[1], "MWH.023");
    assign(storeId, products[2], thirdWarehouse);

    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, products[3], fourthWarehouse))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 3 warehouses"));

    // re-using one of the store's warehouses is allowed
    assign(storeId, products[3], "MWH.012");
  }

  @Test
  public void warehouseCanStoreMaxFiveProductTypes() {
    long storeId = create("store", "{\"name\": \"STORE-IT-" + shortId() + "\"}");
    String warehouse = createWarehouse("AMSTERDAM-001");
    long[] products = new long[6];
    for (int i = 0; i < products.length; i++) {
      products[i] = create("product", "{\"name\": \"PRODUCT-IT-" + shortId() + "\"}");
    }

    for (int i = 0; i < 5; i++) {
      assign(storeId, products[i], warehouse);
    }

    // a 6th product type is rejected
    given()
        .contentType(ContentType.JSON)
        .body(json(storeId, products[5], warehouse))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 5 product types"));

    // a product the warehouse already stores is fine for another store
    long otherStoreId = create("store", "{\"name\": \"STORE-IT-" + shortId() + "\"}");
    assign(otherStoreId, products[0], warehouse);
  }

  // ---------- helpers ----------

  private long assign(long storeId, long productId, String warehouse) {
    return given()
        .contentType(ContentType.JSON)
        .body(json(storeId, productId, warehouse))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static long create(String path, String body) {
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static String createWarehouse() {
    return createWarehouse("EINDHOVEN-001");
  }

  private static String createWarehouse(String location) {
    String buCode = "MWH.IT" + shortId();
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"businessUnitCode\": \""
                + buCode
                + "\", \"location\": \""
                + location
                + "\", \"capacity\": 10, \"stock\": 1}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(200);
    return buCode;
  }

  private static String json(long storeId, long productId, String warehouse) {
    return String.format(
        "{\"storeId\": %d, \"productId\": %d, \"warehouseBusinessUnitCode\": \"%s\"}",
        storeId, productId, warehouse);
  }

  private static String shortId() {
    return UUID.randomUUID().toString().substring(0, 6);
  }
}
