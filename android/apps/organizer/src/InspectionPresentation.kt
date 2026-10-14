package dev.deal.apps.organizer

// Compatibility imports for the existing Organizer presentation. Observation decoding
// belongs to embedding and is shared with Calendar, not duplicated in each application.
internal fun activitySummary(event: org.json.JSONObject) = dev.deal.embedding.activitySummary(event)
internal fun readableLogDetail(detail: String) = dev.deal.embedding.readableLogDetail(detail)
internal fun dialogueRole(event: org.json.JSONObject) = dev.deal.embedding.dialogueRole(event)
