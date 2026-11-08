nb. This document is IN DEVELOPMENT and can change unpredictably.

The core protocol of the system is known as "FeatherPub." Or rather, a prototype/experiment of what FeatherPub may hopefully become. 

FeatherPub is inspired by ActivityPub, but makes a few adaptations that break compatibility and others that ensure internal compatibility with other FeatherPub clients (at least after we formalize it).

The core tenants are:

1. Types are consistent over the wire. If in the spec a field is an array then it is *always* going to be represented as an array. What this means in practice is:
2. We will explicitly opinionated about how the system gets used. 
3. Activities are strictly ephemeral. As a result multiple types of activities have been rebranded as objects, so don't be surprised when you see things like `Add { Like { . } }`
4. There is One Correct Format™ for any given object for anything sent as part of federation. There is One Correct Format™ for any given object retrieved from a given URI endpoint.  These can be, but are not necessarily, different.  We may support other formats, but there is one correct one that will always be supported.
5. Everything is JSON specified in either OpenAPI or json-schema.

A more complete FeatherPub design document will be published after 1.0, and a more thorough 1.0 design doc will be published once we get closer to beta.

## A Note on Language

We use the standard MAY, MUST, SHOULD language here from RFC 2119.

- MUST: An absolute requirement. If you aren't doing this then your system is not conformant. 
- SHOULD/SUGGESTED/RECOMMENDED: A _strong_ recommendation. Servers could very well have a valid reason to diverge here, but you could very well break interoperability if you do so.
- MAY: Truly optional.

In addition sometimes things will be listed as a "default" behavior. These are generally the equivalent of a SHOULD unless otherwise indicated, but with the caveat that it is _expected_ that servers may some day need to diverge and not feel constrained by the spec, or while it will continue to be accepted is considered possible/likely that what is default will change over time. 

An example of this would be the recommendation of SHA3-256: servers are encouraged to adopt this as a default, but if in the future a weakness were found in SHA3-256 that didn't exist in BLAKE3, servers should feel empowered to switch to BLAKE3 and disallow SHA3-256 without losing conformance or waiting for FeatherPub to update. This would break interoperability, but security takes precedence in a lot of cases and the organic "flocking" to a variation on the spec is preferable to being held back.
## FeatherPub Features

We will aim to ensure that FeatherPub includes:

1. A canonical way of representing Server and Studio actors, as well as actors for groups, utilities, etc along with defined behavior for things like relays. 
2. A way of specifying audiences as public, studio-local, or personal. Possibly with some other set of restrictions (visible to the followers of this actor, for example) but those need to be examined for feasibility.
3. A transmission pattern of not attaching arbitrary objects and instead passing URIs where feasible, going only to a depth of 1–2 on objects that are transmitted over the wire.
4. A federated wipeout **request** pattern, with expected TTLs as part of the contract for objects sent to other servers. 
5. Clear moderator primitives for moderating instances and providing information to a federated network (via FIRES)
6. A general pattern of API endpoints, such that you can easily determine from a tenant or user actor:
	1. what endpoints will be available
	2. how those endpoints will behave (i.e., for filtering or searching in addition to standard POST/PATCH/etc) and what features they support
7. A typesafe specification that does not require union types after parsing.
8. A dedicated method of doing plugins/extensions. 
9. Consistent methods of doing pagination.
10. Explicit support for batch message sending and retrieving.
11. The studio actor will act as a relay and forward messages sent to it to the appropriate inboxes, acting as a `sharedInbox` for the system. 
12. APIs will be inspired by the AIPs. 

## Capabilities

Capabilities are indicated by the **Type** and any attached **Mixins** on the object. These subtypes are referred to as **Specializations**. This is indicated by a `type` field.

The type is called the _Primary Type_ while the remainder of types in the `mixins` field work as "mix-in"-style or "marker" types. For example, if I say that an object is:

```json
{
	"type": "Document",
	"mixins": ["Listenable"]
}
```

This indicates that the object **is a** document and has a `listeners` collection on it. The _fields_ of an object are always represented by the `type` field. 

From an implementation standpoint in OpenAPI and json-schema, the type works as a discriminator while allowed mixins are considered optional fields, then parsing implementation can be handled at runtime in the application logic.
## Plugins

Plugins are represented via the `@context` field, which is always an array of URIs. For example:

```
{
	"@context": ["https://larpconnect.app/schemas/featherpub"]
}
```

Would indicate the basic FeatherPub feature set, which include the FeatherPub understanding of types and behaviors. Additional types and behaviors may be specified via additional URIs. 

It is generally assumed that `One Server = One Schema`. Which means that every object from a given server will use a _consistent_ collection of plugins. This collection _may_ change over time, but it should change consistently. 
## API

### A Note On Paths

ActivityPub is known for a design that emphasizes what they call "follow-your-nose": the layout of objects, paths, etc is not something that needs to be consistent between implementations.  FeatherPub takes the opposite approach: Paths are defined in advance, giving us the ability to move servers while maintaining the same domain. 

Paths prefix with `/v1/` as a deliberate versioning strategy for constant paths. This way if, in the future, breaking changes are introduced in the path layout between servers it can be isolated from older implementations. 
### Path-Identifier Primacy

FeatherPub operates off of a concept referred to in the documentation as "Path-Identifier Primacy." What this means is that objects can be be universally broken down into two components: 

1. A _basic path_.
2. An _identifier_.

The basic path is a standard path that ends in the identifier, for example:

```
/v1/actors/{actor-id}
```

In standard implementations the identifier will be either a UUIDv4 or a UUIDv7, allowing also for aliases that are alphanumeric in structure. 

Given the identifier and the basic path you should always be able to assemble the path to the object. 

This seems like a simple and obvious statement, but it means that the following example path structures are forbidden as identifiers:

```
/v1/actors/{actor-id-1}/{actor-id-2}
/v1/actors/{parent-id}/actors/{child-id}
/v1/actors/{id}/note
/v1/actors?id={id}
```

It does _not_ forbid layering resources with different identifiers, however:

```
/v1/actors/{actor-id}/notes/{note-id}
```

Here the two identifies are for different objects that can be returned separately, and so this is an allowed structure.

Nor does it forbid unique resources with singleton names:

```
<actor-path>/{actor-id}/inbox
```

### Basic Path Layout

Conformant servers will always have these paths: 

- `/.well-known/`
	- `webfinger`
	- `jwks.json`
	- `openid-configuration`
	- `oauth-authorization-server`
	- `security.txt`
	- `nodeinfo`
- `<base-path>/v1/actors/{actor-id}/`
	- `inbox[:(message|batchMessage)]`
	- `outbox[:(message|batchMessage)]`
	- `collections/{collection-id}`
	- `timelines[/{timeline-id}[:(batchGet)]]`

They MAY have other paths.


### Communication Format

Communication happens over plain JSON using the mimetype `application/json` (and as a consequence be UTF-8). Other formats MAY be accepted by individual servers, but MUST only communicate in alternate protocols if there is a handshake with the server other server confirming acceptance, and MUST continue to accept `application/json`.

Within this spec we use `@` to indicate a "meta" field: A field that indicates how the object is to be processed, that doesn't tell us anything about the object itself. These inform or hint as to how to process the object without telling you anything about the type of the object.

There is one such field that is REQUIRED and a few more optional ones. 

Required:

- `@context: <list of uris>` — indicates which plugins are in force.

Optional:

- `@language: <language code>` — the language code to interpret text fields in the object with by default. MUST follow IETF BCP 47.

### HTTP Verbs

Standard support: 

- GET
- POST
- OPTIONS

Supported in client-to-server operations:

- PATCH
- DELETE

Not generally supported (may be implemented by servers or clients but not as part of FeatherPub):

- PUT
- HEAD
- TRACE

### Inbox and Outbox

The inbox and outbox for a given actor are always accessed via:

```
<actor-path>/{actor-id}/inbox
<actor-path>/{actor-id}/outbox
```

### Persistent (Owned) Object Paths

Objects that are owned by an actor SHOULD be found at:

```
<actor-path>/{actor-id}/{object-type}/{object-id}
```

So for example a `Note` object can always be found at:

```
<actor-path>/{actor-id}/notes/{note-id}
```

Note that the object types here are defined by the _application_ rather than the _protocol_, and so MAY be different between different implementers. Still, they SHOULD follow the basic semantics above.

### Batch Endpoints

One of the core features of featherpub is the concept of **batch endpoints**. What this means is that if you have an inbox:

```
/v1/actors/{actor-id}/inbox[:message]
```

You can also expect it to have:

```
/v1/actors/{actor-id}/inbox:batchMessage
```

In combination with the _studio actor_ maintaining the shared inbox, this allows one server to write:

```
/v1/actors/default/inbox:batchMessage
```

To have a set of messages correctly routed, turning a $\theta(n)$  request pattern into a $\mathrm{O}(1)$ request.

## Object Model
### Activities

Activities are, in FeatherPub, considered _ephemeral_ from the perspective of the sender. They do not have a concrete identity or a publicly accessible URI. Compared to ActivityPub, there are also many _fewer_: The core relationships are all removed from the activities and moved into the objects.

The following Activities are supported:

- Create — Contains the object being created. Informs the receiver that the object has been created.
- Update — Contains the object being updated. Informs the receiver that the object has been updated.
- Delete — Contains a Reference to the object in question. Informs the receiver that the object has been deleted. 
- Offer — Provides an activity and optionally asks for verification of it. Which can either be Accepted or Rejected.  
	- Accept — A response to an Offer that says that an Offer's action was accepted.
	- Reject — Aa response to an Offer that says that an Offer's action was not accepted.
- Add — Add an object to a collection. You can either _tell_ someone that you have done something or _ask_ (via Offer) that they do something. 
- Remove — Remove an object from a collection. You can either _tell_ someone that you have done something or _ask_ that they do so. 
- Move — Move an object between collections. You can either _tell_ someone that you have done something or _ask_ that they do so.
- Announce — Brings attention to some already extant object. 

While they are considered ephemeral, this means that there is no expectation of consistency in the hash of a stored Activity and that a represented Activity can be considered a bookkeeping ledger, not an object with a permanent identifier. 

#### Activity IDs

Activities MUST have an `id` field with a URI from the same domain (or a subdomain) as the associated actor. 

This id **must** be unique to that specific activity and direct to a valid URI. That URI _need not resolve_, it only needs to be unique with at least 64 bits of cryptographically secure entropy. It MUST NOT resolve in such a way that provides an unrelated resource and MUST NOT return the value of the activity, but it **MAY** be any of the following (or other schemes), assuming they meet the other requirements:

1. The object identifier of the underlying object + a sequence id. 
2. A special "activities" endpoint that returns a successful, blank response
3. A special "activities" endpoint that returns a successful response with the `Content-Digest` of the actor.

While virtually all server-to-server actions are functionally idempotent individually, they can be accidentally replayed in pathological ways. Servers MUST ensure that they do not process duplicate Activity IDs within a reasonable timeframe. This restriction does not apply to activities or sequences of activities that create essentially a `nop`: if a second `Create` message arrives for the same object and no update is made to the underlying object, then the second activity id can be discarded.

What is considered a "reasonable" window is left up to implementation. 24 hours + the retry period (7 days, so 8 days total) would be a reasonable setting, but both longer (e.g., the lifetime of the object) and shorter (e.g., the JWT token lifetime + 5 minutes) could be justifiable.

It's worth emphasizing that this is not meant as something to deal with malicious attackers: by the time you are processing this message it should have already passed the JWT checks, JWS checks if applicable, rate limits, and possibly other object validation checks as well: This is a message that the server **should** process and that they have the right to send us, but we want to make sure that the result on the other side is coherent.

### Objects

- Question — A poll or similar. 
- Note — A message of up to 1024 characters. May include attached media. Also used for Boosts.
- Page — A message of up 4096 characters. May include attached media.
- Article — A longer message. No arbitrary limits. May include attached or inline media. 
- Relationship — An arbitrary relation between two entities.
- Event — Something that is taking place.
- Place — A location that something is taking place in.
- Profile — Information about an entity
- Block — A communication restriction. 
- Reaction — A reaction. Such as a like, a dislike, or an emoji reaction. 
- Listen — Subscribes to events from this object, managed by an external actor. In general is saying "I care about the _state changes_ to this object" rather than "I care about _things generated by_ this object."
- Follow — A specialized version of a listener that attaches to an _actor_. Says "I care about things generated by this object."
- Flag — Indicates that an object should be brought to the attention of someone. 
#### Collection Types

- OrderedCollection
- OrderedCollectionPage
#### Versioning

Objects SHOULD have both a `created` and an `updated` field with ISO8601 dates in them. When a remote server sees a version of the object with a newer updated date, it SHOULD update its internal representation of that object. 
#### Object Specializations

- Reaction
	- EmojiReaction
	- Like
	- Dislike
- Listenable
- Replyable
- Reactable
- OrderedCollection
	- Paginating
##### Replyable

Includes a `replyActor`:

```json

{
	// …
	"type": "…"
	"mixins": ["Replyable"],
	"replyActor": "actorUri",
	"context": ["contextUri"]
}
```

When a `replyActor` then receives anything via either `to` or `cc` that lists `inReplyTo` as this object OR that includes a context URI that is listed here it SHOULD add it to a `replies` collection with the following structure:

```json
{
	type: "Reply",
	actor: "originatingActorUri",
	target: "replyUri"
}
```
##### Listenable

Accepts a `Listener` object in a collection, usually `listeners`:

```json
{
	"type": "Listen",
	"actor": "listenerUri",
	"target": "targetUri"
}
```


##### Reactable

Accepts reactions. The basic contract for a reactable is:

```json
{
	"type": "…"
	"mixins": ["Reactable"],
	"reactions": "reactionCollectionUri",
	//The following fields are optional, but at least one SHOULD be included
	"likes": "<number>",
	"dislikes": "<number>",
	"emojiReactions": {
		"<symbol>": "<number>"
	}
}
```

A reaction is structured as follows:

```json
{
	"id": "<uri>",
	"type": "Reaction",
	"mixins": ["(Like|Dislike|EmojiReaction)"],
	"target": "objectUri",
	"actor": "actorUri"
}

```

#### Generators

Objects (and by extension Actors) SHOULD have a `generator` field on it that indicates which object is the "controller" of that object. So if you want to, for instance, flag an object to the owner of that object you can send it to the `generator`. Top level actors will be marked by referring to themselves as generators.

Note that this concept is in many ways orthogonal to the concept of `attributedTo`. The `generator` is the _mechanistic_ component that generated the object in question—an artifact of the protocol—while `attributedTo` is the **semantic** creator. These may be the same but they by no means need to be.

### Addressing and Audience


Objects may have the following fields on them: 

- `to`: The object is _addressed to_ these actors.
- `cc`: The object is _referred to_ these actors. 
- `audience`: The object is _visible_ to these URIs.

#### To

These are the actors who are directly the intended audience of the message. For example, people involved in a conversation, followers, etc. Examples are:

1. Those being directly addressed
2. Followers who have expressed an interest and who are not otherwise being excluded

#### Cc

These are the actors who are not being directly addressed, but who are interested parties for the object. Examples are:

1. `replyActor` objects
2. Event or group management actors
3. Relays
#### Audience

The `audience` field is used in FeatherPub to dictate who MAY see the object outside of the `to` and `cc` lists. Servers MUST NOT send, display, or transmit the full body of the object (or more than the URI and type) to anyone who they do not **know** fit the criteria of the `to`, `cc`, or `audience` fields.

There are three basic types allowed in the audience field:

- Domains, limiting to just the domain itself (e.g., `example.social`).
- Hash values. Specifically `#Public`, though others may be supported in the future. An object with a `#Public` audience is available without authorization on the internet, though it does not require that the receiving server treat it as such.
- Collection URIs. This is mostly for local servers to provide a way of saying "these users on my local system are allowed to see this object." The collections SHOULD be a following or followers collection on the actor meant to receive the object and servers SHOULD NOT have any expectation that remote servers will actually enforce this. Meanwhile, if a server cannot (or does not wish) to enforce this, they MAY ignore it.

This behavior cannot be guaranteed in non-conformant servers and so should not be depended upon, but it is expected of conformant servers to not expand visibility _beyond_ what is specified.

It is also notable that this is a _limitation_ and not a _requirement_: just because something is labeled as `#Public` does not mean the server is _required_ to host it, only that it _may_ do so. By the same token, if a server finds it onerous to allow a collection of users to view an object they MAY simply disallow this and not expand the visibility beyond what is already specified in `to` and `cc` (who receive copies).
### Actor Types

- Person — An individual. Will almost always include a Profile. 
- Service — A specific subunit of functionality. For example, a scheduling utility for managing events would be a Service. Services will typically have a specialized API or specialized set of behaviors.
- Organization — A _studio_ would have an Organization actor. 
- Group — A collection of actors centered around a topic or purpose. 
- Application — There will usually be one of these per Server. There may be additional ones representing other running software components.

#### Actor Specializations

- Relay
- Topic
- Instance
- Profile
##### Actor Fields

Actors will always have the following fields, in addition to type and id:

- `inbox`: A specialized uri with specific semantics.
- `outbox`: A specialized uri with specific semantics.
- `preferredUsername`: the username that can be referenced from webfinger.
- `endpoints`
	- `sharedInbox`: A specialized URI referring to the inbox of an actor who can route to this one. 
- `streams`
- `following`
- `followers`
- `summary`

In addition, `streams` MAY have the following:

- `reacted`
- `listening`
###### Actor Hierarchy

All actors exist within what is called an _actor hierarchy_ on a given server. This is represented via the `generator` field. Thus there is always going to be a server-level actor that all other actors descend from. There SHOULD be at most one "top level" actor per server, identified because it is its own generator. 
#### Links

The following links are supported:

- Link
	- DocumentLink: A link to an external arbitrary document. 
	- ImageLink: A link to an image file
	- VideoLink: A link to a video file
	- AudioLink: A link to an audio file
	- ObjectLink: Links to a FeatherPub object of any description.
- Hashtag

##### Link

A specialized wrapper for a URI.  There is no requirement that links point to a resource that is owned by the server.
##### Hashtags

Hashtags contain two elements:

1. The tag itself.
2. A constant identifier

Servers SHOULD ensure constancy in their identifiers: 

SHOULD have a constant identifier. This identifier MAY be shared between servers or it MAY be individual to a given server. 
## Behaviors

### Threads

There are three fields that may be added to an object to indicate its behavior relative to other objects.

1. A `context`
2. A `inReplyTo`
3. A `target`

The `context` is the thread, the `inReplyTo` is the post being replied to. A `target` means that the object acts on another object in some object-specific way.
### Quote-Boosting

There is no direct concept of "quote-boosting" built into the protocol _per se_. Instead this works by attaching a post to the `tags` on another post, usually a `Note`.

### Offering

Any activity may be _offered_. An `Offer` is a statement of "I would like to know if the execution of this activity succeeds" and will contain an actor and an id to respond with either an `Accept (id)` or `Reject (id)`. 

An `Offer` should be used when the sender does not have permission to modify the resources that it is asking to be modified or if a response is needed in order to modify their own resource in some way. 

For example, you could `Offer` an `Add` to send a RSVP, or `Offer` to `Add` an actor to a collection to implement a follow. 
### Reactions

Broadly speaking there are three types of reactions: Likes, Dislikes, and Emoji reactions. 

A reaction gets added to the `reactions` collection on an object.
### Listening

`A -Listen-> B` says that `A` wants status updates **about** `B`.  Examples might be updates on a document or posts to a thread. These normally represent a scenario where the object in question does not have agency: A simple collection, an event, etc. 

So let's say that you have a document:

```hocon
Document {
	owner: AliceActor
	listeners: OrderedCollection {}
}
```

`BobActor` sends `AliceActor` the following (omitting irrelevant fields):

```
1: > Add {
      Listen {
	    BobActor.id,
        Document.id
      },
      Document.listeners.id
	 }
   }
```

When `AliceActor` processes this, it adds the `Listen` object to the `listeners` collection. Note that an object may have multiple listener collections, but each should have its own mix in associated with it. 
### Following

Similar structurally to `Listen` except that the target is the actor's followers:

```
Add {
	Follow {
		AliceActor.id,
		BobActor.id
	},
	BobActor.followers.id
}
```

A unique providence of _Actors_, following says "I care about updates that this actor sends out."

Listening is semantically similarly to following but with an important distinction: who the actor is. For example, if you listened to **and** followed a Actor:

1. On the `Follow` channel you would receive status messages that the actor sent. 
2. On the `Listen` channel you would be notified if the actor updated their profile or was deleted. 

While an actor may have multiple listener collections they only have one follower collection.  If there is a desire to have multiple follower collections the recommendation would be to use multiple actors (see subactors). 

### Deletion

When an object is deleted a server SHOULD send out a `Delete` object and replace that object with a `Tombstone`.   The tombstone need not reveal anything specific about the object that was deleted, merely that there was an object here, and now there isn't.
### Pagination

Exactly one type of collection is supported: the `OrderedCollection` with the `OrderedCollectionPage`. Things with the type `OrderedCollection` have a static identifier while things with the type `OrderedCollectionPage` do not.

`OrderedCollection` has the following structure:

```yaml
id: <uri>
type:
  - OrderedCollection
items:
  - item1
  - item2
```

It may optionally be marked with the Paginating` type, in which case:

```yaml
id: <uri>
type:
  - OrderedCollection
mixins:
  - Paginating
items: 
  - item1
  - item2
next: <uri> # Null if there is no next page
first: <uri> # Optional
prev: <uri> # Optional
totalItems: <number> # Optional
```

Items of type `OrderedCollectionPage` are identical to `OrderedCollection` + `Paginating` but do not have static identifiers: the `id` is transient and represents the URI of the current snapshot, while items of type `OrderedCollection` are expected to have a static identifier.

The goal here is so that you can use *pagination tokens*. Pagination tokens **must** be **opaque** to the client.

FeatherPub makes no guarantees about how pagination behaves in the presence of modifications to the underlying collection other than that it **must not** fail under concurrent modification and, in the absence of changes, show all non-deleted items in the collection. Beyond that it may reflect the changes or not, show objects that are deleted during pagination or not, etc.

If an item **is** deleted or modified then the behavior of the system as to whether show those changes is not defined and may be implementation specific. Clients **must not** depend on any specific behavior here. 

The `OrderedCollection` URI SHOULD always support the following options:

- `page_token` — the pagination token for a given page
- `page_size` — a numeric value with a sensible default and maximum

The system SHOULD use the `Link` header with `next` and (optionally) `prev` fields for pagination purposes in addition to the actual structure of the object. Servers also SHOULD use etag headers for cache and version control.
## Security

### General


- All communication MUST take place over TLS.
- Hash algorithms MUST NOT be shorter than 256 bits or weaker than SHA2/SHA3, with SHA3 being the default.  Other examples of acceptable hash algorithms would be:
	- BLAKE3
	- BLAKE2
- Algorithms cannot be weaker than 128-bit AES or the Ed25519 key variant for EdDSA (the default).

### Security Limits

- If a server allows online registration, it MUST be either disabled or admin-approval-required by default.
- A server is responsible for including per-actor outgoing rate limits. These MAY be generous or restrictive and they MAY be different from each other.
- A server is responsible for including per-external-system incoming rate limits. These may be against servers as a whole or individual actors, and they can vary by any number of things, but they must be implemented.

### Visibility

Conformant servers are responsible for ensuring that items that _should not_ be visible without authorization _are not_ visible with authorization (or, where applicable, authentication). This can apply to objects, streams, or simply *fields* on an object.

For example, if a server platform decides that it does not want to display `likes` on an object it may do so, but the protocol does not specify how or where it needs to do this and it is responsible for those decisions on its own. If it wishes for the entire collection of reactions to be visible (or restricted) it may do so, but it is up to it to decide and enforce this consistently. 

The protocol **does not specify normatively how or where to do these things**, it is up to individual platforms to take a stance on how they wish to enforce it. This means that things like the following are allowed if the server platform wishes to follow them, but are not required:

1. A replyable object where not everyone can reply and visibility on the `replies` collection and `replyActor` are limited.
2. A reactable object where only logged in local users are allowed to react and others do not see the `reactions` id (and thus cannot add reactions to it). 

etc. 

### Server-to-Server

FeatherPub utilizes JWT with a detached JWS (in certain circumstances only) for server-to-server communication. The specification is as follows:

- `typ`: `at+jwt`
- `alg`: `EdDSA` is the default, others may or may not be supported
- `kid`: Resolved via `JWKS`

A normative definition of a body is:

```yaml
iss: <uri of the originating server>
sub: The URI of the actor originating the message
aud: server(s) that the message is intended for
iat: timestamp that the JWT was minted
exp: timestamp, usually 60 seconds forward
jti: a single-use nonce
```

Regardless of the size of specific fields, the entire JWT token SHOULD fit within 4 KiB.

Allowed clock drift is generally one minute but servers MAY opt for longer or shorter.

Receiving servers are responsible for validating:

1. That the issuing server is allowed to contact the receiving server.
	1. That the IP address does not resolve at the socket to any of the following or their IPv6 equivalents (where applicable) unless specifically allowlisted to do that:
		1. 10.0.0.0/8⁠
		2. 172.16.0.0/12⁠
		3. 192.168.0.0/16
		4. 127.0.0.0/8 
		5. 169.254.0.0/16
		6. 0.0.0.0/8
		7. 100.64.0.0/10
		8. fc00::/7
		9. fe80::/10
		10. 2001:db8::/32
2. That the sub UUID is a valid sending actor.
3. That it is included in the `aud` field. 
4. That the message was received and validated between the `iat` and `exp` ± clock drift.
5. That the `jti` has not been reused within an acceptable time window. 
6. That the `typ` is what is expected.
7. That the `alg` is supported and of sufficient quality.
8. That the `Content-Digest` is correct against the payload.
9. That the JWKS key can be loaded from `.well-known/jwks.json` and is still valid (with some degree of attenuation).
10. That the algorithm validates against the key resolved from JWKS.
#### Audience

The JWT payload MUST include an `aud` field which MAY be a single string or array of strings. Servers MUST validate that the target `aud` includes the server being targeted. 

#### Replay Protection

There are two basic forms of replay protection in effect. One is a _security_ replay protection and the other is a _application_ replay protection. For the application version see the Activity IDs section.

The auth JWT MUST include a `jti` field which contains at least 96 bits of entropy (and 120 bits is preferred). A UUIDv4 that has been securely generated is acceptable. Servers are responsible for ensuring that a `jti` is not reused within the acceptable timeout period + 1 minute skew buffer (+ any additional skew buffer that the server chooses to use). Servers MAY protect against `jti` reuse over a longer period: there is no requirement once outside of the skew buffer time. 

Note that `jti` is a plain text field, so it should not be _parsed_ as a UUID even if it may be _implemented_ as a UUID. A perfectly copacetic `jti` field would be: `UUIDv7 || "-" || 64-bit-csprng-number`  || "-" || sequence_id`, essentially including non-random components. So long as the required entropy levels are reached there's no further requirement.

#### Body Protection

We depend on TLS for the protection of the body in transit.

All messages SHOULD use the `Content-Digest` hash of the body. The DEFAULT hash algorithm for this purpose is SHA3-256.
##### Detached JWS

A detached JWS is used for specifically three high assurance scenarios where the value needs to be durable and forwardable.

The scenarios we expect to see detached JWS in:

1. Migrations
2. Wipeout requests
3. Certain classes of moderator action under tools like FIRES

These are situations where there is a need to specifically have the action be durable and independently verifiable by downstream operators outside of the two servers responsible for the initial communication.

The specification for this will be defined later when those scenarios become more relevant. 
#### Federated Response Codes

Servers may respond with any of the following success response codes on a POST request:

1. `204 No Content` Server succeeded and has nothing more to say
2. `202 Accepted` The server has acknowledged receipt of the message and will process it later.
3. `200 OK` If a more complex response is required, e.g., for a server migration, AND the server has processed it now.
#### Rate Limits

Servers SHOULD use the `draft-ietf-httpapi-ratelimit-headers-11` to communicate rate limits with other servers. We hope to update this to the full RFC in the future. Precise rate limits are defined by individual servers and are not a direct part of the spec, which only mandates their use. 

Servers are responsible for ensuring adequate protection against DDoS and similar since the rate limit headers are suggestions to the client and not an actual security restriction. 
#### Retry

For retryable errors (most `5xx` responses, `429 Too Many Requests`, etc) servers SHOULD try again after a reasonable timeout and use an exponential backoff strategy with jitter. They SHOULD retry at least 7 times (so 8 times total with the initial request) before giving up, with:

- At least two of those attempts being at least 1 full day from the initial attempt.
- At least one of those retry attempts being at least 7 days from the initial attempt.
- The first retry attempt having no less than 1 minute from the initial attempt. 

Retry times SHOULD be based on when the response _returns_ rather than when it is first _attempted_. 

The following is a potential (non-normative) pattern:

- T0: 0 seconds
- T1: T0 + 1m15s ± jitter(15s)
- T2: T1 + 2m30s ± jitter(30s)
- T3: T2 + 5m00s ± jitter(60s)
- T4: T3 + 10m00s ± jitter(2m)
- T5: T4 + 20m00s ± jitter(4m)
- T6: T5 + 40m00s ± jitter(8m)
- T7: T6 + 1h20m ± jitter(16m)
- T8: T7 + 2h40m ± jitter(32m)
- T9: T8 + 6h20m ± jitter(1h4m)
- T10: T9 + 12h40m ± jitter(2h8m)
- T11: T10 + 25h20m ± jitter(4h16m)
- T12: T11 + 50h40m ± jitter(8h32m)
- T13: T12 + 101h20m ± jitter(17h4m)

A more minimalist example that is also valid could be:

- T0: 0 seconds
- T1: T0 + 1m15s ± jitter(15s)
- T2: T1 + 30m ± jitter(30s)
- T3: T2 + 60m ± jitter(60s)
- T4: T3 + 4h ± jitter(8m)
- T5: T4 + 12h ± jitter(24m)
- T6: T5 + 1d8m ± jitter(4h)
- T7: T6 + 7d16m ± jitter(8h)

The `jitter` function is by default presumptively uniform, but implementations MAY opt for other strategies that preserve the basic intention (e.g., inverse-gaussian).

Servers SHOULD NOT send retries more quickly than their exponential backoff will allow for under most circumstances, but they MAY do so if specifically asked to do so by a cache or rate limiting header.

Servers have no obligation to continue trying after the last retry after 7 days but MAY continue to do so.

Servers MAY also change the _grouping_ of items in retries, so long as the items individually are retried according to the above specifications. Thus if a server is sending a `batchMessage` of 20 items and another `batchMessage` of 10 items it MAY end up grouping their retries together. 
#### Ghost Servers

A server that has not been heard from or contacted within 28 days is considered a "ghost" server. These are presumed gone and servers MAY discontinue sending messages to them. They are under no obligation to track the last message sent to a ghost server, so if it does come back online after 28d they are not under any obligation to return even the last 7 days of data.

Basically: if a server goes offline for a significant amount of time it should just be considered gone until proven otherwise. 

As servers approach that 28 day mark they MAY choose to reduce the frequency of contacting the server in question, e.g., batching requests to only a few times a day rather than sending them in the moment. 

#### Federated Behaviors

Upon receipt of an `Activity` servers follow steps like this:

1. Validate the JWT, `Content-Digest` header, etc
2. Validate that the `Activity` `id` has not been processed previously
3. Confirm routing to the correct inboxes, if applicable, and that known `Block` structures are being respected.
4. That the server actually has permission to be giving us the object in question (e.g., example.com cannot provide us an object ).
5. That all relevant actors and server information have been loaded.
6. Ensure that the object is conformant and coherent and that it is actually addressed to the inboxes it is being routed to. Examples of things to check include:
	1. That the context contains `featherpub`. Unrecognized contexts SHOULD NOT prevent continuing to process the object **if** the `featherpub` context is included.
		1. Implementations MAY decide to process other objects, but this entirely at their discretion and not covered by this protocol. 
	2. That there exists an `updated` field that is equal to or greater than the `created` field. 
	3. That there exists a URI of identifiable format for the id(s).
	4. That the type is recognized (unrecognized primary types SHOULD be discarded, unrecognized mixin types SHOULD be processed regardless and ignored).
7. Verify that the activity is from within the server's retry window. Implementations MAY accept older objects, but SHOULD—if they do—consider how to surface this information. 
8. Verify that the activity is not from the future.
9. Verify that the object created/updated is not from the future. 
10. Follow the steps required by the Activity.

The steps for the individual destination inbox depend on the specific activity received.

##### Create

The server SHOULD add a representation of the object being created to the appropriate timelines. It should also:

1. Ensure that the `inReplyTo` and `context` fields map to loaded objects and, if not, attempt to load them. It should succeed in writing this object regardless of if it is successful in loading either the `inReplyTo` or `context` objects, but should at least attempt to load those contextual pieces if they aren't already stored.

Functionally idempotent.
 
Verifications:
- That the object does not already exist. If it DOES already exist in the timeline then the server MAY do one of two things. It MUST NOT generate a failure response.
	1. Nothing, leaving the existing setup in place and discarded the new result.
	2. Use whichever version has the more recent `updated` field. This is the expected default behavior, basically treating it as an `Update` instead of a `Create`
- That all referenced objects are either loaded or marked to be loaded if possible.

##### Update

Ensure that the version stored and presented is consistently the most recent version of the object. They SHOULD NOT update the location of the object on timelines that don't specifically document this variant behavior.

Verifications:
- Servers MUST verify _which_ item (the item in the update or the original) is newer based on the `updated` field and present that one. They MUST NOT replace a newer item with an older item.

Functionally idempotent.

##### Delete

Informs servers that an object has been deleted. Servers MUST remove the object from timelines when the `Delete` is processed (within a reasonable amount of time) and delete the object fully within 28 days. Servers MAY leave a `Tombstone` behind. 

Functionally idempotent.

Verifications:
- If the object does not exist then the server MUST NOT create the object as a `Tombstone`
- The `Delete` request MUST have a `created` date that is _after_ the last `updated` date on record for the object.
### Server-to-Client

#### Security

Authorization SHOULD and by default be handled via OAuth2.  Other implementations MAY be added in addition.

#### Inbox Semantics

Calls to `GET` the `*/inbox` have no defined behavior and MAY fail (e.g., with a 501, 403, or 404 error code: the exact error-code is non-normative). 

Writing to the inbox should not be done by clients and MAY be rejected. 

Implementations MAY also choose to implement `GET */inbox`, but this is implementation-specific behavior and not specified here. 

#### Outbox Semantics
#### Timelines API

An actor may have one or more **timelines**. These are accessed at `/v1/actors/{actor-id}/timelines/{timeline-id}`.

Timelines return `OrderedCollection` pages with pagination and are populated with objects. The content of those pages is, if  not defined here, considered implementation specific. It is SUGGESTED for major timelines to have unique aliases, with the following aliases being reserved:

- `default`
- `all`
- `notes`
- `events`
- `games`
- `locations`
- `collections`
- `pages`
- `documents`
- `articles`
- `boosts`
- `self`
- `friends`
- `following`
- `followers`
- `images`
- `videos`
- `updates`
- `announcements`
- `songs`
- `music`
- `documentation`

Unsupported items from the above list SHOULD respond with a `501 NOT IMPLEMENTED` error.

The following SHOULD be implemented:

- `default`
- `all`

The following are SUGGESTED:

- events
- locations
- announcements

All endpoints SHOULD support filtering using [this EBNF grammar](https://google.aip.dev/assets/misc/ebnf-filtering.txt) and/or CEL under the `filter` query option. 

#### Client-Server Behaviors

##### Create

In order to create the client will `POST` a `Create` object to the `outbox`. Upon receiving this the server will then:

1. Add the user's _actor_ to the `attributedTo` field. 
2. Populate the `generator` field. If there is one actor per user in this setup then this will be the same as the `attributedTo`, but in multi-actor scenarios these may be distinct.
3. Confirm conformance with basic specifications for the system for a `Create` object along with the underlying object.
4. Actually create the object in question. 
5. Send out any notifications as required.

When the API returns it SHOULD provide a `Location` header response indicating where the new resources is that has been created. 

##### Update (Patch)

Updating objects is handled via the `PATCH` verb. Systems update via the PATCH verb and then the server MAY communicate out this change using `Update`.

Servers are RECOMMENDED to use etags for patch commands. 

## Major ActivityPub Divergences

There are a few areas where there are clarifications or narrowing of the specification for AcitivityPub: areas where the specification is possibly more specific or incompatible at the edges, but the core of it is recognizable from ActivityPub (e.g, the type system, the collections system for the most part), or where ActivityPub makes  no statement and so while it is broadly incompatible with modern ActivityPub implementations it can't be said to be incompatible (e.g., JWT).

This section is about the areas where the departures are so radical as to be considered a different protocol, and any thoughts about how this could be reconciled.

### Transient Verbs and Removal of Undo

Where ActivityPub treats `Activity` objects as, well, objects, FeatherPub treats `Activity` objects as ephemeral, with no persistent identity of their own. This has several consequences, including:

1. Clarifying what you can and cannot delete and the side effects from that process (if you `Delete` a `Create` object does it delete the underlying object?). 
2. Removing the concept of "undo" entirely.
### Verbs to Objects: Follow, Listen, Like, etc.

In ActivityPub it is common to express _relationships_ as _activities_. For example:

- Follow
- Like

ActivityStreams has more:

- Listen
- Dislike

etc

In this case we do a few things very differently. 

1. We turn these into objects and we directly store those objects.
2. We change the verb to simple `Add` and assume that the other actor knows what the object's structure is like here
3. We dictate the use of `Offer` when  telling a remote system to do something and are depending on the result. 


So where in ActivityPub you do the following:

```
1: A -Follow-> B
2: B -Accept-> A
3: B.followers.add(A)
3: A.following.add(B)
```

We do the following:

```
1: A -Offer {Add {Follow {id}}}-> B
2: B -Accept-> A
3: B.followers.add(Follow {id})
3: A.following.add(Follow {id})
```

### Weakening "Follow Your Nose"

ActivityPub depends on being able to find everything directly through objects. This is referred to as "follow your nose" and the idea is that rather than knowing that a server will have a resource at path `$foo` the protocol assumes you will have some field or other indicator that tells you where to look for that path. 

FeatherPub _mostly_ preserves this—you can find the paths through usually `endpoints` or stored in the standard fields, and these are the ones you default to—but lays out a nominal API layout that can be assumed, and provides a stronger, more robust structure for what the rest of the API should (SHOULD) look like, and dedicated ways to find ambiguous paths. This provides a level of predictability in how to find objects or how APIs might be specified, and it provides more contextual out-of-band information to clients.

On the other hand, it also means that there can be situations where a fully AP system would not know what it is looking at. This doesn't matter for most functionality (the fact that the `id` always points to `https://<server>/<base-path>/v1/actors/{actor-id}/notes/{note-id}` is not relevant), but it does show up in the _custom functions_ like `:batchMessage`. 

To be clear: You will still see the links for everything, and those are the links you should use rather than  anticipating the path, but the paths are still specified in advance and expected, and servers may infer things from the format (and things like custom actions).

### Leaning into HTTP

Where ActivityPub has a mixed relationship with HTTP (moving several primitives into its own system), FeatherPub leans a little more heavily into HTTP with content headers and the `DELETE` and `PATCH` verbs. 

In FeatherPub, the `Delete` activity becomes a notification _that_ an object has been deleted from the server, rather than a command _to_ delete.  Similarly, an `Update` becomes a notice of updating while the actual underlying command becomes a `PATCH` method. 

### Routing Mechanics

The use of `audience`, `to`, etc in FeatherPub is entirely divergent and custom, with very little overlap. ActivityPub would recognize some of the primitives being used, but would treat what FeatherPub is doing as inherently malformed. 

This is not so much deliberate as it is considered an acceptable tradeoff: ActivityPub does not provide sufficient clarity around how any of these fields work and, to the extent that it does, it lacks functionality that FeatherPub considers critical.
