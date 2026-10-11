package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** CRUD tests for /product. Uses its own products so it doesn't depend on ProductEndpointTest. */
@QuarkusTest
public class ProductResourceTest {

  private static final String PATH = "product";

  @Test
  public void getSingleShouldReturnExistingProduct() {
    // id 2 is KALLAX in import.sql
    given()
        .when()
        .get(PATH + "/2")
        .then()
        .statusCode(200)
        .body("name", equalTo("KALLAX"))
        .body("stock", equalTo(5));
  }

  @Test
  public void getSingleShouldReturn404ForUnknownProduct() {
    given()
        .when()
        .get(PATH + "/999999")
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  @Test
  public void createShouldStoreProduct() {
    String name = uniqueName();

    long id =
        given()
            .contentType(ContentType.JSON)
            .body(productJson(name, 7))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo(name))
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().get(PATH + "/" + id).then().statusCode(200).body("stock", equalTo(7));
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
  }

  @Test
  public void updateShouldChangeAllFields() {
    long id = createProduct(uniqueName(), 1);
    String newName = uniqueName();

    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\": \""
                + newName
                + "\", \"description\": \"new description\", \"price\": 12.50, \"stock\": 9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(newName))
        .body("description", equalTo("new description"))
        .body("price", equalTo(12.50f))
        .body("stock", equalTo(9));
  }

  @Test
  public void updateShouldReturn422WhenNameIsMissing() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"stock\": 1}")
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void updateShouldReturn404ForUnknownProduct() {
    given()
        .contentType(ContentType.JSON)
        .body(productJson(uniqueName(), 1))
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void deleteShouldRemoveProduct() {
    long id = createProduct(uniqueName(), 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void deleteShouldReturn404ForUnknownProduct() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  private long createProduct(String name, int stock) {
    return given()
        .contentType(ContentType.JSON)
        .body(productJson(name, stock))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static String productJson(String name, int stock) {
    return "{\"name\": \"" + name + "\", \"stock\": " + stock + "}";
  }

  private static String uniqueName() {
    return "PRODUCT-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
