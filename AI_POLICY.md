# Use of Generative AI in Contributions

> [!IMPORTANT]
> This policy has **ZERO TOLERANCE** around anything labeled **MUST**. Especially for wilful violation. 

Summary position: 

> We use AI to help us build the world, but AI will not run the world.

Generative AI agents are allowed in this project, but with some guiding principles. 

1. **Mandatory Disclosure**: AI Contributions **MUST** be disclosed. The agent _MAY_ sign on as a co-contributor or you _MAY_ indicate
   such in the body of the commit, but either way it **MUST** be disclosed. The contribution **SHOULD** include information on _that_
   the contribution includes AI, the _extent_ of AI contribution in the PR (e.g., tests, scaffolding, all of it), **and** _which_ AI agent or
   agents were used (e.g., jules/gemini 3, claud 4, GPT-5).
2. **Forbidden Tools**: The following AI tools are forbidden for _any_ use in contributions. You **MUST** not use them in any way on this project. This list may change over time:
   * Grok/xAI
   * Llama/Meta AI
   * Devin/Cognition AI
   * Perplexity AI
   * Stability AI (and in general we won't use any AI generated "art")
3. **Principal Authorship**: A human (_you_) **MUST** be listed as the primary author.
4. **Accountability**: A human (_you_) are responsible for any contributions put under your name, regardless of if they use an AI. You
   **MUST** ensure that the terms and conditions of the generative AI tool do not place any contractual restrictions
   on how the tool’s output can be used that are inconsistent with the project’s open source software license or policies.
   * **Anti-Plagiarism**: If an AI generates a known algorithm (e.g., a specific R-tree implementation), the contributor is responsible for verifying it does
     not infringe on restrictive patents or licenses incompatible with this project. The contributor is also responsible for ensuring that
     the algorithm's origin is accurately cited.
   * **Proper Usage** If others contributions are included in the output from an AI tool, you **MUST** ensure that it is compatible with the license structure
     of both the code's origin AND this project's policies.
5. **Human Validation**: You **MUST** personally review and validate any code contributed by an AI agent in your name. This **MUST** be done _BEFORE_
   submitting it for others to review.
6. **Quality Standards**: AI Agents output is held to extremely strict quality standards and the build must pass successfully (humans are also held to this standard,
   but many of the elements have seemed easier for humans than AI agents). They **SHOULD** follow the guidance in `AGENTS.md` and `.agents/SKILLS.md`.
7. **Isolation of Concerns**:
   * Agent contributions **SHOULD NOT** modify the code quality system unless _directly_ requested to do so. 
   * Agent contributions **SHOULD NOT** mix code quality changes with functional changes.
8. **Branch Naming and Grouping**: Agent contributions **SHOULD** as a best effort name their branches as `<agent name>/<branch name>`, e.g., `jules/my-feature-change`.
   This is an easy one to miss, but check for it the best you can.
9. **Security and Privacy**: You **MUST NOT** include credentials, non-anonymized production data, or proprietary logic in prompts to AI agents for
   for contributions to this project.

## A Note on Using AI in Pipeline Versus Process

Generative AI tooling may **never** be integrated into the actual pipeline for this tool. This tool is about building and coordinating community
spaces and games, _not_ about using AI agents for tasks.

AI tools may be used to contribute source code, they may never be part of the core loop of this system.

## Inspirations and Further Reading

* [ACM Code of Ethics and Professional Conduct](https://www.acm.org/code-of-ethics)
* [Linux Foundation Generative AI Principles](https://www.linuxfoundation.org/legal/generative-ai)
* [IEEE Ethically Aligned Design](https://standards.ieee.org/wp-content/uploads/import/documents/other/ead_v2.pdf)

