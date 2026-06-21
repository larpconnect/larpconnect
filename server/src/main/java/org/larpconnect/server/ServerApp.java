package org.larpconnect.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

/** Main entry point for the LarpConnect application. */
public final class ServerApp {
  /**
   * Main execution method.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    System.out.println("Starting LarpConnect Server...");
    Injector injector = Guice.createInjector(new ServerModule());
    System.out.println("Injector configured successfully: " + injector);
  }
}
