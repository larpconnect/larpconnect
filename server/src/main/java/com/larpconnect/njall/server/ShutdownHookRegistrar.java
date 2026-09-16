package com.larpconnect.njall.server;

/** Interface abstracting JVM shutdown hook registration and removal for testability. */
interface ShutdownHookRegistrar {

  /**
   * Registers a shutdown hook thread with the JVM runtime.
   *
   * @param hook the thread to run during JVM shutdown
   */
  void registerShutdownHook(Thread hook);

  /**
   * De-registers a shutdown hook thread from the JVM runtime.
   *
   * @param hook the thread to remove
   * @return true if the hook was previously registered and successfully removed
   */
  boolean removeShutdownHook(Thread hook);
}
