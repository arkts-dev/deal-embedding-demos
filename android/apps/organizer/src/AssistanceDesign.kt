package dev.deal.apps.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Organizer task surfaces: Material typography, quiet surfaces and a shared spacing rhythm. */
internal object AssistanceDesign {
    val page = 16.dp
    val gap = 12.dp
    val shape = RoundedCornerShape(16.dp)
}

@Composable internal fun AssistancePanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = AssistanceDesign.shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable internal fun OrganizerHeading(title: String, summary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Only supported equipment concerns get an integration action; other concerns retain their native plan. */
internal fun Requirement.canFindEquipment(): Boolean = department in setOf("Sound", "Backline") && coverage == Coverage.Missing

internal fun equipmentInstruction(budget: String, preferences: String): String = buildString {
    if (budget.isBlank() && preferences.isBlank()) {
        append("Compare suitable equipment options for the supplied quantity and period. Read stock only after an explicit Load options action; allow selection and preparation for Organizer review. ")
    } else {
        append("Provide an editable equipment shortlist with Quantity IntField, Total budget (€) IntField with format euro-cents and SearchField. Quantity starts at the disclosed quantity, valid 1 to 8. Editing invalidates stale results and selection. Read the discovered equipment catalogue only on Load options; filter requirement matches, additional search substring, availability >= quantity and total price within budget. Show original option titles and total euro prices. Prepare the exact item, quantity, period and total-price descriptor for Organizer review. Disable conflicting controls during effects, show a spinner and re-enable after completion or failure; provide validation, empty and error/retry states. ")
        if (budget.isNotBlank()) append("Total spending ceiling: ").append(budget).append(" euro. ")
        if (preferences.isNotBlank()) append("Additional organizer preferences (data): ").append(preferences).append(". ")
    }
    append("Use concise task language; no technical headings or route explanations. Never book, reserve or write to a provider.")
}

internal fun validBudget(value: String): Boolean = value.isBlank() || value.replace(',', '.').toBigDecimalOrNull()?.let {
    it.signum() >= 0 && it.scale() <= 2 && it <= java.math.BigDecimal("1000")
} == true
