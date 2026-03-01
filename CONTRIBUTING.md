# Contributing to LarpConnect

First off, thank you for considering contributing to LarpConnect!

LarpConnect is partly an experiment to see what the limits of AI are for
programming complex tasks while maintaining strict code quality standards and
rules. But it is, at its core, first and foremost, an online distributed
application that is designed to run on the internet. So we welcome contributions
_so long as they adhere to the guidelines and checks put into place_.

> [!IMPORTANT]] AI Policy: Purely AI-generated contributions without human
> oversight are not permitted. Contributors must review and take full
> responsibility for their code. See [AI_POLICY.md](./AI_POLICY.md) for details

## Table of Contents

- [Ground Rules](#ground-rules)
- [Your First Contribution](#your-first-contribution)
- [Getting Started](#getting-started)
- [How to Report a Bug](#how-to-report-a-bug)
- [How to Suggest a Feature or Enhancement](#how-to-suggest-a-feature-or-enhancement)
- [Code Review Process](#code-review-process)
- [Coding Standards and Technical Requirements](#coding-standards-and-technical-requirements)
- [Commit Message Conventions](#commit-message-conventions)

## Ground Rules

LarpConnect is a high-complexity project that values correctness, testing, and
adherence to rigid coding standards.

- **Quality first:** All contributions must pass the full suite of automated
  checks, including tests, static analysis (Checkstyle, SpotBugs, ErrorProne),
  and formatting (Spotless).
- **Communication:** For major changes or enhancements, please create an issue
  first to discuss your proposal.
- **Respect the architecture:** Adhere to the multi-module structure and
  dependency management rules.
- **Linear History:** We maintain a linear commit history. Rebasing on the main
  branch is required.
- **You Are Not an AI:** You aren't expected to write like an AI or engage with
  the `@AiContract` blocks at all. We have tools that will follow along and
  hopefully clean these up accurately.

## Your First Contribution

Unsure where to begin? Look for issues labeled "beginner" or "help wanted".

If you're new to open source, here are some helpful resources:

- [How to Contribute to an Open Source Project on GitHub](https://egghead.io/series/how-to-contribute-to-an-open-source-project-on-github)
- [First Timers Only](http://www.firsttimersonly.com/)

## Getting Started

1. **Fork the repository** and create your branch from `main`.
2. **Branch naming:** Feature branches created by AIs must prefixed with
   `<ai name>/`, e.g., `jules/`. This does not apply to humans.
3. **Set up your environment:** Ensure you have Java 25 installed.
4. **Make your changes.**
5. **Format your code:** Run `./gradlew spotlessApply` before committing.
6. **Verify your changes:** Run `./gradlew check` to execute tests and static
   analysis.
7. **At least once before submitting:** Run `./gradlew build`.
8. **Submit a Pull Request.**

## How to Report a Bug

### Security Disclosures

If you find a security vulnerability, do NOT open an issue. Please contact the
maintainers directly.

### Filing a Bug Report

When filing an issue, please include:

- A clear, descriptive title.
- Steps to reproduce the issue.
- Expected behavior vs. actual behavior.
- Any relevant logs or screenshots.

## How to Suggest a Feature or Enhancement

If you have a feature idea:

1. Open an issue describing the feature.
2. Explain why it's needed and how it should work.
3. Discuss the proposed implementation with the maintainers.

## Code Review Process

- All pull requests will be reviewed by the core team.
- Feedback will be provided on code quality, architectural alignment, and
  adherence to standards.
- You may be asked to rebase your branch or update your tests.

## Coding Standards and Technical Requirements

Detailed technical standards are maintained in [AGENTS.md](AGENTS.md). Key
highlights include:

- **Java 25:** Use modern features like Records, Sealed Classes, and Pattern
  Matching.
- **Null Safety:** Use `Optional<T>` for potentially absent values. No direct
  `.get()`.
- **Dependency Injection:** Use Guice with constructor injection.
- **Testing:** JUnit 5, AssertJ, and Mockito. Follow the naming pattern:
  `<method>_<what is being tested>_<expected outcome>`.
- **Immutability:** Prefer immutable collections and final variables.
- **Logging:** Use SLF4J.

## Commit Message Conventions

1. **First line:** 74 characters or less, plain text.
2. **Body:** Separated by a blank line, written in Markdown.
3. **Descriptive:** Clearly explain what was done.

---

For more details, please refer to the [README.md](README.md) and
[AGENTS.md](AGENTS.md).
