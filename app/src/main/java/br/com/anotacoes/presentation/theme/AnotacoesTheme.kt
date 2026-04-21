package br.com.anotacoes.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import br.com.anotacoes.domain.model.AppTheme

// ─── VERDE (Green) — matches design-preview verde theme ───
private val VerdeLightColors = lightColorScheme(
    primary = Color(0xFF2D7A4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC0E8D0),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF35616F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFD9E0),
    onSecondaryContainer = Color(0xFF001F27),
    tertiary = Color(0xFFB8860B),
    onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFFFFF0C2),
    onTertiaryContainer = Color(0xFF3A2800),
    background = Color(0xFFF5F5F0),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFD4EBD9),
    onSurfaceVariant = Color(0xFF3F4F45),
    outline = Color(0xFF7A8A80),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
)

private val VerdeDarkColors = darkColorScheme(
    primary = Color(0xFF6FCF97),
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF1A4A30),
    onPrimaryContainer = Color(0xFF6FCF97),
    secondary = Color(0xFF82C4C8),
    onSecondary = Color(0xFF00252A),
    secondaryContainer = Color(0xFF1A3A3E),
    onSecondaryContainer = Color(0xFF82C4C8),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF1A1000),
    tertiaryContainer = Color(0xFF3A2A00),
    onTertiaryContainer = Color(0xFFFFD54F),
    background = Color(0xFF161A16),
    onBackground = Color(0xFFE0E8E2),
    surface = Color(0xFF1F2820),
    onSurface = Color(0xFFE0E8E2),
    surfaceVariant = Color(0xFF1A3A28),
    onSurfaceVariant = Color(0xFFBFD9CA),
    outline = Color(0xFF6A8A74),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF690005),
)

// ─── ROSA (Pink) — matches design-preview rosa theme ───
private val PinkRoseLightColors = lightColorScheme(
    primary = Color(0xFFC2185B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD6E9),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF7B1FA2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAD4F5),
    onSecondaryContainer = Color(0xFF2A003A),
    tertiary = Color(0xFFE91E8C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD6EC),
    onTertiaryContainer = Color(0xFF2C1020),
    background = Color(0xFFFDF6FA),
    onBackground = Color(0xFF2C1020),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C1020),
    surfaceVariant = Color(0xFFF5DAEF),
    onSurfaceVariant = Color(0xFF51414B),
    outline = Color(0xFFA07090),
    error = Color(0xFFB71C1C),
    onError = Color(0xFFFFFFFF),
)

private val PinkRoseDarkColors = darkColorScheme(
    primary = Color(0xFFF06292),
    onPrimary = Color(0xFF3A0020),
    primaryContainer = Color(0xFF5C0030),
    onPrimaryContainer = Color(0xFFF06292),
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF1A0030),
    secondaryContainer = Color(0xFF3A0050),
    onSecondaryContainer = Color(0xFFCE93D8),
    tertiary = Color(0xFFFF80AB),
    onTertiary = Color(0xFF2A0018),
    tertiaryContainer = Color(0xFF4A0030),
    onTertiaryContainer = Color(0xFFFF80AB),
    background = Color(0xFF1C1018),
    onBackground = Color(0xFFF0D8E8),
    surface = Color(0xFF2A1622),
    onSurface = Color(0xFFF0D8E8),
    surfaceVariant = Color(0xFF3A1E32),
    onSurfaceVariant = Color(0xFFD5C2CC),
    outline = Color(0xFF9A6878),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
)

// ─── VITÓRIA (Pastel) — matches design-preview vitoria theme ───
private val VitoriaLightColors = lightColorScheme(
    primary = Color(0xFFFFC8DD),
    onPrimary = Color(0xFF4A0020),
    primaryContainer = Color(0xFFFFAFCC),
    onPrimaryContainer = Color(0xFF3A0015),
    secondary = Color(0xFFCDB4DB),
    onSecondary = Color(0xFF2A0A3A),
    secondaryContainer = Color(0xFFEEE0F8),
    onSecondaryContainer = Color(0xFF2A0A3A),
    tertiary = Color(0xFFA2D2FF),
    onTertiary = Color(0xFF002A4A),
    tertiaryContainer = Color(0xFFDDEFFF),
    onTertiaryContainer = Color(0xFF002A4A),
    background = Color(0xFFFEF8FC),
    onBackground = Color(0xFF2C1820),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C1820),
    surfaceVariant = Color(0xFFBDE0FE),
    onSurfaceVariant = Color(0xFF4A3848),
    outline = Color(0xFFB89AA8),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
)

private val VitoriaDarkColors = darkColorScheme(
    primary = Color(0xFFFF8FAB),
    onPrimary = Color(0xFF3A0018),
    primaryContainer = Color(0xFF5C0030),
    onPrimaryContainer = Color(0xFFFF8FAB),
    secondary = Color(0xFFD4A8E8),
    onSecondary = Color(0xFF1A0030),
    secondaryContainer = Color(0xFF38124A),
    onSecondaryContainer = Color(0xFFD4A8E8),
    tertiary = Color(0xFF80C4FF),
    onTertiary = Color(0xFF001428),
    tertiaryContainer = Color(0xFF002444),
    onTertiaryContainer = Color(0xFF80C4FF),
    background = Color(0xFF180D14),
    onBackground = Color(0xFFF5DCEC),
    surface = Color(0xFF241220),
    onSurface = Color(0xFFF5DCEC),
    surfaceVariant = Color(0xFF1A2E42),
    onSurfaceVariant = Color(0xFFCFB8C8),
    outline = Color(0xFF9A6880),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
)

// ─── OCEANO (Ocean Blue) — matches design-preview oceano theme ───
private val OceanBlueLightColors = lightColorScheme(
    primary = Color(0xFF0054A6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E0F8),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF3D6B8A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC0D8EC),
    onSecondaryContainer = Color(0xFF001828),
    tertiary = Color(0xFF0077CC),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE8FF),
    onTertiaryContainer = Color(0xFF002D6B),
    background = Color(0xFFF4F8FD),
    onBackground = Color(0xFF0D1C2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1C2C),
    surfaceVariant = Color(0xFFC8DFFA),
    onSurfaceVariant = Color(0xFF3A4E5E),
    outline = Color(0xFF607890),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
)

private val OceanBlueDarkColors = darkColorScheme(
    primary = Color(0xFF90C4FF),
    onPrimary = Color(0xFF001840),
    primaryContainer = Color(0xFF00296B),
    onPrimaryContainer = Color(0xFF90C4FF),
    secondary = Color(0xFF8AB4CC),
    onSecondary = Color(0xFF001828),
    secondaryContainer = Color(0xFF103050),
    onSecondaryContainer = Color(0xFF8AB4CC),
    tertiary = Color(0xFF60B0FF),
    onTertiary = Color(0xFF001028),
    tertiaryContainer = Color(0xFF002050),
    onTertiaryContainer = Color(0xFF60B0FF),
    background = Color(0xFF0A1420),
    onBackground = Color(0xFFD8E8F4),
    surface = Color(0xFF121E2C),
    onSurface = Color(0xFFD8E8F4),
    surfaceVariant = Color(0xFF122038),
    onSurfaceVariant = Color(0xFFBBCEDE),
    outline = Color(0xFF5A7898),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF690005),
)

// ─── DEEP PURPLE — legacy theme ───
private val DeepPurpleLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
)

private val DeepPurpleDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

@Composable
fun AnotacoesTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDarkSystem = isSystemInDarkTheme()

    val colorScheme = when (appTheme) {
        AppTheme.SYSTEM -> if (isDarkSystem) VerdeDarkColors else VerdeLightColors
        AppTheme.VERDE_CLARO -> VerdeLightColors
        AppTheme.VERDE_ESCURO -> VerdeDarkColors
        AppTheme.ROSA_CLARO -> PinkRoseLightColors
        AppTheme.ROSA_ESCURO -> PinkRoseDarkColors
        AppTheme.VITORIA_CLARO -> VitoriaLightColors
        AppTheme.VITORIA_ESCURO -> VitoriaDarkColors
        AppTheme.OCEANO_CLARO -> OceanBlueLightColors
        AppTheme.OCEANO_ESCURO -> OceanBlueDarkColors
        AppTheme.ROXO_CLARO -> DeepPurpleLightColors
        AppTheme.ROXO_ESCURO -> DeepPurpleDarkColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}