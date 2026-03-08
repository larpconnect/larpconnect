package com.larpconnect.njall.common.id;

import com.google.common.util.concurrent.Service;
import com.larpconnect.njall.common.annotations.DefaultImplementation;

/** A service layer for generating system-wide unique identifiers. */
@DefaultImplementation(UuidV7GeneratorService.class)
public interface IdGeneratorService extends IdGenerator, Service {}
