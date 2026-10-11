package com.fulfilment.application.monolith.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.location.LocationGateway;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

public class LocationDataHealthCheckTest {

  @Test
  void shouldBeUpWhenLocationsAreLoaded() {
    LocationDataHealthCheck check = new LocationDataHealthCheck();
    check.locationGateway = new LocationGateway();

    HealthCheckResponse response = check.call();

    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
  }

  @Test
  void shouldBeDownWhenNoLocations() {
    LocationDataHealthCheck check = new LocationDataHealthCheck();
    check.locationGateway = mock(LocationGateway.class);
    when(check.locationGateway.countLocations()).thenReturn(0);

    HealthCheckResponse response = check.call();

    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
  }
}
