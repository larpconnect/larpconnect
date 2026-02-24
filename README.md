# LARPConnect: Connecting LARP Communities

> [!CAUTION]
> **DEVELOPMENT STATUS: PRE-ALPHA**
> This project is **not** ready for production traffic. **DO NOT USE IT IN PRODUCTION.** Core security, stability, and data integrity features are still under active development.

---

## Project Overview

**LARPConnect** is a platform designed to bridge and federate Live Action Role-Playing (LARP) communities. It allows for the creation of interconnected subcommunities that can communicate and share resources.

Think of it as a Discord-like or Mastodon-like experience tailored specifically for the needs of LARPers, with a heavy focus on:

* **Community Federation:** Connecting disparate groups while allowing for local subcommunity autonomy.
* **Data and Metadata Management:** Tracking complex game data, character information, and announcements from staff in a usable, searchable format.
* **Logistics & Signups:** Integrated tools for event registration and resource tracking.

---

## Technical Specifications

The project is a multi-module Gradle build designed for high performance and type safety.

* **Language:** Java 25
* **Framework:** Vert.x 5
* **Build System:** Gradle (Multi-module)
* **Main Entry Point:** `:server` 

---

## Getting Started

### Prerequisites
* **JDK 25** or higher.
* **Gradle 9.3+** (via the included wrapper).

### Build Instructions
To compile the project and run the test suite:

```bash
./gradlew build
```


