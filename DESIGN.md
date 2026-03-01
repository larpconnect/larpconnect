# Design Doc

LarpConnect (codename: Njall) is an application for bringing LARPers together and connecting both LARPers
and games. 

As many of our spaces have been taken over, become more fragmented, or become less and less suitable for
diverse populations, it seemed to be important to have a tool that allowed for coordination and discussion
between different LARP (and more broadly RPG) groups. 

The design here is fundamentally _federated_ in nature, where the goal is that you can run your own or
join someone else's server easily, and the servers can talk to one another.  It is supposed to integrate with
and join-together different platforms and communities, rather than serving as a 1:1 replacement for the tools
and capabilities found elsewhere, and it is not supposed to be a "social networking service" per se and is
designed to support more ephemeral use cases, e.g., a server that exists for the span of a game, as well as 
longer-runnning use cases, e.g., for a LARP studio.

It may not go anywhere. I'm doing a lot of highly experimental things that may not work out. But I wanted a
more serious project to learn some skills that I can apply elsewhere, and this seemed like a good practice bed
to work on them. 

## Technical Introduction

LarpConnect is a multimodule, modern Java (JDK 25+), reactive (via Vert.x and concepts from the Actor Model),
application that is designed to support single tenant, multi-single tenant, and multi tenant architectures.
The goal is not that an individual would run a server, but that an individual LARP studio could run a server
and individuals will join it the way that they would join a discord server and thus talk within community.

Protocol will be custom but loosely inspired by, and roughly compatible with, a variant of ActivityPub.
This implementation, however, will favor correctness, type safety, and precision in communication over adherence
to the ActivityPub standard, and I am not expecting it to ever be compatible with the other major ActivityPub
implementations available today. 

To repeat:

**This implementation is _not_, explicitly _not_, designed to be compatible with modern Mastodon implementations.**

The protocol variation here is an implementation of some ideas I had earlier for something called _FeatherPub_,
and we'll see how those play out in practice. This design is not supposed to be backwards compatible with AP as it
is used today, but more looking forward to what else can be done in this space. 

## General Architecture

The system is designed to be split into **Verticles**. Each verticle is designed to work a bit like an actor
(from the actor pattern), though of course there are some areas where these concepts are different. So API calls
are wrapped by a `Message` object, put onto the bus, and sent on to wherever they will be handled. 

This _event driven architecture_ is designed to decouple a lot of different concerns

### Wire Protocol and API

Protocol is specified in protobuf and held to that: if a message does not fit with the internal protobuf design
we will simply discard it. The API specification is laid out in OpenAPI (though we generate this from other sources,
the source of truth is protobuf+gRPC) for easy ingest and compatibility with other projects or downstream products.

While inspired by ActivityPub, several decisions have been made to make it essentially incompatible with modern AP-based
systems. One of those is the converting of messages to protobuf, which is much stricter about types and typing than would
be allowed for with strict ActivityPub implementations. Still, I believe that this would be relatively straightforward to
build compatibility with for other AP systems should they wish to do so, it is just that it is not our goal to be able
to receive messages from other services that don't conform to our limited structure. 

### Testing

Changes MUST be thoroughly tested. API features SHOULD have integration tests written in cucumber, but everything must
have some combination of unit and integration tests.

The project, to this end, enforces extremely aggressive testing requirements: 80% instruction, 90% branch coverage. This 
means that, as you write code, you need to have tests go in on the same commit. 


