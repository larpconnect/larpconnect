# LarpConnect: Connecting Larp Communities

> [!CAUTION]
> **DEVELOPMENT STATUS: PRE-ALPHA** This project is **not** ready
> for production traffic. **DO NOT USE IT IN PRODUCTION.** Core security,
> stability, and data integrity features are still under active development.

[![Build](https://github.com/larpconnect/larpconnect/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/larpconnect/larpconnect/actions/workflows/build.yml)

---

## Project Overview

**LarpConnect** is a platform designed to bridge and federate Live Action
Role-Playing (LARP) communities. It allows for the creation of interconnected
subcommunities that can communicate and share resources.

Think of it as a Discord-like or Mastodon-like experience tailored specifically
for the needs of LARPers, with a heavy focus on:

- **Community Federation:** Connecting disparate groups while allowing for local
  subcommunity autonomy.
- **Data and Metadata Management:** Tracking complex game data, character
  information, and announcements from staff in a usable, searchable format.
- **Logistics & Signups:** Integrated tools for event registration and resource
  tracking.

---

## Development

To build the project run:

```java
./gradlew build
```

---

## Technical Specifications

The project is a multi-module Gradle build designed for high performance and
type safety.

- **Language:** Java 25
- **Framework:** Vert.x 5
- **Build System:** Gradle (Multi-module)
- **Main Entry Point:** `:server`

## License

All source code and comments licensed under Apache 2.0

See [LICENSE](./LICENSE) for more details.
