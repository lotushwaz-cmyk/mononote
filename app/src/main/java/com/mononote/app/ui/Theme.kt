package com.mononote.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Mono {
    val Bg = Color(0xFFFFFFFF)
    val Ink = Color(0xFF111111)
    val Sub = Color(0xFF5E5E5E)
    val Muted = Color(0xFF8A8A8A)
    val Box = Color(0xFFE3E3E3)
    val Line = Color(0xFFECECEC)
    val Soft = Color(0xFFF2F2F2)
    val Sun = Color(0xFFC0392B)
    val CoverText = Color(0xFFF2F2F2)
    val CoverSub = Color(0xFFA8A8A8)
}

@Composable
fun MonoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Mono.Ink,
            onPrimary = Mono.Bg,
            secondary = Mono.Sub,
            background = Mono.Bg,
            surface = Mono.Bg,
            onBackground = Mono.Ink,
            onSurface = Mono.Ink,
            surfaceVariant = Mono.Soft,
            outline = Mono.Box,
        ),
        content = content,
    )
}
