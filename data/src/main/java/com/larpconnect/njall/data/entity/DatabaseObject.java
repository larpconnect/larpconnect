package com.larpconnect.njall.data.entity;

public sealed interface DatabaseObject
    permits CollectionItem, ActorEndpoint, StudioRole, Entity, ContactInfo {}
