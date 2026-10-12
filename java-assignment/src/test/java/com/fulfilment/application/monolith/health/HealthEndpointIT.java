package com.fulfilment.application.monolith.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

/** The packaged app starts, connects to the real database and reports itself healthy. */
@QuarkusIntegrationTest
public class HealthEndpointIT {

  @Test
  public void packagedAppShouldBeReadyAndLive() {
    given()
        .when()
        .get("/q/health/ready")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"))
        .body("checks.name", hasItems("Database connections health check", "Location data"));

    given().when().get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"));
  }
}
