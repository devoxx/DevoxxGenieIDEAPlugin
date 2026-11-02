---
slug: matt-pocock-addy-osmani-agent-skills
title: "Matt Pocock vs. Addy Osmani: Two Approaches to Better AI Coding with Agent Skills"
authors: [stephanj]
tags: [prompt engineering, testing, code review, architecture, open source]
date: 2026-11-02
description: Compare Matt Pocock's composable engineering skills with Addy Osmani's Agent Skills, from requirements discovery and domain modeling to TDD, adversarial review, and delivery discipline.
keywords: [agent skills, Matt Pocock, Addy Osmani, AI coding, test-driven development, domain modeling, code review]
---

# Matt Pocock vs. Addy Osmani: Two Approaches to Better AI Coding with Agent Skills

The frustrating thing about AI coding agents is not always that they cannot write code. It is that they can write plenty of code before discovering what you actually wanted.

They assume requirements, introduce unnecessary abstractions, and sometimes treat a successful build as proof that the problem is solved.

Agent skills offer one way to address this: package engineering practices into reusable instructions that an agent can follow. Two collections worth examining are:

- **[Matt Pocock — Skills for Real Engineers](https://github.com/mattpocock/skills)**
- **[Addy Osmani — Agent Skills](https://github.com/addyosmani/agent-skills)**

Both aim to make AI-assisted development more disciplined, but they put different emphasis on how to get there.

<!-- truncate -->

*This comparison reflects the published repositories reviewed on November 2, 2026. It is a comparison of their instructions, not a hands-on performance benchmark.*

## Skills are more than saved prompts

A useful skill does more than tell an agent to "write clean code." It describes when to apply a practice, what steps to follow, and what evidence is needed before moving on.

Osmani's collection makes that structure explicit: skills contain workflows, checkpoints, verification requirements, and guidance against skipping inconvenient steps. Its lifecycle commands connect specification, planning, implementation, testing, review, and shipping. [Source: Agent Skills](https://github.com/addyosmani/agent-skills)

The important shift is from asking an AI for an output to giving it a repeatable way of working.

## Matt Pocock: get aligned before getting productive

Pocock describes his skills as small, adaptable, and composable. Rather than handing control to a framework that owns the entire process, the idea is to assemble practices that fit how you work.

A central concern is misalignment: the agent builds something plausible, but not what you meant. His answer is to make questioning a deliberate part of development, through skills such as `grill-me` and `grill-with-docs`. [Source: Skills for Real Engineers](https://github.com/mattpocock/skills)

The interesting part is what happens to the answers.

### Build a shared language, not just a longer conversation

Pocock's `grill-with-docs` combines questioning with domain modeling. The domain-modeling skill challenges ambiguous terminology, checks explanations against the code, and records resolved concepts in a glossary.

It also distinguishes vocabulary from implementation decisions: the glossary defines the domain, while significant architectural trade-offs can become ADRs. [Sources: grill-with-docs](https://github.com/mattpocock/skills/blob/main/skills/engineering/grill-with-docs/SKILL.md), [domain-modeling](https://github.com/mattpocock/skills/blob/main/skills/engineering/domain-modeling/SKILL.md)

Imagine asking an agent to implement "account cancellation."

Does *account* mean a user identity, a customer organization, or a subscription? Does *cancellation* mean immediate deletion, disabling access, or stopping the next renewal?

Those are not naming details. They determine behavior.

My reading of Pocock's approach is that shared vocabulary is part of the development infrastructure. A precise term can prevent more mistakes than another paragraph of implementation instructions.

## Addy Osmani: carry discipline through to production

Osmani's collection presents a more explicit lifecycle, with commands such as `/spec`, `/plan`, `/build`, `/test`, `/review`, and `/ship`. It also covers concerns beyond implementation, including security, observability, migrations, and launch preparation. Individual skills can be used independently; adopting the whole lifecycle is not mandatory. [Source: Agent Skills](https://github.com/addyosmani/agent-skills)

Two skills illustrate its approach particularly well.

### Interview the intent

The `interview-me` skill asks one question at a time, attaching the agent's best guess so the user can correct its assumptions. It tries to uncover the intended outcome, audience, constraints, and definition of success before a specification exists.

Importantly, it also states when **not** to interview: clear mechanical changes and straightforward information requests do not need a requirements workshop. [Source: interview-me](https://github.com/addyosmani/agent-skills/blob/main/skills/interview-me/SKILL.md)

### Challenge confidence

The `doubt-driven-development` skill addresses a different problem: a plan or implementation can become convincing simply because the agent has spent a long time reasoning about it.

Its response is fresh-context adversarial review. A reviewer receives the artifact and its contract, without the author's persuasive reasoning, and looks for ways it could fail.

This is intended for non-trivial decisions—not every keystroke—and is distinct from a final code review. [Source: doubt-driven-development](https://github.com/addyosmani/agent-skills/blob/main/skills/doubt-driven-development/SKILL.md)

That distinction matters: clarifying what to build and challenging whether a proposed solution works are separate jobs.

## A revealing difference: their TDD instructions

Both collections require failing tests before implementation, but their current instructions are not interchangeable.

Pocock's `tdd` emphasizes behavior through public interfaces and requires agreement on the testing boundaries, or "seams." It rejects writing a large batch of tests followed by a large batch of implementation. Instead, it prescribes one test and one minimal implementation at a time.

Notably, it explicitly moves refactoring out of the implementation loop and into review. [Source: Pocock's tdd](https://github.com/mattpocock/skills/blob/main/skills/engineering/tdd/SKILL.md)

Osmani's `test-driven-development` uses the familiar **red–green–refactor** cycle. It also tells the agent to discover the repository's actual test tooling, reproduce bugs before fixing them, and run broader regression checks. [Source: Osmani's test-driven-development](https://github.com/addyosmani/agent-skills/blob/main/skills/test-driven-development/SKILL.md)

This is a practical reason not to install overlapping skills blindly: even two skills labeled "TDD" can prescribe different behavior.

## Which approach should you choose?

My editorial shorthand is:

| If your recurring problem is… | Start by exploring… |
|---|---|
| The agent misunderstands what you mean | Pocock's questioning and domain-modeling skills |
| Tests are coupled to internal implementation | Pocock's public-interface testing guidance |
| Delivery steps get skipped | Osmani's lifecycle workflows |
| Confident decisions go unchallenged | Osmani's doubt-driven development |
| Reviews focus only on functionality | Osmani's five-axis review: correctness, readability, architecture, security, and performance |

The last approach explicitly broadens review beyond whether the feature works, while favoring improvements to overall code health rather than demanding perfection. [Source: code-review-and-quality](https://github.com/addyosmani/agent-skills/blob/main/skills/code-review-and-quality/SKILL.md)

These are starting points, not exclusive territories. Both collections care about understanding, testing, and maintainability.

## Don't collect skills. Fix a failure mode.

My recommendation is to start with one recurring problem.

If the agent keeps building the wrong thing, improve requirements discovery. If fixes arrive without regression tests, establish a test-first workflow. If working code reaches production without operational preparation, strengthen the delivery checks.

Then evaluate whether the skill actually helped:

- Did it expose assumptions earlier?
- Did it reduce rework?
- Did it produce meaningful verification?
- Was the extra interaction worth the cost?

A skill file is guidance, not enforcement. Tests, CI, permissions, and human review still matter.

Pocock's collection makes a strong case for **better collaboration and shared understanding**. Osmani's collection makes a strong case for **explicit verification and delivery discipline**.

The opportunity is not to make an agent behave exactly like either author. It is to make your own engineering standards repeatable.
