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

## Features

(TBD)

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

### Architecture Notes

For this the primary design involves four core pieces:

1. The server (written in Java and based on Vert.x)
2. The database (PGSQL, though other databases can be used in test)
3. The queue (RabbitMQ, though other queues can be used in test)
4. The API specification, which is OpenAPI + Protobuf.

We'll use some sort of ORM layer here, but what that looks like is TBD.

### Testing

Changes MUST be thoroughly tested. API features SHOULD have integration tests written in cucumber, but everything must
have some combination of unit and integration tests.

The project, to this end, enforces extremely aggressive testing requirements: 80% instruction, 90% branch coverage. This
means that, as you write code, you need to have tests go in on the same commit.

### Code Quality

Code quality is enforced via several mechanisms.

#### Spotless + Google Java Standards

This ensures that all of the code that is submitted follows a (relatively) consistent set of standards. This is not
sufficient, but it cleans up a lot of little things like whitespace and imports.

#### Checkstyle

Checkstyle enforces a lot of basic things around visibility and some small things that AIs tend to be _very_ bad about
doing that I don't want for the sake of consistency.

#### SpotBugs

Catches a lot of standard bug patterns.

#### ArchUnit

This enforces some of the heavier weight requirements that, again, AIs have trouble following. Often things around
visibility or the "out or down" dependency requirements.

### Context Engineering

This project is in some ways an experiment in context engineering. Specifically, it uses a series of annotations in order to direct
AI agents as to where to look for information, because event driven architectures are honestly hell for agents to navigate
successfully in a lot of ways.

#### Annotations

To do this we use a few different annotations:

* `@AiContract` is the core annotation. It defines the "contract" by which a method does things. Think of it as telling the AI
  "these are the invariants, the guarantees, that we expect here." Basically ersatz design by contract.
* `@InstallInstead` since this is a _multimodule_ system we need the ability to prompt it "yes, I know this is bound in module Foo, but
  you really need to install module Bar. I promise."
* `@BuildWith` says "yes this class has a private constructor, I want you to go over here and install this module in order to
  actually do anything."
* `@DefaultImplementation` says "I only expect there to be one type for this interface, and it is here." We still prefer the explicit
  _bindings_ to be in modules (hence why we don't use `@ImplementedBy`), but this acts as a hint to agents as to where to go.

That said: _contributors_ are not expected to worry about these. We have agents and humans to help keep them up to date, and the most that
we expect you to do is delete the annotation if you change the situation that lead to it in the first place.

#### Skills

We define multiple skills in `.agents/skills/` in the form of markdown files. These are to provide specialized context to agents and let
them "opt in" to the degree of specialization that they need.

