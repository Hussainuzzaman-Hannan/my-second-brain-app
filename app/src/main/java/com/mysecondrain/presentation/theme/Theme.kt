package com.mysecondrain.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = Primary40,
    onPrimary            = Primary100,
    primaryContainer     = Primary90,
    onPrimaryContainer   = Primary10,
    secondary            = Secondary40,
    onSecondary          = Color.White,
    secondaryContainer   = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary             = Tertiary40,
    onTertiary           = Color.White,
    tertiaryContainer    = Tertiary90,
    onTertiaryContainer  = Tertiary10,
    error                = Error40,
    errorContainer       = Error90,
    onError              = Color.White,
    onErrorContainer     = Error10,
    background           = Neutral99,
    onBackground         = Neutral10,
    surface              = Neutral99,
    onSurface            = Neutral10,
    surfaceVariant       = NeutralVariant90,
    onSurfaceVariant     = NeutralVariant30,
    outline              = NeutralVariant50,
    outlineVariant       = NeutralVariant80,
    inverseSurface       = Neutral20,
    inverseOnSurface     = Neutral95,
    inversePrimary       = Primary80
)

private val DarkColorScheme = darkColorScheme(
    primary              = Primary80,
    onPrimary            = Primary20,
    primaryContainer     = Primary20,
    onPrimaryContainer   = Primary90,
    secondary            = Secondary80,
    onSecondary          = Secondary20,
    secondaryContainer   = Secondary20,
    onSecondaryContainer = Secondary90,
    tertiary             = Tertiary80,
    onTertiary           = Tertiary10,
    tertiaryContainer    = Tertiary10,
    onTertiaryContainer  = Tertiary90,
    error                = Error80,
    errorContainer       = Error10,
    onError              = Error10,
    onErrorContainer     = Error90,
    background           = Neutral10,
    onBackground         = Neutral90,
    surface              = Neutral10,
    onSurface            = Neutral90,
    surfaceVariant       = NeutralVariant30,
    onSurfaceVariant     = NeutralVariant80,
    outline              = NeutralVariant60,
    outlineVariant       = NeutralVariant40,
    inverseSurface       = Neutral90,
    inverseOnSurface     = Neutral20,
    inversePrimary       = Primary40
)

@Composable
fun MySecondBrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}