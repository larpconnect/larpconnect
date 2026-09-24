# Njall Technical Glossary

| Term | Definition | Use When | Avoid |
| --- | --- | --- | --- |
| Njall | The internal name of the larpconnect application | Referring to the application in internal documents or source code | In external documents that are designed for the general public -- People who may be interfacing with but not installing the software.  In those cases, use "LarpConnect." Avoid prefixing it: the project is "Njall" not "Project Njall" |
| Actor | A pekko actor | Referring to the actor model implementation | |
| Tenant | A group that is hosted on this system and maintains their own external identity (usually via their own domain). May include stronger (e.g., a separate schema, separate actors) or weaker (e.g., a shared schema, shared actors, but still separate data) isolation guarantees. Tenanting is at the studio level and all studios are at least weak tenants | | |
| Multitenant | An architecture pattern where a multiple accounts of a particular type share access to the same instance of the application, database, etc but their data is kept logically separated | | |
| DTO | A data transfer object | Referring to a serialized data object that is being used in the system | |
| DAO | A data access object | Referring to an abstraction for working with DTOs | | |
| Plane | A horizontal cut through the layers of the application that requests pass through | | |
| Verticle | A slice of the application that centers on a specific responsibility | Discussing end-to-end functionality (e.g., admin) | |
| Data plane | Refers to the underlying data components of the system and their representation in the software, everything from the ORM and data representations back to the actual database and file store | | |
| API plane | Refers to the parts of the system that are facing to the user and that handle security, communication, etc | | |
| Application plane | Refers ot the internal workings of the application itself and internal communication over either an event bus or message queue | | |
| Admin verticle | Refers to the part of the system that manages the administration of the _server_ as opposed to the individual tenants | | |
| User verticle | Refers to the part of the system that manages the day-to-day actions of users, studios, etc | | |
| Module | A gradle module (e.g., :server) with a set of code that is compiled OR a guice module specifying bindings that are used at runtime | | |
| Library module | Any gradle module that does not have the ability to run on its own and that exists to provide functionality to other modules | | |
| Application module | Any gradle module that is capable of being executed. Has a main method | | |
| FeatherPub | An ActivityPub inspired protocol that is being developed out of Njall | Talking about the protocol that njall uses to talk to other serviers or clients | Speaking generally about federated protocols that may or may not be compatible (e.g., ActivityPub) |
| Migrate | The part of the application that keeps the database in sync and up-to-date | | |
| Server | The part of the application that hosts the API and is exposed to the outside world | | |
| Webfinger | An account lookup service specified by RFC 7033 | | |
| JRD | JSON Resource Descriptor - the response format for Webfinger lookups | | |

