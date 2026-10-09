---
kind: design
id: organizer-assistance
created_at: 2026-10-09T13:36:25Z
updated_at: 2026-10-09T13:36:25Z
status: approved
references:
  - README.md
  - .deus/research/deal-studio-comparison.md
  - .deus/research/unified-logging.md
  - .deus/research/workspace-lifecycle.md
  - https://github.com/nexu-io/open-design/tree/802708f6c9f294347ef777b1fda49b9cbe26ef72
  - https://github.com/egavrin/deal-studio/tree/f47d86134f824d5082f6f3e960e6821ebc1dbc81
problem: The organizer must resolve outstanding event requirements without navigating generated artifacts or choosing implementation mechanisms.
options:
  - Requirement-led assistance with native presentation and one engineering doorway.
  - A workspace launcher with separate catalogue and source-generation choices.
decision: Requirement-led assistance; preserve real execution and expose its mechanisms only through Inspect.
acceptance:
  - An organizer can find an outstanding equipment need, obtain options, select an item, review it, and return through the requirement.
  - Production calls are real and observable at owned boundaries; fixtures never substitute for live integration evidence.
  - Ordinary screens use native task language, honest loading and error states, and no engineering taxonomy.
  - Inspect exposes actual route, provenance, checks, calls, source, capture controls, and logging limitations.
  - Authority, explicit reads, exact machine values, durable source, and bounded failure behavior remain intact.
---

# Organizer assistance

## Objective and terms

The actor is the event organizer. The outcome is resolving an outstanding equipment requirement through suitable options and local review, not creating a technical artifact. The initial complete journey is Rental equipment assistance; other event concerns must not imply implemented integrations merely because generic generation exists.

A requirement is an event need with quantity, specification, location and exact period. Assistance is the interface helping resolve it. A workspace is the retained source and live instance backing assistance; this term belongs in engineering views. Inspect is the single engineering doorway. Review is native inspection of a locally prepared selection, not booking.

## Invariants

1. The event and its requirements are the front door. Prioritize Needs attention and Ready. An outstanding equipment need offers Find equipment; returning through the same requirement reuses its retained assistance. Users do not hunt through a technical shelf.
2. No ordinary screen asks users to choose catalogue versus source generation. Existing DEAL choice-first policy selects a checked catalogue construction when suitable, otherwise bounded source generation. Inspect reports the observed route; no production bypass exists merely to force a demonstration.
3. Production toolchain, model, inter-app, and host/application calls use real implementations. No canned response, fake inventory, simulated progress, inert action, or success-only substitute may represent an operational result. Deterministic fixtures and mocks are explicitly test-only and do not establish live model/provider integration.
4. Preparing an interface and fetching stock are separate operations. Provider reads remain explicit. Selection and local preparation do not reserve, confirm, or write provider state. Review clearly says Not booked. The interface never marks a requirement covered merely because generation or local staging succeeded.
5. Back preserves assistance. Cold reopening rechecks saved source without inference. Explicit replacement, cancellation, or failure preserves the prior accepted assistance. Starting another model run requires deliberate user action; exhaustion never silently restarts.

## Decisions

### Native journey and visual contract

Overview presents actionable outstanding requirements before decorative or secondary content. Find equipment opens a compact requirement-led request with supplied quantity and period, relevant optional budget/preferences, and one Find options action. Extra instructions are optional and secondary. Remove generation goal chips, unrelated responsibility/commitment switches, module lists, and model explanations from this journey. Necessary consent and understandable data disclosure remain visible rather than hidden as debugging.

Preparation shows a truthful short phase, spinner and cancellation. During each asynchronous operation, disable its initiating control and conflicting edits, preserve relevant entered values, and prevent duplicate submission. Keep cancellation, Inspect and safe navigation available where their lifetimes permit. Re-enable controls after success, failure or cancellation; validation and authority may still disable inappropriate actions. Do not invent completion percentages or show unchecked executable previews.

Once ready, Equipment options and practical controls dominate. Native header and generated content must not duplicate titles or implementation explanations. Show availability, quantity, total price, search and budget where the task requires them. Loading, empty results, invalid input, permission denial, transport error and retry have distinct concise states. Retry invokes the actual operation. Review selection leads to native review; change requirements and start again are task actions, not debugging actions.

Use Compose Material components and one shared presentation contract: 4/8/12/16/24/32 dp spacing, 16–20 dp page padding, a consistent small set of Material shapes, theme typography, restrained accent, legible surface hierarchy, and minimum 48 dp interactive targets. Text and icons carry status independently of color. Respect system insets, keyboard, dark mode, grayscale and enlarged text. Motion is short and purposeful; no continuously decorative background or animation suggesting unobserved progress. Native defaults take precedence over importing web styling.

### Engineering visibility

Each screen has one consistent Inspect entrance, not parallel Details and Execution entrances. Inspect shows the relevant request/build/workspace and permits global activity/history access, including retained unlinked artifacts and explicit deletion. Workspace terminology, actual route, provenance, attempts, source, contracts, checks and raw diagnostics belong here.

Every embedding-owned invocation boundary must expose its real start and terminal result, error or cancellation, with available duration and causal correlation: compiler calls, model exchanges, inter-app transport, host/application actions, effect delivery and publication. Inspect must be usable during calls and after failures. Reopening cannot falsely imply fresh generation. No hidden AI reasoning or invented provider-internal spans are displayed.

Reuse the unified DEAL logging contract and existing mechanisms. Metadata is the default; explicit bounded content capture is enabled before the operation and is not retroactive. Never capture credentials or authorization headers. Raw artifacts stay exact, private and marked untrusted. Show omissions, truncation, eviction, queue/storage loss and active versus recovered-but-incomplete health. Missing terminal events remain incomplete, not inferred success. Logging observes rather than routes, authorizes, retries or changes results; it is not a lossless audit guarantee.

## Boundaries

DEAL owns generation, repair, route and logging policy. Deal UI owns UI state/effect/session semantics. Native embedding owns compilation invocation, Android transport/authority, storage, scheduling and Compose rendering. UI polish must not invert these dependencies.

Apply the presentation contract to native shell, catalogue copy, authored fixtures and generation guidance. Never silently rewrite retained AI source or strip arbitrary rendered text. Extend widgets only for a demonstrated task gap, with typed props/events, validation, loading/disabled/error behavior, accessibility and actual compiled-JS/Android tests. Consult Studio's renderer/pack examples for evidence; do not import its interpreter, broad pack or a parallel renderer.

## References

OpenDesign's pinned HomeHero implementation and tests demonstrate outcome examples and input/submission states; its tokens and Material DESIGN.md demonstrate coherent visual rules. Its material source/evidence.md explicitly identifies a curated fixture, not authoritative Android guidance. Borrow discipline, not its creation taxonomy, web engine or visual assets.

Studio's pinned GeneratedAppStudioScreen.kt demonstrates result-first presentation, fullscreen use, saved items and collapsed technical details. Borrow presentation hierarchy, not ordinary source tabs or generation modes. These repositories were inspected as source, not validated by running them. Independently implement native patterns; do not assume Studio code licensing.

Reject a developer workspace launcher, route-selection cards, cosmetic hidden failures, fake calls, wholesale engine adoption and speculative workflow/telemetry platforms: none resolves the organizer's demonstrated concern. Local research references in front matter establish the existing logging and durable-source contracts.

## Acceptance

Verify the complete overview → requirement → preparation → options → selection → review → return journey on device. Ordinary screens contain no technology badges, compiler codes, module paths, source tabs or repair counters. Inspect exposes the actual mechanisms without hiding operational warnings from users.

First test deterministic behavior and real compiled toolchain/sandbox/provider integration without inference: duplicate prevention, disabled/spinner/re-enable states, cancellation, denial/retry, failure preservation, exact arguments, unchanged provider persistence, Back and cold reopen. Review the whole screen set in light/dark, grayscale, enlarged text and with the keyboard visible; build success alone is insufficient.

Then run explicitly budgeted fresh model tests for catalogue and source behavior. Establish routes and outcomes from logs and actual artifacts, retaining failures and call denominators. Fixtures do not count as live model success; a rendered screen does not prove a catalogue route. No further inference budget is granted by this design. Booking/provider writes, generalized generation reliability and complete provider-internal telemetry remain outside this slice.
