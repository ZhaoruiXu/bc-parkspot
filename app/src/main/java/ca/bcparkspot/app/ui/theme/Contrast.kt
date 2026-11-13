package ca.bcparkspot.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG 2.x minimums. */
object ContrastTargets {
    const val TEXT = 4.5
    const val NON_TEXT = 3.0
}

private const val SRGB_LINEAR_THRESHOLD = 0.03928
private const val SRGB_LINEAR_DIVISOR = 12.92
private const val SRGB_OFFSET = 0.055
private const val SRGB_SCALE = 1.055
private const val SRGB_GAMMA = 2.4
private const val LUMA_R = 0.2126
private const val LUMA_G = 0.7152
private const val LUMA_B = 0.0722
private const val FLARE = 0.05

private fun channel(c: Float): Double {
    val v = c.toDouble()
    return if (v <= SRGB_LINEAR_THRESHOLD) v / SRGB_LINEAR_DIVISOR else ((v + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_GAMMA)
}

fun relativeLuminance(color: Color): Double =
    LUMA_R * channel(color.red) + LUMA_G * channel(color.green) + LUMA_B * channel(color.blue)

fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + FLARE) / (minOf(la, lb) + FLARE)
}
