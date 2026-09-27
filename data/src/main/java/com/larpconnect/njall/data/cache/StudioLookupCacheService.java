package com.larpconnect.njall.data.cache;

import com.google.common.util.concurrent.Service;

/** Background service contract managing the lifecycle of the studio lookup cache. */
public interface StudioLookupCacheService extends Service, StudioLookupCache {}
