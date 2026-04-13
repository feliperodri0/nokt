package br.com.anotacoes.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.anotacoes.R
import br.com.anotacoes.domain.model.AppTheme
import kotlinx.coroutines.delay

@Composable
fun CustomSplashScreen(
    appTheme: AppTheme,
    onSplashFinished: () -> Unit
) {
    val backgroundColor = when (appTheme) {
        AppTheme.VERDE_CLARO, AppTheme.SYSTEM -> Color(0xFF2D7A4F)
        AppTheme.VERDE_ESCURO -> Color(0xFF1B4A30)
        AppTheme.ROSA_CLARO -> Color(0xFFC2185B)
        AppTheme.ROSA_ESCURO -> Color(0xFF88103F)
        AppTheme.VITORIA_CLARO -> Color(0xFFFF8FAB)
        AppTheme.VITORIA_ESCURO -> Color(0xFFB26477)
        AppTheme.OCEANO_CLARO -> Color(0xFF0054A6)
        AppTheme.OCEANO_ESCURO -> Color(0xFF003A75)
        AppTheme.ROXO_CLARO -> Color(0xFF6750A4)
        AppTheme.ROXO_ESCURO -> Color(0xFF4A3975)
    }

    LaunchedEffect(Unit) {
        delay(800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
    }
}