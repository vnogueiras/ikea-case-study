package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Product CRUD against the packaged app. Uses its own products, not the seed data. */
@QuarkusIntegrationTest
public class ProductEndpointIT {

  private static final String PATH = "product";

  @Test
  public void fullLifecycleCreateGetUpdateDelete() {
    String name = uniqueName();

    long id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + name + "\", \"stock\": 4}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().get(PATH + "/" + id).then().statusCode(200).body("stock", equalTo(4));

    String newName = uniqueName();
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\": \""
                + newName
                + "\", \"description\": \"desc\", \"price\": 9.99, \"stock\": 6}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(newName))
        .body("description", equalTo("desc"))
        .body("stock", equalTo(6));

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void invalidRequestsShouldBeRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\": 5, \"name\": \"" + uniqueName() + "\"}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
    given()
        .contentType(ContentType.JSON)
        .body("{\"stock\": 1}")
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422);
    given().when().get(PATH + "/999999").then().statusCode(404);
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  private static String uniqueName() {
    return "PRODUCT-IT-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
