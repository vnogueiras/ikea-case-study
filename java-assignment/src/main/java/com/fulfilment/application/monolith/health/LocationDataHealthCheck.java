package com.fulfilment.application.monolith.health;

import com.fulfilment.application.monolith.location.LocationGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Ready only when location data is available: creating or replacing a warehouse is impossible
 * without it.
 */
@Readiness
@ApplicationScoped
public class LocationDataHealthCheck implements HealthCheck {

  static final String NAME = "Location data";

  @Inject LocationGateway locationGateway;

  @Override
  public HealthCheckResponse call() {
    int locations = locationGateway.countLocations();
    return HealthCheckResponse.named(NAME)
        .status(locations > 0)
        .withData("locations", locations)
        .build();
  }
}
