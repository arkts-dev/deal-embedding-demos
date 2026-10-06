# Cross-app demo

Five apps. The Gig Organizer hosts DEAL generation and execution. Todo, Calendar, Equipment Rental and Pizza publish capabilities; none needs model access.

Organizer stores the riders, venue inventory, matching, allocations and fulfilment links. It never relies on a connected app to compute coverage.

Organizer owns grants, context disclosure and activation. Every consequential operation happens only after native provider confirmation. A generated workspace returns prepared operations to the Organizer; it never becomes the authority for a commitment.

Everything here is user-observable behavior. Component names, layouts, capability signatures and generated source are the implementer's design, constrained only by the rules below.

## Non-negotiable split

**Kotlin and Compose own:** every app's screens, navigation, editing and persistence; the Organizer's riders, inventory, eligibility, allocation, fulfilment links and verification; provider menus, catalogues, availability and proposal records; caller authentication and independent consent per provider; granting access, disclosing context, resolving which provider owns a record, opening its native review, and deciding to activate a generated workspace; and every Compose component that renders generated content, including accessibility.

**DEAL and Deal UI own:** the executable behavior of a generated workspace — its state, actions, effects, reconciliation, and the conditional logic that compares options, checks coverage against a requirement, applies constraints and prepares operations. Generated source decides what is shown, in what order and with what consequences.

**deal-embedding owns:** checking and activating generated source, the component pack, the Compose rendering bridge, typed dispatch transport, value conversion, discovery, generic typed capability invocation, and the lifetime of live workspaces.

A Kotlin screen must never hard-code a generated workflow. Generated source must never hold a rider, inventory, allocation, catalogue, menu, reservation or order record, must never compute authoritative availability or eligibility, must never grant access, disclose context or confirm a write, and must never launch a provider screen. If a feature appears to require either, the design is wrong.

## Required extension

Building this demo **must** extend DEAL UI and deal-embedding. Current capability does not cover it.

Required from **deal-embedding**: the components listed under Component pack; typed dispatch payloads beyond today's string; the multiple live workspaces described below; the preparation-return path; value conversion for zoned timestamps and integer money; and generation scheduling that leaves an open workspace interactive.

Required from **DEAL UI**: whatever the component pack needs from checking — payload typing for new events, and only those semantics that a listed component cannot otherwise express. Typed event payloads already flow through checking and generated DEAL; the missing half is transport.

Required in the same change as any pack or language change: generation guidance, because compilation checks guidance, pack and renderer agreement.

## Build order

The apps came first and are built. Add embedding and DEAL UI capability only when a concrete generated workspace cannot be produced without it, one capability per real need, verified on the device before the next. Nothing is added speculatively.

## Show

Friday at The Foundry. Time zone Europe/Berlin. Initialization picks a Friday; Saturday return follows it.

Glass Harbour and Static Bloom play. Soundchecks 16:00 and 17:00, doors 19:00, performances 20:00 and 21:15.

Static Bloom's lead vocal position is incomplete: the venue microphone and mixer input are allocated, the boom stand and the minimum-length cable are missing. One venue bass amplifier must serve Glass Harbour (preparation 15:30–16:30) and Static Bloom (16:00–17:00). Static Bloom needs six pizzas, two vegan, in its dressing room by 18:30.

Rental desk Friday 14:00–18:00. Alex collects 15:00–15:45, arrives by 16:15. Sam sets up and checks 16:15–16:45. Return Saturday 10:00–12:00.

Most of each rider is already covered, so the show is a real plan with three identifiable problems.

## What the organizer must see

Plain summaries derived from stored facts: "Missing: boom stand and cable." "Reserved. Collect before 16:15." "Committed · check still needed." "Checked and ready." "Both acts need this amplifier at the same time."

Supply, commitment and verification stay separate. A reservation, a completed task or a "delivered" status never counts as the organizer's own check. Only an organizer inspection makes a requirement checked and ready.

## Organizer

Four destinations: Overview, Riders, Resources, Fulfilment. Bottom navigation on phones, rail and list/detail on larger screens. Show and date always visible.

Overview: the show, a timeline of collection, soundchecks, doors, performances and delivery, and the items needing attention — incomplete vocal setup, the amplifier overlap, unarranged pizza. Each names the act, deadline and problem, and opens the requirement or conflict. Department and recent-change views below.

Riders: both acts, technical and hospitality. Requirements expand into dependencies; a group is complete only when every mandatory dependency is. Filters for missing, conflicts and awaiting approval. Selection of a single requirement or a whole group. A native editor for adding or changing requirements. Selection is the normal way into DEAL; typing an instruction is optional.

A requirement surface shows participant, quantity, specification, location, timing window, mandatory-or-negotiable, permitted substitutions, dependencies, current allocation or resolution, and linked tasks, events, reservations and orders, with notes and the rider reference. Cable compatibility means connector types and minimum usable length; a stand means boom capability. Name matching must never satisfy a requirement. An edit that invalidates an allocation or commitment shows the affected links and keeps the commitment record rather than pretending it was cancelled elsewhere.

Resources: grouped by Sound, Backline and Hospitality, with specification, available quantity, source, availability window, allocated quantity and destination, and warnings. Detail shows the allocation timeline and linked requirements, with editing, allocation and release. The amplifier conflict shows both overlapping preparation windows and offers: change a window, choose another compatible resource, or open a generated workspace to compare. Changing a window changes the plan, not the act's rider, and needs a recorded agreement.

Fulfilment: grouped by equipment, hospitality and coordination, with views by person and by deadline. Each item shows the requirements it covers, owner, deadline, resolution state, linked external records with their last reported status, and any outstanding or failed step. Actions: open the connected app, refresh status, arrange the remaining work, and record verification. Verification is native and item-level — microphone, stand, cable and assigned input together; quantity, dietary coverage and correct dressing room; the agreed allocation or the collected replacement amplifier.

Connected apps are listed with their published capabilities, host access controls, provider consent status, and actions to open the provider or refresh discovery. Access is granted here, never inside generated content.

## Generated workspaces

Three intents generate a workspace. Each opens from a native selection; none is reached by writing a prompt.

- **Vocal rental resolution** — from selected boom-stand and cable requirements in Riders, or from "arrange remaining work" in Fulfilment.
- **Pizza hospitality** — from the dressing-room pizza requirement.
- **Amplifier alternatives** — from the amplifier conflict in Resources.

A workspace is a checked `.deal` and `.dealui` pair generated for that intent. It is compiled on device, runs in JavaScriptSandbox, renders through the same Compose components as the rest of the app, and is labelled as generated content with the Organizer's navigation and authority controls outside it. Generation shows progress and stays cancellable; failure leaves the current plan untouched.

Generated workspaces **read and prepare only**. They hold no records and confirm nothing. Every operation they prepare is confirmed in the owning provider's native review, which the Organizer opens.

### Vocal rental resolution

The workspace compares rental options for the selected requirements and shows, per option, exactly what it covers, whether both missing dependencies are covered, availability, price, collection window, return window and terms. The 3 m cable must appear as insufficient length, never as a satisfying match. The package and its separate items must both be visible as alternatives, with the package correctly consuming the same stand and cable stock.

The user adjusts quantities, chooses an option and picks collection and setup times that respect the soundcheck, the desk hours and arrival by 16:15, then prepares a reservation, a pickup task and calendar activities.

### Pizza hospitality

The workspace composes an order against the requirement: six pizzas with at least two vegan, delivered to Static Bloom's dressing room by 18:30. It shows products with dietary and allergen information, quantity controls, a running coverage summary against the requirement, full price, and the available delivery windows. A window that cannot guarantee arrival by 18:30 must be marked as such and must never be presented as satisfying the requirement.

The user prepares the order, a receiving task and a delivery activity.

### Amplifier alternatives

The workspace compares the two ways to resolve the overlap: a changed preparation window, and renting a second compatible amplifier. It shows the conflict plainly, what each option costs in time or money, and which act is affected. Neither option silently changes an agreed rider; the scheduling alternative still requires a recorded agreement, and the rental alternative prepares a reservation through the same provider flow.

### Prepared operations and native confirmation

A prepared operation is a described, unconsumed intent: reserve these items for this period, create this task for this person, add this event at this time. The workspace hands them back through the Organizer's preparation capability, which stages a descriptor for native review and performs no write.

The Organizer then shows the concrete operations with prices, times and terms and opens each in its owning provider's native review. Results are reported per operation. If a reservation succeeds and a calendar event is declined, the reservation stands and only that step remains outstanding. Cross-app work is never presented as a single transaction.

Reopening a confirmed proposal returns the existing record. Retrying must never duplicate a task, event, reservation or order. A provider that confirms while the Organizer loses the reply is an unknown outcome to be queried, not a failure to retry blindly.

## Live workspaces

Several generated workspaces stay live at once. Each keeps its own sandbox isolate and its own state; they share the connected sandbox engine. Isolating workspaces per intent is deliberate: state separation stays honest and one hung workspace cannot stall another.

The Organizer lists live workspaces by their generated title. Opening one restores its state without regenerating; closing one releases its isolate and its capability session. Workspaces persist across app and process restarts, including their source, their compiled output and their state.

A workspace refreshes its provider reads when reopened, so what it shows is current. Its design must tolerate provider data having changed since generation: an option that was available may be gone, a window may be full, a price may differ. A stale workspace must say so rather than presenting old facts as current.

## Todo

Owns tasks, assignees, due times and checklists. The journey produces: collect the vocal equipment (Alex, before setup, checklist for stand, cable and reservation reference); set up Static Bloom's vocal position (Sam, checklist for inspecting the microphone, connecting the cable, fitting the stand and checking the assigned input); receive the pizzas (Morgan, checklist for six pizzas, two vegan, correct room).

List groups by show and orders by deadline, with manually created tasks under Other tasks, filters for open, completed and all, and cards showing title, assignee, due time, checklist progress, show and overdue state. Items persist when checked. Completion requires the mandatory checks; reopening keeps checklist state.

Detail adds status, location, instructions and linked records, which navigate to their providers. Actions: edit, complete or reopen, open the show plan, open a linked record, delete. Deleting a task cancels nothing else.

Publishes reading tasks by show and by reference, staging a task proposal, and reading a proposal outcome. The Organizer shows "Task created for Alex", "Declined" or "Still awaiting approval", including confirmed details and the task reference, and sees that outcome even after either app is closed and reopened. Confirming the same proposal returns the same task and never creates a duplicate. All of it requires authorized caller identity and provider consent.

## Calendar

Owns events, times and reminders in one dedicated demo calendar. Personal calendars are untouched.

Seeds the show schedule once: both soundchecks, doors and both performances. Collection, setup and delivery activities are created through the journey.

Agenda shows a time gutter and events with title, interval, location, responsible person, show label, reminder and fulfilment links, for a selected date with neighbours available. Overlaps are shown together. Detail adds the show reference and linked records. Actions: edit, open related records, delete. Editing changes this calendar only; the Organizer reflects the new time on refresh and flags any violated requirement deadline.

Publishes reading events by show and range and by reference, staging an event proposal, and reading its outcome with confirmed details. Users may adjust a proposed event before adding it, so the Organizer must compare the actual confirmed times with its plan, because an adjusted event can break a deadline. Confirming the same proposal returns the same event.

## Equipment Rental

Owns catalogue, availability, quotes and reservations. Local and labelled as a demo transaction; no payment and no supplier.

Catalogue: boom stand (adjustable, adapter fitting the venue clip) €8, stock 2; XLR female to XLR male 10 m cable €5, stock 3; a package of one stand and one 10 m cable for €12 that consumes that stock rather than extra inventory; a 3 m cable of the same connectors for €3, which fails the rider's minimum length; a bass combo of at least 300 W with 6.35 mm input and balanced XLR DI output for €40, stock 1, whose specification both acts accept and which is the only alternative to the venue amplifier. Prices are for the Friday-to-Saturday period, with a €50 refundable deposit shown separately. Quotes last 15 minutes and hold no stock. Collection only. Exact items are reserved; substitutions need approval.

Users pick a rental period and browse by category, seeing specification, availability for that period and price, then build a quote. Quote shows contents, period, collection location and hours, charges, deposit, total, expiry, availability and substitution terms, and can be edited, refreshed when expired, or turned into a reservation. Reservations list as upcoming, active or closed with equipment, period, collector and status. Detail supports marking collection and return with item-level lists, so a partial collection stays visible, plus cancellation. Cancelling releases stock and cancels nothing in other apps.

Publishes equipment search by specification, quantity and period; quote requests; staging a reservation proposal; reading its outcome with the reservation reference; and reading a shared reservation including item-level collection state. Search returns facts about equipment, never a claim that a rider is satisfied; the Organizer compares facts with its requirements.

## Pizza

Owns menu, delivery options and orders. Local and labelled as a demo order.

Menu: Margherita 30 cm €10 vegetarian, milk and wheat; Garden Vegan 30 cm €12 vegan, wheat; Pepperoni 30 cm €13 meat, milk and wheat. Delivery €5 per order. Dietary labels describe menu data and are not an allergy guarantee. Delivery goes to Static Bloom's dressing room at the demo address through the stage entrance, received by Morgan, whose contact details are clearly fictional.

Window 17:45–18:15 meets the 18:30 requirement; 18:15–18:45 cannot guarantee it; 18:45–19:15 is too late. Offers last 15 minutes and confirmation rechecks availability.

Users browse by dietary filter, see size, price, ingredients, allergens and quantity controls, and build a basket. Basket shows quantities, dietary counts, subtotal, delivery, total, address, recipient, instructions and available windows, and compares the selection with the requirement. Order review revalidates products, prices and slots before confirming and returns actual contents, price and window; repeating it returns the same order.

Orders list as upcoming, completed or cancelled, with contents, delivery details, links and history. Delivery status advances only through explicit demo controls, never presented as live tracking. Cancellation is allowed while confirmed and refused once preparing. "Delivered" is the provider's state; the Organizer records receipt and dietary checks itself, and cancelling cancels nothing in other apps.

Publishes reading the menu and delivery options, staging an order proposal, reading its outcome with the order reference, and reading a shared order with contents and status.

## Journey

The Organizer's overview surfaces the vocal shortage. The requirement shows microphone and input covered, stand and cable missing. Those two are selected and fulfilment is arranged.

The native request sheet states the goal, deadline, responsible person and whether to preserve existing commitments, and names the available apps with their access state. The disclosure summary lists exactly what the model receives: the selected requirements with their specifications, show timing and constraints, and capability descriptions. Provider data arrives later, when the checked workspace runs its capability calls; it is never sent back to the model.

The generated workspace compares the package, the separate items and the insufficient 3 m cable, and checks the chosen collection 15:00–15:45 with Alex, setup 16:15–16:45 with Sam, arrival by 16:15, and return Saturday 10:00–12:00 against the soundcheck and the desk hours, exposing anything unresolved rather than choosing an impossible plan.

The reservation is prepared and reviewed in Rental, where availability and price are revalidated and the demo reservation confirmed. Only then are the pickup task, the setup task and the calendar activities prepared, each confirmed in its own app with the Organizer retrieving each outcome. Nothing is written invisibly in a chain.

Declining the collection event on purpose leaves the reservation, the pickup task and the setup task intact and the scheduling unresolved; the workspace offers to prepare that event again from the confirmed terms. Reopening a confirmed proposal returns the existing record without duplicating anything.

Pizza follows the same shape: goal, six pizzas with at least two vegan, delivery by 18:30, Morgan as recipient, with Pizza, Todo and Calendar available. The workspace composes the order, marks the too-late window, and prepares the order, the receiving task and the delivery activity after Pizza confirms.

The amplifier conflict opens a workspace comparing a changed preparation window with renting a compatible amplifier. The scheduling alternative is agreed outside the demo and recorded natively; the rental alternative reserves the second amplifier and updates the allocation.

Finally the providers report what they know — tasks completed, items collected, delivery advanced through demo controls — and the Organizer refreshes the links, then verifies the vocal setup, the pizza receipt and the amplifier resolution itself, which is the only thing that makes a requirement checked and ready.

## Component pack

The pack defines this renderer's vocabulary. The current nine components — text, hero, button, column, card, spinner, time, integer and toggle — cannot express the three workspaces. Each addition below is required by a named interaction in this document; an addition with no such interaction is not part of this demo.

Generated workspaces must reach the same Compose quality as the rest of the apps. Same components, same motion, same polish. Only the visual accent and the boundary label distinguish generated content; it is never a degraded or differently built surface.

**Selection and comparison** — an option list whose entries carry a title, supporting lines, a price, availability and a selected state, and which reports the selected identity; used by the rental comparison and the amplifier alternatives.

**Expandable section** — collapsible content with a header and a summary line; used to inspect an option's contents and an order's details.

**Quantity control** — an integer stepper with bounds, reporting its value as an integer payload; used for equipment quantities and pizza quantities.

**Text entry** — single-line and multi-line input with a length bound, reporting its value as a string payload; used for recipient, instructions and notes.

**Single selection** — a compact choice among a few labelled values, reporting the chosen identity; used for delivery window and collection window.

**Filter chips** — a small set of mutually independent filters; used for product category and dietary filter.

**Keyed list** — a scrollable list of keyed entries; required by the catalogue, menu and option lists.

**Progress and status** — determinate progress, and empty, unavailable and failed states with a retry action; used while providers are queried and when a workspace's data has gone stale.

**Notice** — an inline message with an informational, warning or error tone; used to state that a window cannot guarantee the deadline, that an option covers only one dependency, and that a price changed since generation.

**Adaptive container** — a two-pane comparison layout on wider screens that collapses to one pane on phones; used by every comparison workspace.

Whether a capability is expressed as a new component or as a composition of existing ones is an implementation decision, as is naming. What is fixed is the interaction each must support, the payload type each event carries, and that each is checked, rendered in Compose, and described in the guidance.

## Typed dispatch

The checker and generated DEAL already carry typed event payloads; the sandbox bridge and the renderer currently pass only strings. Integer, boolean and string payloads must reach generated actions as the declared type, with validation on both sides. Compose may hold only transient presentation state — focus, keyboard, picker visibility, scroll position. Committed values, selections and transitions stay in DEAL.

## Values

Scheduled times carry date, time and UTC offset with the Europe/Berlin identifier; intervals carry both ends; money is whole integer cents with a currency code, preserved through binding conversion. Providers return actual confirmed times and prices, and the Organizer rechecks them against deadlines and the budget.

## Boundaries

Generated content cannot launch arbitrary activities or invent a trusted review destination. Navigation is resolved natively against the discovered provider. The Organizer stays free of provider-specific adapters; each provider publishes its own contract and the Organizer discovers it.

The session request ceiling must accommodate repeated catalogue, menu and outcome queries without breaking a usable workspace, and must survive a workspace being reopened and refreshed.

Generation must not block open workspaces: inference is scheduled separately from the live sessions, while activation stays serialized and authority unchanged.
