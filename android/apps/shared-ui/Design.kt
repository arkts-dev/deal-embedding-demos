package dev.deal.shell

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun AppTheme(accent: Color, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = accent, background = Color(0xff111519), surface = Color(0xff191e23), surfaceContainer = Color(0xff242b31))
    else lightColorScheme(primary = accent, background = Color(0xfff6f5f1), surface = Color.White, surfaceContainer = Color(0xffebece6), primaryContainer = accent.copy(alpha = .13f), onPrimaryContainer = Color(0xff172f2c))
    MaterialExpressiveTheme(colorScheme = scheme, content = content)
}

@Composable fun Page(eyebrow: String, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.displayMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable fun Consent(allowed: Boolean, onChange: (Boolean) -> Unit) {
    Panel {
        Text("Connected to Calendar", style = MaterialTheme.typography.titleLarge)
        Text("You control access. Revoking also cancels pending requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (allowed) "Sharing enabled" else "Private by default", modifier = Modifier.padding(top = 12.dp))
            Switch(checked = allowed, onCheckedChange = onChange)
        }
    }
}
