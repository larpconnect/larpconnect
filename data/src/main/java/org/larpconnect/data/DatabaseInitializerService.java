package org.larpconnect.data;

import com.google.common.util.concurrent.Service;

/**
 * Service wrapper for database initialization that integrates with Guava's lifecycle management.
 */
public interface DatabaseInitializerService extends DatabaseInitializer, Service {}
