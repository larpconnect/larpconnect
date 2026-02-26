package com.larpconnect.njall.server;

import java.io.InputStream;

@FunctionalInterface
interface ResourceLoader {
  InputStream getResource(String path);
}
