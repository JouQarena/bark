package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class KalbarqColorPalette(
    val id: String,
    val arabicName: String,
    val subtitle: String,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val outgoingBubbleDark: Color,
    val outgoingBubbleLight: Color,
    val storyRing: Color
)

val KalbarqThemePalettes = listOf(
    KalbarqColorPalette(
        id = "ROYAL_BLUE",
        arabicName = "أزرق كلبرق الملكي (الافتراضي)",
        subtitle = "الأزرق الكهربائي الفاخر الخاص بهوية كلبرق",
        primary = Color(0xFF0077FF),
        primaryDark = Color(0xFF0055D4),
        primaryLight = Color(0xFF00C6FF),
        outgoingBubbleDark = Color(0xFF0A3977),
        outgoingBubbleLight = Color(0xFFD6EAFF),
        storyRing = Color(0xFF00A6FF)
    ),
    KalbarqColorPalette(
        id = "KALBARQ_GOLD",
        arabicName = "ذهبي كلبرق الملكي (النسخة الذهبية)",
        subtitle = "مستوحى من واتساب الذهبي الفاخر",
        primary = Color(0xFFD4AF37),
        primaryDark = Color(0xFFB48811),
        primaryLight = Color(0xFFFBBF24),
        outgoingBubbleDark = Color(0xFF4A3708),
        outgoingBubbleLight = Color(0xFFFEF3C7),
        storyRing = Color(0xFFF59E0B)
    ),
    KalbarqColorPalette(
        id = "EMERALD_GREEN",
        arabicName = "الأخضر الزمردي الكلاسيكي",
        subtitle = "طابع المراسلة الكلاسيكي المريح للعين",
        primary = Color(0xFF00A884),
        primaryDark = Color(0xFF008069),
        primaryLight = Color(0xFF25D366),
        outgoingBubbleDark = Color(0xFF005C4B),
        outgoingBubbleLight = Color(0xFFD9FDD3),
        storyRing = Color(0xFF25D366)
    ),
    KalbarqColorPalette(
        id = "ROYAL_PURPLE",
        arabicName = "البنفسجي الملكي الفاخر",
        subtitle = "واجهة عصرية متألقة بدرجات الموف",
        primary = Color(0xFF7C3AED),
        primaryDark = Color(0xFF5B21B6),
        primaryLight = Color(0xFFA855F7),
        outgoingBubbleDark = Color(0xFF3B1A78),
        outgoingBubbleLight = Color(0xFFEDE9FE),
        storyRing = Color(0xFFA855F7)
    ),
    KalbarqColorPalette(
        id = "CRIMSON_RUBY",
        arabicName = "الياقوتي القرمزي",
        subtitle = "لون جريء وحيوي عالي التباين",
        primary = Color(0xFFE11D48),
        primaryDark = Color(0xFFBE123C),
        primaryLight = Color(0xFFFB7185),
        outgoingBubbleDark = Color(0xFF5B1025),
        outgoingBubbleLight = Color(0xFFFFE4E6),
        storyRing = Color(0xFFF43F5E)
    ),
    KalbarqColorPalette(
        id = "CYBER_CYAN",
        arabicName = "تركواز سايبر السماوي",
        subtitle = "تدرج فيروزي هادئ وعصري",
        primary = Color(0xFF0891B2),
        primaryDark = Color(0xFF0E7490),
        primaryLight = Color(0xFF22D3EE),
        outgoingBubbleDark = Color(0xFF0B4457),
        outgoingBubbleLight = Color(0xFFCFFAFE),
        storyRing = Color(0xFF06B6D4)
    )
)

// Dynamic reactive brand tokens so all screens update instantaneously when the user changes Kalbarq's UI color
var EmeraldPrimary by mutableStateOf(Color(0xFF0077FF))
var EmeraldDark by mutableStateOf(Color(0xFF0055D4))
var EmeraldLight by mutableStateOf(Color(0xFF00C6FF))
var OutgoingGreenDark by mutableStateOf(Color(0xFF0A3977))
var OutgoingGreenLight by mutableStateOf(Color(0xFFD6EAFF))
var StoryRingActive by mutableStateOf(Color(0xFF00A6FF))

fun applyKalbarqColorPalette(presetId: String): KalbarqColorPalette {
    val palette = KalbarqThemePalettes.find { it.id == presetId } ?: KalbarqThemePalettes.first()
    EmeraldPrimary = palette.primary
    EmeraldDark = palette.primaryDark
    EmeraldLight = palette.primaryLight
    OutgoingGreenDark = palette.outgoingBubbleDark
    OutgoingGreenLight = palette.outgoingBubbleLight
    StoryRingActive = palette.storyRing
    return palette
}

// Dark Theme Deep Midnight Navy & Sapphire
val DarkBackground = Color(0xFF071120)
val DarkSurface = Color(0xFF0C192E)
val DarkSurfaceElevated = Color(0xFF152642)
val DarkInputBar = Color(0xFF13233D)
val DarkBorder = Color(0xFF1E3456)
val DarkTextPrimary = Color(0xFFEFF6FF)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

// Light Theme Crisp Sky & Ice Blue
val LightBackground = Color(0xFFEAF2FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F6FD)
val LightBorder = Color(0xFFDCE7F5)
val LightTextPrimary = Color(0xFF0A192F)
val LightTextSecondary = Color(0xFF475569)

// Functional & Accents (Lightning Gold & Cyan)
val CheckmarkBlue = Color(0xFF38BDF8)
val SecurityShieldGold = Color(0xFFFFB800)
val OnlineGreenGlow = Color(0xFF10B981)
val ErrorRed = Color(0xFFF15C6D)
