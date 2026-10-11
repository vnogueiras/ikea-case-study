package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Seed data (import.sql): MWH.001 @ ZWOLLE-001, MWH.012 @ AMSTERDAM-001, MWH.023 @ TILBURG-001. */
@QuarkusTest
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  @Test
  public void createShouldStoreValidWarehouse() {
    String buCode = uniqueBuCode();

    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson(buCode, "AMSTERDAM-002", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body("id", notNullValue())
        .body("businessUnitCode", equalTo(buCode));

    given().when().get(PATH).then().statusCode(200).body(containsString(buCode));
  }

  @Test
  public void createShouldRejectExistingBusinessUnitCode() {
    assertRejected(warehouseJson("MWH.012", "AMSTERDAM-002", 10, 1), "already exists");
  }

  @Test
  public void createShouldRejectUnknownLocation() {
    assertRejected(warehouseJson(uniqueBuCode(), "NOWHERE-001", 10, 1), "not a valid");
  }

  @Test
  public void createShouldRejectFullLocation() {
    // TILBURG-001 allows 1 warehouse and already has MWH.023
    assertRejected(warehouseJson(uniqueBuCode(), "TILBURG-001", 5, 1), "maximum number");
  }

  @Test
  public void createShouldRejectCapacityAboveLocationMax() {
    // EINDHOVEN-001 max capacity is 70
    assertRejected(warehouseJson(uniqueBuCode(), "EINDHOVEN-001", 71, 1), "capacity");
  }

  @Test
  public void createShouldRejectStockAboveCapacity() {
    // VETSBY-001 is empty, so only the stock rule can fail
    assertRejected(warehouseJson(uniqueBuCode(), "VETSBY-001", 5, 6), "exceeds");
  }

  @Test
  public void getShouldReturnExistingWarehouse() {
    // id 2 is MWH.012 in import.sql
    given()
        .when()
        .get(PATH + "/2")
        .then()
        .statusCode(200)
        .body("id", equalTo("2"))
        .body("businessUnitCode", equalTo("MWH.012"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(50))
        .body("stock", equalTo(5));
  }

  @Test
  public void getShouldReturnCreatedWarehouse() {
    String buCode = uniqueBuCode();
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(warehouseJson(buCode, "ZWOLLE-002", 10, 2))
            .when()
            .post(PATH)
            .then()
            .statusCode(200)
            .extract()
            .path("id");

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo(buCode))
        .body("location", equalTo("ZWOLLE-002"));
  }

  @Test
  public void getShouldReturn404ForUnknownId() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void getShouldReturn404ForNonNumericId() {
    given().when().get(PATH + "/abc").then().statusCode(404);
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

  private static String warehouseJson(String buCode, String location, int capacity, int stock) {
    return String.format(
        "{\"businessUnitCode\": \"%s\", \"location\": \"%s\", \"capacity\": %d, \"stock\": %d}",
        buCode, location, capacity, stock);
  }

  private static String uniqueBuCode() {
    return "MWH.T" + UUID.randomUUID().toString().substring(0, 6);
  }
}
