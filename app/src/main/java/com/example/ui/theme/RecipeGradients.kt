package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
// ScrapChef Recipe Hero Gradients
// ═══════════════════════════════════════════════════════════════════
// 7 deterministic palettes selected by hashing the recipe name.
// Same recipe always produces the same gradient (stable identity).
// Selection: hash(recipeName).ushr(1) % 7
// ═══════════════════════════════════════════════════════════════════

object RecipeGradients {

    data class Palette(
        val from: Color,
        val via: Color,
        val to: Color
    )

    private val palettes = listOf(
        Palette(Color(0xFFE8622C), Color(0xFFF4A261), Color(0xFFE9C46A)), // 0: burnt orange → golden
        Palette(Color(0xFFC1440E), Color(0xFFE8622C), Color(0xFFF4A261)), // 1: deep red-orange → warm
        Palette(Color(0xFF9D4E15), Color(0xFFE8622C), Color(0xFFF6BD60)), // 2: rich brown → amber
        Palette(Color(0xFFB5651D), Color(0xFFE8956A), Color(0xFFF4D58D)), // 3: warm earth → cream
        Palette(Color(0xFF7B2D1B), Color(0xFFC1440E), Color(0xFFE8622C)), // 4: dark mahogany → orange
        Palette(Color(0xFFD4621A), Color(0xFFEE9B5A), Color(0xFFF2CC8F)), // 5: pumpkin → sand
        Palette(Color(0xFFA23B0E), Color(0xFFE8622C), Color(0xFFFFB37C)), // 6: rust → peach
    )

    /**
     * Returns the gradient palette for the given recipe name.
     * Deterministic: the same name always maps to the same palette.
     */
    fun forRecipe(recipeName: String): Palette {
        val index = (recipeName.hashCode().ushr(1)) % palettes.size
        return palettes[index]
    }
}
