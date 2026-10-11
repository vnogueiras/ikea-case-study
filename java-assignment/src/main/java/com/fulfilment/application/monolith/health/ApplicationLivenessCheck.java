package com.fulfilment.application.monolith.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Liveness: the application is running and able to answer. It deliberately does not check the
 * database, so a database outage marks the app as not ready instead of getting it restarted.
 */
@Liveness
@ApplicationScoped
public class ApplicationLivenessCheck implements HealthCheck {

  static final String NAME = "Application";

  @ConfigProperty(name = "quarkus.application.version", defaultValue = "unknown")
  String version;

  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.named(NAME).up().withData("version", version).build();
  }
}
