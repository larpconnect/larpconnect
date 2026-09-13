package com.larpconnect.njall.api.admin;

/** Response protocol emitted by the health check actor. */
public sealed interface HealthCheckResponse {

  /** Indicates all evaluated health checks succeeded. */
  record Healthy() implements HealthCheckResponse {}

  /**
   * Indicates at least one health check failed or encountered an error.
   *
   * @param reason The diagnostic failure reason.
   */
  record Unhealthy(String reason) implements HealthCheckResponse {}

  /**
   * Pure factory method constructing a healthy response.
   *
   * @return A {@link Healthy} response instance.
   */
  static HealthCheckResponse healthy() {
    return new Healthy();
  }

  /**
   * Pure factory method constructing an unhealthy response.
   *
   * @param reason The failure explanation.
   * @return An {@link Unhealthy} response instance.
   */
  static HealthCheckResponse unhealthy(String reason) {
    return new Unhealthy(reason);
  }
}
