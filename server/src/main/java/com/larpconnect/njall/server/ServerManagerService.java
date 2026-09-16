package com.larpconnect.njall.server;

import com.google.common.util.concurrent.Service;

/** Public lifecycle service interface managing the runtime HTTP server and Pekko actor system. */
public sealed interface ServerManagerService extends ServerManager, Service
    permits DefaultServerManagerService {}
