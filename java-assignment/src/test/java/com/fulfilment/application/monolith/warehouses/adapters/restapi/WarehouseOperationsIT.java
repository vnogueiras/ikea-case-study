package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * End-to-end warehouse flows against the packaged app and a real Postgres. All IT classes share
 * one database, so this class only creates warehouses in AMSTERDAM-002 and ZWOLLE-002.
 */
@QuarkusIntegrationTest
public class WarehouseOperationsIT {

  private static final String PATH = "warehouse";

  @Test
  public void fullLifecycleCreateGetReplaceArchive() {
    String buCode = uniqueBuCode();

    // create
    String firstId =
        given()
            .contentType(ContentType.JSON)
            .body(json(buCode, "AMSTERDAM-002", 20, 10))
            .when()
            .post(PATH)
            .then()
            .statusCode(200)
            .body("businessUnitCode", equalTo(buCode))
            .extract()
            .path("id");

    // get
    given()
        .when()
        .get(PATH + "/" + firstId)
        .then()
        .statusCode(200)
        .body("location", equalTo("AMSTERDAM-002"))
        .body("capacity", equalTo(20))
        .body("stock", equalTo(10));

    // replace: old one is archived, new one takes over the business unit code
    String secondId =
        given()
            .contentType(ContentType.JSON)
            .body(json(buCode, "AMSTERDAM-002", 30, 10))
            .when()
            .post(PATH + "/" + buCode + "/replacement")
            .then()
            .statusCode(200)
            .body("capacity", equalTo(30))
            .extract()
            .path("id");

    given().when().get(PATH + "/" + firstId).then().statusCode(404);
    given().when().get(PATH + "/" + secondId).then().statusCode(200);

    // archive the replacement: the business unit disappears from the list
    given().when().delete(PATH + "/" + secondId).then().statusCode(204);
    given().when().get(PATH + "/" + secondId).then().statusCode(404);
    given().when().get(PATH).then().statusCode(200).body(not(containsString(buCode)));
  }

  @Test
  public void archivedBusinessUnitCodeCanBeReused() {
    String buCode = uniqueBuCode();
    String id = create(buCode, "ZWOLLE-002", 10, 1);
    given().when().delete(PATH + "/" + id).then().statusCode(204);

    create(buCode, "ZWOLLE-002", 10, 1);
  }

  @Test
  public void createShouldReturn400WhenBusinessRulesFail() {
    // existing business unit code
    assertCreateRejected(json("MWH.012", "AMSTERDAM-002", 10, 1), "already exists");
    // unknown location
    assertCreateRejected(json(uniqueBuCode(), "NOWHERE-001", 10, 1), "not a valid");
    // TILBURG-001 allows one warehouse and already has MWH.023
    assertCreateRejected(json(uniqueBuCode(), "TILBURG-001", 5, 1), "maximum number");
    // HELMOND-001 max capacity is 45
    assertCreateRejected(json(uniqueBuCode(), "HELMOND-001", 46, 1), "capacity");
    // stock above capacity
    assertCreateRejected(json(uniqueBuCode(), "HELMOND-001", 5, 6), "exceeds");
  }

  @Test
  public void replaceShouldKeepCurrentWarehouseWhenRulesFail() {
    String buCode = uniqueBuCode();
    String id = create(buCode, "AMSTERDAM-002", 10, 5);

    // stock must match
    given()
        .contentType(ContentType.JSON)
        .body(json(buCode, "AMSTERDAM-002", 10, 6))
        .when()
        .post(PATH + "/" + buCode + "/replacement")
        .then()
        .statusCode(400)
        .body("error", containsString("match"));

    // capacity must hold the current stock
    given()
        .contentType(ContentType.JSON)
        .body(json(buCode, "AMSTERDAM-002", 4, 5))
        .when()
        .post(PATH + "/" + buCode + "/replacement")
        .then()
        .statusCode(400);

    // nothing changed
    given().when().get(PATH + "/" + id).then().statusCode(200).body("capacity", equalTo(10));
  }

  @Test
  public void unknownWarehouseShouldReturn404() {
    given().when().get(PATH + "/999999").then().statusCode(404);
    given().when().delete(PATH + "/999999").then().statusCode(404);
    given()
        .contentType(ContentType.JSON)
        .body(json("MWH.UNKNOWN", "AMSTERDAM-002", 10, 1))
        .when()
        .post(PATH + "/MWH.UNKNOWN/replacement")
        .then()
        .statusCode(404);
  }

  // ---------- helpers ----------

  private void assertCreateRejected(String body, String messagePart) {
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString(messagePart));
  }

  private String create(String buCode, String location, int capacity, int stock) {
    return given()
        .contentType(ContentType.JSON)
        .body(json(buCode, location, capacity, stock))
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .extract()
        .path("id");
  }

  private static String json(String buCode, String location, int capacity, int stock) {
    return String.format(
        "{\"businessUnitCode\": \"%s\", \"location\": \"%s\", \"capacity\": %d, \"stock\": %d}",
        buCode, location, capacity, stock);
  }

  private static String uniqueBuCode() {
    return "MWH.IT" + UUID.randomUUID().toString().substring(0, 6);
  }
}
