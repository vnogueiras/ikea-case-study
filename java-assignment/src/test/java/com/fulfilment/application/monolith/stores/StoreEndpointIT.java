package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Store CRUD against the packaged app, including the (real) legacy system call. */
@QuarkusIntegrationTest
public class StoreEndpointIT {

  private static final String PATH = "store";

  @Test
  public void fullLifecycleCreateUpdatePatchDelete() {
    String name = uniqueName();

    long id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + name + "\", \"quantityProductsInStock\": 3}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("name", equalTo(name))
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().get(PATH + "/" + id).then().statusCode(200).body("name", equalTo(name));

    String newName = uniqueName();
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"" + newName + "\", \"quantityProductsInStock\": 8}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(newName))
        .body("quantityProductsInStock", equalTo(8));

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"" + newName + "\", \"quantityProductsInStock\": 12}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", equalTo(12));

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void listShouldContainSeedStores() {
    given().when().get(PATH).then().statusCode(200).body("name", hasItems("KALLAX", "BESTÅ"));
  }

  @Test
  public void createShouldFailForDuplicateName() {
    // name is unique; the commit fails and an error is returned
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"KALLAX\"}")
        .when()
        .post(PATH)
        .then()
        .statusCode(500);
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
        .body("{\"quantityProductsInStock\": 1}")
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422);
    given().when().get(PATH + "/999999").then().statusCode(404);
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  private static String uniqueName() {
    return "STORE-IT-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
