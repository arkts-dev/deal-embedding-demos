# Cross-app demo

Five apps. The Gig Organizer hosts DEAL generation and execution. Todo, Calendar, Equipment Rental and Pizza publish capabilities; none of them needs model access.

Organizer stores the riders, venue inventory and matching. It never relies on connected apps to compute coverage.

Organizer owns grants, context disclosure and activation. Consequential operations happen only after native provider confirmation. A generated workspace returns records to Organizer; it never becomes the authority for a commitment.

## Non-negotiable split

This is fixed. Do not negotiate it during implementation.

**Kotlin and Compose own:** every app’s screens, navigation, editing and persistence; the Organizer’s riders, inventory, eligibility, allocation, fulfilment links and verification; provider menus, catalogues, availability and proposal records; caller authentication and independent consent for each provider; granting access, disclosing context, resolving which provider owns a record, opening its native review, and deciding to activate a generated workspace; and every Compose component that renders generated content, including its accessibility.

**DEAL and Deal UI own:** the executable behaviour of a generated workspace — its state, actions, effects, reconciliation, and the conditional logic that compares options, checks coverage against a requirement, applies constraints and stages prepared operations. Generated source decides what is shown and when, and what it asks providers to do.

**deal-embedding owns:** checking and activating generated source, the component pack, the Compose rendering bridge, typed dispatch transport, value conversion, discovery and generic typed capability invocation.

A Kotlin screen must never hard-code a generated workflow. Generated source must never hold a rider, inventory, allocation, catalogue, menu, reservation or order record, must never compute authoritative availability or eligibility, and must never grant access, disclose context, confirm a write or launch a provider screen. If implementing a feature appears to require either, the design is wrong.

## Required extension

Building this demo **must** extend DEAL UI and deal-embedding. This is not optional and it is not a refactor of existing capability: the current renderer cannot express the required workspace, and the current transport and bridge cannot complete the required cross-app journey. Those extensions are built as part of this demo, alongside the apps.

Required from **deal-embedding**: new checked Compose components (option comparison, expandable details, quantity and budget entry, text entry, date and window selection, single selection, filtering, long keyed lists, loading/failure/retry states, adaptive comparison layout); typed dispatch payloads beyond today’s string; value conversion for zoned timestamps, intervals and integer money; the authenticated open-review/return/outcome bridge; durable proposals and their links; and generation scheduling that does not block a live workspace.

Required from **DEAL UI**: typed event payloads beyond string where components need them, and whatever checked semantics field-level updates and validation require.

Required in the same change as any pack or language change: generation guidance, because compilation checks guidance, pack and renderer agreement.

## Build order

The apps come first. Kotlin screens, persistence, consent and proposal records are built and verified natively, so the embedding work is derived from real screens instead of invented surface.

Add to DEAL UI and deal-embedding only when a concrete screen cannot be built without it, one capability per real need, verified on the device before the next. A component or bridge must be requested by an actual app interaction — never added speculatively.

Everything shown here is observed by the user, not an implementation prescription. Component layouts, screen structure and capability naming are open.

## Show

Friday at The Foundry. Time zone Europe/Berlin. The user picks a Friday when initializing the demo; Saturday return follows it.

Glass Harbour and Static Bloom play. Soundchecks 16:00 and 17:00, doors 19:00, performances 20:00 and 21:15.

Static Bloom’s lead vocal position is incomplete: the venue microphone and mixer input are allocated, but the boom stand and the minimum-length cable are missing. One venue bass amplifier must serve Glass Harbour (preparation 15:30–16:30) and Static Bloom (16:00–17:00). Static Bloom needs six pizzas, two of them vegan, in its dressing room by 18:30.

Rental desk is open Friday 14:00–18:00. Alex collects 15:00–15:45 and brings the equipment to the venue by 16:15. Sam sets up and checks 16:15–16:45, before the 17:00 soundcheck. Return is Saturday 10:00–12:00.

Most of each act’s setup is already covered. The demo is a real plan with a few identifiable gaps.

## What the organizer must see

Plain summaries, derived from stored facts: “Missing: boom stand and cable.” “Reserved. Collect before 16:15.” “Collected. Setup check still needed.” “Checked and ready.” “Both acts need this amplifier at the same time.”

Supply, commitment and verification stay separate. A reservation, a completed task or a “delivered” status never counts as the organizer’s own check. A requirement is ready only after the organizer inspects it.

## Organizer

Four destinations: Overview, Riders, Resources, Fulfilment. Bottom navigation on phones, rail and list/detail on larger screens. The show and date stay visible.

Overview shows the show, a timeline of collection, soundchecks, doors, performances and delivery, and the items needing attention: incomplete vocal setup, the amplifier overlap, unarranged pizza delivery. Each item names the act, the deadline and the problem, and opens the requirement or conflict. Department and recent-change views sit below. This screen is fully usable without generation.

Riders shows both acts with technical and hospitality requirements. Requirements expand into their dependencies, and a group is complete only when every mandatory dependency is. Users filter to gaps, conflicts or proposals. They select single requirements or whole groups and choose Arrange fulfilment; they can also add a requirement through a native editor. Selection is the normal way into DEAL; typing an instruction is optional.

A requirement surface shows the participant, quantity, specifications, location, timing window, whether it is mandatory or negotiable, permitted substitutions, dependencies, current allocation or resolution, and linked tasks, events, reservations and orders, with notes and a reference to the rider. Cable compatibility means connector types and minimum usable length; a stand means boom capability. Name matching must not satisfy a requirement. Editing shows affected links when a change invalidates an allocation or commitment, and keeps the commitment record rather than pretending it was cancelled elsewhere.

Resources groups by Sound, Backline and Hospitality, and shows specifications, available quantity, source, availability window, allocated quantity and destination, and any warning. Detail shows the allocation timeline and linked requirements, with editing, allocation and release. The amplifier conflict shows the two overlapping preparation windows side by side, and offers: change a preparation window, pick another compatible resource, or explore alternatives with DEAL. Changing a window changes the plan, not the act’s rider, and needs recorded agreement when the acts must accept it.

Arrange fulfilment takes the selected requirements and lets the user choose a goal, a spending ceiling, a deadline, a responsible person, whether to preserve existing commitments, and an optional short instruction. It lists which connected apps are available and what context will be shared. Generation shows progress and stays cancellable; failure leaves the current plan untouched. These controls express intent; they do not pick a prepared workflow.

Generated workspaces are labelled as DEAL content, with native show navigation and authority controls outside them. For the vocal shortage the user compares compatible rental options, checks whether each covers both missing dependencies, sees prices, collection windows and deadlines, chooses one, and prepares a reservation, a pickup task and calendar activities. For pizza the user compares products and delivery windows against the requirement, checks dietary coverage and price, and prepares the order and coordination records. Layout and interaction are generated from the goal and the discovered capabilities. The demo needs enough checked Compose components for real option lists, expandable details, selections, quantities and progress feedback; authoritative editing and confirmation stay native.

Before anything is executed, the user reviews the concrete operations — for example reserving a stand and cable, creating a pickup task for Alex, scheduling collection before soundcheck — with prices, times and terms. Results are reported per operation. If the reservation succeeds and the calendar event fails, the reservation reference is kept and only the failed step is retried. Cross-app work is not presented as a single transaction.

Fulfilment groups by Equipment, Hospitality and Coordination, with views by person and by deadline. Each item shows the requirements it covers, the responsible person and deadline, its resolution state, linked external records with their last reported status, and any outstanding or failed step. Actions: open the connected app, refresh status, arrange the remaining work, and record verification. Verification is native and item-level: microphone, stand, cable and assigned input together for the vocal setup; quantity, dietary coverage and correct dressing room for the pizza; the agreed allocation or the collected replacement amplifier.

Connected apps are listed with their published capabilities, host access controls, provider consent status, and actions to open the provider or refresh discovery. Access is granted here, never inside generated content.

## Todo

Owns tasks, assignees, due times and checklists. The journey produces: collect the vocal equipment (Alex, before setup, checklist for stand, cable and reservation reference); set up Static Bloom’s vocal position (Sam, 17:00, checklist for inspecting the microphone, connecting the cable, fitting the stand and checking the assigned input); receive the dressing-room pizzas (Morgan, 18:30, checklist for six pizzas, two vegan, correct room). Assignees are editable names; there are no accounts or messaging.

The task list groups by show and orders by deadline, with manually created tasks under Other tasks, and filters for open, completed and all. A card shows title, assignee, due time, checklist progress, show and overdue state, and expands to items and completion. Items persist when checked. Completion requires the mandatory checklist items; reopening restores the task and keeps checklist state.

Task detail shows status, assignee, deadline, location, instructions, checklist and linked records — Organizer item, reservation, calendar activity, order — which navigate to their providers. Actions: edit, complete or reopen, open show plan, open a linked record, delete. Deleting a task cancels nothing else.

Todo publishes reading tasks by show and by reference, staging a task proposal, and reading a proposal outcome. Organizer shows “Task created for Alex”, “Declined” or “Still awaiting approval”, including confirmed details and the task reference, and sees that outcome even after either app is closed and reopened. Confirming the same proposal again returns the same task and never creates a duplicate. All of this requires authorized caller identity and provider consent.

## Calendar

Owns events, times and reminders, using one dedicated demo calendar. Personal calendars are untouched.

The show schedule initializes soundchecks, doors and performances. Collection, setup and delivery activities are created through the journey.

The agenda shows a time gutter and events with title, interval, location, responsible person, show label, reminder and fulfilment links, on a selected date with neighbours available. Overlaps are shown together. Event detail adds the show reference and linked records. Actions: edit, open related records, delete. Editing an event changes Calendar only; Organizer reflects the new time on refresh and flags any violated requirement deadline.

Calendar publishes reading events by show and range and by reference, staging an event proposal, and reading its outcome with the confirmed details. Users can adjust a proposed event before adding it, and Organizer must compare the actual confirmed times with its plan, because an adjusted event may break a deadline. Confirming the same proposal returns the same event.

## Equipment Rental

Owns catalogue, availability, quotes and reservations. Everything is local and labelled as a demo transaction; there is no payment or supplier.

Catalogue: boom stand (adjustable, adapter fitting the venue clip) at €8, stock 2; XLR female to XLR male 10 m cable at €5, stock 3; a package of one stand and one 10 m cable at €12 that consumes that stock rather than extra inventory; a 3 m cable of the same connectors at €3, which fails the rider’s minimum length; a bass combo of at least 300 W with 6.35 mm input and balanced XLR DI output at €40, stock 1, whose specification both acts accept and which is the only alternative to the venue amplifier. Prices are per the Friday-to-Saturday period, with a €50 refundable deposit shown separately. Quotes last 15 minutes and hold no stock; collection only; exact items are reserved and substitutions need approval.

Users pick a rental period and browse by category, seeing specifications, availability for that period and price, then build a quote with quantities. Quote shows contents, period, collection location and hours, charges, deposit, total, expiry, availability and substitution terms, and can be edited, refreshed when expired or turned into a reservation. Reservations list as upcoming, active or closed, and show equipment, period, collector and status. Detail supports marking collection and return with item-level lists, so a partial collection stays visible, plus cancellation with confirmation. Cancelling releases the reserved stock and cancels nothing in other apps.

Rental publishes equipment search by specification, quantity and period; quote requests; staging a reservation proposal; reading its outcome with the reservation reference; and reading a shared reservation including item-level collection state. Search returns facts about equipment, never a claim that the rider is satisfied; Organizer compares the facts with its requirements.

## Pizza

Owns the menu, delivery options and orders. Local and labelled as a demo order.

Menu: Margherita 30 cm €10, vegetarian, milk and wheat; Garden Vegan 30 cm €12, vegan, wheat; Pepperoni 30 cm €13, meat, milk and wheat. Delivery €5 per order, so two of each is €70 of food and €75 in total. Dietary labels describe menu data and are not an allergy guarantee. Delivery goes to Static Bloom’s dressing room at the demo address, handled through the stage entrance, received by Morgan, whose contact details are clearly fictional.

Window 17:45–18:15 meets the 18:30 requirement; 18:15–18:45 cannot guarantee it; 18:45–19:15 is too late. Offers last 15 minutes and confirmation rechecks availability.

Users browse by dietary filter, see size, price, ingredients, allergens and quantity controls, and build a basket. The basket shows quantities, dietary counts, subtotal, delivery, total, address, recipient, instructions and available delivery windows, and compares the selection with the requirement. Order review shows contents, dietary and allergen information, price, window, destination and recipient, and revalidates products, prices and slots before confirming; confirmation returns the actual contents, price and window, and repeating it returns the same order.

Orders list as upcoming, completed or cancelled, with a detail screen showing contents, delivery details, links and history. Delivery status is advanced only through explicit demo controls, never presented as live tracking. Cancellation is allowed while confirmed and refused once preparing. “Delivered” is the provider’s state; Organizer records receipt and dietary checks itself, and cancelling cancels nothing in other apps.

Pizza publishes reading the menu and delivery options, staging an order proposal, reading its outcome with the order reference, and reading a shared order with its contents and status.

## Journey

Organizer’s overview surfaces the vocal shortage. The requirement shows microphone and input covered, stand and cable missing. The user selects those two and arranges fulfilment.

The request sheet states the goal, the deadline, Alex as responsible, and preserving existing commitments, and names Equipment Rental, Todo and Calendar. The disclosure summary says that selected requirements, specifications, show timing and capability descriptions go to the model; provider responses arrive later, when the checked workspace runs its capability calls.

The generated workspace compares options: the package covering stand, adapter and 10 m cable, the separate items, and the 3 m cable marked as insufficient. It shows contents, availability, quote and terms, and checks the chosen collection 15:00–15:45 with Alex, setup 16:15–16:45 with Sam, arrival by 16:15, and return Saturday 10:00–12:00 against soundcheck and desk hours, exposing anything unresolved instead of picking an impossible plan.

The user prepares the reservation and reviews it in Rental, where availability and price are revalidated and the demo reservation is confirmed. Only then are the pickup task, the setup task and the calendar activities prepared, each confirmed in its own app, with Organizer retrieving each outcome. No writes happen invisibly in a chain.

Declining the collection event on purpose leaves the reservation, the pickup task and the setup task intact and the scheduling unresolved; the workspace offers to prepare that event again from the confirmed terms. Reopening a confirmed proposal returns the existing record without duplicating anything.

The same flow covers pizza: goal, six pizzas with at least two vegan, delivery by 18:30, Morgan as recipient, and Pizza, Todo and Calendar access. The workspace composes the order, marks the too-late window, and prepares the order and the receiving task and delivery activity after Pizza confirms.

The amplifier conflict is explored with DEAL, which compares changed preparation windows with renting a compatible amplifier. Neither option silently changes an agreed rider; the scheduling alternative is agreed outside the demo and recorded natively, and the chosen resolution updates the allocation.

Finally the providers report what they know — tasks completed, items collected, delivery advanced through demo controls — and Organizer refreshes the links, then verifies the vocal setup, the pizza receipt and the amplifier resolution itself, which is the only thing that makes a requirement checked and ready.

## Constraints on those extensions

The current component pack — text, hero, button, column, card, spinner, time, integer and toggle — is the starting point, not the target. Reuse existing Deal UI facilities before adding language semantics, and keep Compose restricted to transient presentation state: focus, keyboard, picker visibility and scroll position. Committed values and workflow transitions stay in DEAL.

Generated content must be unable to launch arbitrary activities or invent a trusted review destination.

Fulfilment must outlive the generated session: creation is idempotent, each operation reports independently, and a provider confirming while Organizer loses its reply is an unknown outcome to be queried rather than retried blindly. The session request ceiling must accommodate repeated catalogue and outcome queries without breaking a usable workspace.

Across all five apps, scheduled times carry date, time and UTC offset with the Europe/Berlin identifier, intervals carry both ends, and money is whole cents with a currency code, preserved through value conversion. Providers return actual confirmed times and prices; Organizer rechecks them against deadlines and the budget.
