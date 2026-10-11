package com.fulfilment.application.monolith.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class HealthEndpointTest {

  @Test
  public void readinessShouldBeUpWithDatabaseAndLocationChecks() {
    given()
        .when()
        .get("/q/health/ready")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"))
        .body("checks.name", hasItems("Database connections health check", "Location data"));
  }

  @Test
  public void livenessShouldBeUp() {
    given()
        .when()
        .get("/q/health/live")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"))
        .body("checks.name", hasItem("Application"));
  }

  @Test
  public void overallHealthShouldBeUp() {
    given().when().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
  }
}
