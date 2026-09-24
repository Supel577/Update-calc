package com.example.calcvault.ui.vault.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class VaultTheme(
    val id: String,
    val name: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color(0xFF94A3B8)
)

enum class WallpaperRenderType {
    NEON_CYBER,
    EMERALD_WAVES,
    MOUNTAIN_DUSK,
    COSMIC_NEBULA,
    STEALTH_SLATE,
    PURE_AMOLED,
    ATMOSPHERE_AZURE,
    CARBON_GRID,
    SONIC_PULSE,
    MATRIX_TERMINAL
}

data class PresetWallpaper(
    val id: String,
    val name: String,
    val drawableRes: Int,
    val description: String,
    val renderType: WallpaperRenderType
)

object VaultThemes {
    val presetWallpapers = listOf(
        PresetWallpaper("wp_neon_cyber", "Cyberpunk Neon", com.example.R.drawable.calc_wp_neon_cyber, "Deep blue & purple bokeh gradient", WallpaperRenderType.NEON_CYBER),
        PresetWallpaper("wp_emerald_waves", "Emerald Waves", com.example.R.drawable.calc_wp_emerald_waves, "Silky obsidian and mint fluid ripples", WallpaperRenderType.EMERALD_WAVES),
        PresetWallpaper("wp_mountain_dusk", "Mountain Dusk", com.example.R.drawable.calc_wp_mountain_dusk, "Moody mountain peaks under night sky", WallpaperRenderType.MOUNTAIN_DUSK),
        PresetWallpaper("wp_cosmic_nebula", "Cosmic Nebula", com.example.R.drawable.calc_wp_cosmic_nebula, "Deep space galaxy with stardust glow", WallpaperRenderType.COSMIC_NEBULA),
        PresetWallpaper("wp_stealth_slate", "Stealth Slate", com.example.R.drawable.calc_wp_stealth_slate_1789885790275, "Deep matte dark minimal pattern", WallpaperRenderType.STEALTH_SLATE),
        PresetWallpaper("wp_pure_amoled", "Pitch Dark", com.example.R.drawable.calc_wp_pure_amoled_1789885802904, "Battery-saving ultra pure black OLED", WallpaperRenderType.PURE_AMOLED),
        PresetWallpaper("wp_weather_blue", "Atmosphere Azure", com.example.R.drawable.calc_wp_atmosphere_azure_1789885815432, "Deep midnight atmospheric gradient", WallpaperRenderType.ATMOSPHERE_AZURE),
        PresetWallpaper("wp_carbon_notes", "Carbon Grid", com.example.R.drawable.calc_wp_carbon_grid_1789885830874, "Textured dark graphite grid", WallpaperRenderType.CARBON_GRID),
        PresetWallpaper("wp_music_frequency", "Sonic Pulse", com.example.R.drawable.calc_wp_sonic_pulse_1789885844771, "Dark violet sonic resonance wave", WallpaperRenderType.SONIC_PULSE),
        PresetWallpaper("wp_matrix_terminal", "Matrix Dark", com.example.R.drawable.calc_wp_matrix_dark_1789885862063, "Phosphor emerald digital stream", WallpaperRenderType.MATRIX_TERMINAL)
    )

    fun getWallpaperDrawableRes(pathOrId: String?): Int? {
        if (pathOrId.isNullOrEmpty()) return null
        val id = if (pathOrId.startsWith("preset:")) pathOrId.removePrefix("preset:") else pathOrId
        return presetWallpapers.find { it.id == id }?.drawableRes
    }

    fun getWallpaperPreset(pathOrId: String?): PresetWallpaper? {
        if (pathOrId.isNullOrEmpty()) return null
        val id = if (pathOrId.startsWith("preset:")) pathOrId.removePrefix("preset:") else pathOrId
        return presetWallpapers.find { it.id == id }
    }
    val MidnightStealth = VaultTheme(
        id = "midnight",
        name = "Midnight Stealth",
        description = "Deep slate blue with vibrant sky accents",
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        cardBackground = Color(0xFF1E293B),
        primaryAccent = Color(0xFF38BDF8),
        secondaryAccent = Color(0xFF0284C7)
    )

    val AmoledBlack = VaultTheme(
        id = "amoled_black",
        name = "AMOLED Pitch Black",
        description = "Pure black pixels for battery saving and high contrast",
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        cardBackground = Color(0xFF181818),
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF059669)
    )

    val CyberpunkViolet = VaultTheme(
        id = "cyberpunk",
        name = "Cyberpunk Violet",
        description = "Neon ultraviolet and electric fuchsia glow",
        background = Color(0xFF0D0B18),
        surface = Color(0xFF181429),
        cardBackground = Color(0xFF211B38),
        primaryAccent = Color(0xFFA855F7),
        secondaryAccent = Color(0xFFEC4899)
    )

    val EmeraldObsidian = VaultTheme(
        id = "emerald",
        name = "Emerald Obsidian",
        description = "Obsidian shadows paired with mint emerald",
        background = Color(0xFF06150F),
        surface = Color(0xFF0E241B),
        cardBackground = Color(0xFF133226),
        primaryAccent = Color(0xFF34D399),
        secondaryAccent = Color(0xFF059669)
    )

    val RubyCarbon = VaultTheme(
        id = "ruby_carbon",
        name = "Ruby Carbon",
        description = "Carbon fiber darks paired with ruby red",
        background = Color(0xFF120A0C),
        surface = Color(0xFF211317),
        cardBackground = Color(0xFF2C191E),
        primaryAccent = Color(0xFFF43F5E),
        secondaryAccent = Color(0xFFE11D48)
    )

    val DeepOcean = VaultTheme(
        id = "deep_ocean",
        name = "Deep Ocean Abyss",
        description = "Abyssal navy marine with cyan highlights",
        background = Color(0xFF08121E),
        surface = Color(0xFF102135),
        cardBackground = Color(0xFF162E49),
        primaryAccent = Color(0xFF06B6D4),
        secondaryAccent = Color(0xFF0284C7)
    )

    val AmberSunset = VaultTheme(
        id = "amber_sunset",
        name = "Sunset Amber",
        description = "Warm espresso base with glowing amber gold",
        background = Color(0xFF14100C),
        surface = Color(0xFF231B15),
        cardBackground = Color(0xFF30251C),
        primaryAccent = Color(0xFFF59E0B),
        secondaryAccent = Color(0xFFD97706)
    )

    val MonokaiPro = VaultTheme(
        id = "monokai_pro",
        name = "Monokai Dark",
        description = "Dark coffee tones and warm pastel highlights",
        background = Color(0xFF19181A),
        surface = Color(0xFF222024),
        cardBackground = Color(0xFF2D2A2E),
        primaryAccent = Color(0xFFFF6188),
        secondaryAccent = Color(0xFFFFD866)
    )

    val ArcticTitanium = VaultTheme(
        id = "arctic_titanium",
        name = "Arctic Titanium",
        description = "Industrial cold steel with glacier blue accents",
        background = Color(0xFF0F141C),
        surface = Color(0xFF1A2230),
        cardBackground = Color(0xFF222D3E),
        primaryAccent = Color(0xFF60A5FA),
        secondaryAccent = Color(0xFF3B82F6)
    )

    val RoseGoldLuxury = VaultTheme(
        id = "rose_gold",
        name = "Rose Gold Luxury",
        description = "Sophisticated dark velvet with soft rose gold",
        background = Color(0xFF151118),
        surface = Color(0xFF221A26),
        cardBackground = Color(0xFF2F2435),
        primaryAccent = Color(0xFFF472B6),
        secondaryAccent = Color(0xFFDB2777)
    )

    val TerminalMatrix = VaultTheme(
        id = "terminal_matrix",
        name = "Terminal Matrix",
        description = "True monochrome black with CRT phosphor green",
        background = Color(0xFF000000),
        surface = Color(0xFF061408),
        cardBackground = Color(0xFF0A220E),
        primaryAccent = Color(0xFF22C55E),
        secondaryAccent = Color(0xFF16A34A)
    )

    val SolarFlare = VaultTheme(
        id = "solar_flare",
        name = "Solar Flare",
        description = "Deep volcanic dusk with blazing orange flame",
        background = Color(0xFF110B07),
        surface = Color(0xFF20150E),
        cardBackground = Color(0xFF2D1E14),
        primaryAccent = Color(0xFFFB923C),
        secondaryAccent = Color(0xFFEA580C)
    )

    val allThemes = listOf(
        MidnightStealth,
        AmoledBlack,
        CyberpunkViolet,
        EmeraldObsidian,
        RubyCarbon,
        DeepOcean,
        AmberSunset,
        MonokaiPro,
        ArcticTitanium,
        RoseGoldLuxury,
        TerminalMatrix,
        SolarFlare
    )

    fun getThemeById(id: String): VaultTheme {
        return allThemes.find { it.id == id } ?: MidnightStealth
    }
}

@Composable
fun CodeBasedWallpaper(
    renderType: WallpaperRenderType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (renderType) {
            WallpaperRenderType.NEON_CYBER -> {
                drawRect(Color(0xFF090D16))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.5f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.2f),
                        radius = w * 0.75f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC026D3).copy(alpha = 0.45f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.8f),
                        radius = w * 0.8f
                    )
                )
            }
            WallpaperRenderType.EMERALD_WAVES -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF040D08)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(w, h)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.35f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
                        radius = w * 0.7f
                    )
                )
            }
            WallpaperRenderType.MOUNTAIN_DUSK -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF0F172A))
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF818CF8).copy(alpha = 0.3f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, 0f),
                        radius = w * 0.9f
                    )
                )
            }
            WallpaperRenderType.COSMIC_NEBULA -> {
                drawRect(Color(0xFF030014))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF9333EA).copy(alpha = 0.5f), Color(0xFF4C1D95).copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.85f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.3f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.2f),
                        radius = w * 0.5f
                    )
                )
            }
            WallpaperRenderType.STEALTH_SLATE -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(w, h)
                    )
                )
            }
            WallpaperRenderType.PURE_AMOLED -> {
                drawRect(Color(0xFF000000))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0284C7).copy(alpha = 0.12f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.6f
                    )
                )
            }
            WallpaperRenderType.ATMOSPHERE_AZURE -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF020617))
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.3f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.3f),
                        radius = w * 0.7f
                    )
                )
            }
            WallpaperRenderType.CARBON_GRID -> {
                drawRect(Color(0xFF18181B))
                val step = 28f
                var x = 0f
                while (x < w + h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, x),
                        strokeWidth = 1.2f
                    )
                    x += step
                }
            }
            WallpaperRenderType.SONIC_PULSE -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF3B0764), Color(0xFF1E1B4B), Color(0xFF09090B))
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFA855F7).copy(alpha = 0.35f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.7f),
                        radius = w * 0.8f
                    )
                )
            }
            WallpaperRenderType.MATRIX_TERMINAL -> {
                drawRect(Color(0xFF000000))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF22C55E).copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.35f),
                        radius = w * 0.75f
                    )
                )
                val scanStep = 8f
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = Color(0xFF22C55E).copy(alpha = 0.035f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += scanStep
                }
            }
        }
    }
}
