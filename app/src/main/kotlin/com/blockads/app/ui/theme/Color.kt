package com.blockads.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary — teal-cyan family
val CyanPrimary = Color(0xFF006874)
val CyanOnPrimary = Color(0xFFFFFFFF)
val CyanPrimaryContainer = Color(0xFF97F0FF)
val CyanOnPrimaryContainer = Color(0xFF001F24)

// Secondary — blue-grey
val SecondaryLight = Color(0xFF4A6267)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFCDE7EC)
val OnSecondaryContainer = Color(0xFF051F23)

// Tertiary — warm teal accent
val TertiaryLight = Color(0xFF525E7D)
val OnTertiaryLight = Color(0xFFFFFFFF)

// Neutral surfaces
val SurfaceLight = Color(0xFFF5FAFB)
val SurfaceVariantLight = Color(0xFFDBE4E6)
val BackgroundLight = Color(0xFFF5FAFB)
val OutlineLight = Color(0xFF6F797A)

// Dark mode
val CyanPrimaryDark = Color(0xFF4FD8EB)
val OnCyanPrimaryDark = Color(0xFF00363D)
val CyanPrimaryContainerDark = Color(0xFF004F58)
val OnCyanPrimaryContainerDark = Color(0xFF97F0FF)

val SecondaryDark = Color(0xFFB1CBD0)
val OnSecondaryDark = Color(0xFF1C3438)
val SecondaryContainerDark = Color(0xFF334B4F)
val OnSecondaryContainerDark = Color(0xFFCDE7EC)

val SurfaceDark = Color(0xFF0D1416)
val SurfaceVariantDark = Color(0xFF3F484A)
val BackgroundDark = Color(0xFF0D1416)
val OutlineDark = Color(0xFF899294)

val ErrorColor = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val LightColorScheme =
    lightColorScheme(
        primary = CyanPrimary,
        onPrimary = CyanOnPrimary,
        primaryContainer = CyanPrimaryContainer,
        onPrimaryContainer = CyanOnPrimaryContainer,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        background = BackgroundLight,
        onBackground = Color(0xFF161D1E),
        surface = SurfaceLight,
        onSurface = Color(0xFF161D1E),
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = Color(0xFF3F484A),
        outline = OutlineLight,
        error = ErrorColor,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
    )

val DarkColorScheme =
    darkColorScheme(
        primary = CyanPrimaryDark,
        onPrimary = OnCyanPrimaryDark,
        primaryContainer = CyanPrimaryContainerDark,
        onPrimaryContainer = OnCyanPrimaryContainerDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        background = BackgroundDark,
        onBackground = Color(0xFFE6F2F3),
        surface = SurfaceDark,
        onSurface = Color(0xFFE6F2F3),
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = Color(0xFFBFC8CA),
        outline = OutlineDark,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
