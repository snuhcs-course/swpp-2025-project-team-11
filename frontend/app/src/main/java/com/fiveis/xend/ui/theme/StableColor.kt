package com.fiveis.xend.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * Contact.id, Group.id로부터 deterministic 색 생성
 *
 * - SplitMix64 기반의 경량 PRNG로 id를 섞어서 0..1 난수 3개 생성
 *
 * Usage:
 *   val color = StableColor.forId(group.id) // 또는 contact.id
 *   val onColor = StableColor.onColor(color) // 텍스트나 아이콘 색, 현재는 미사용
 */
object StableColor {
    data class Spec(
        val hueRange: ClosedFloatingPointRange<Float>,
        val saturationRange: ClosedFloatingPointRange<Float>,
        val lightnessRangeLightTheme: ClosedFloatingPointRange<Float>,
        val lightnessRangeDarkTheme: ClosedFloatingPointRange<Float>,
        val minContrastOnSurface: Float = 2.6f
    )

    val GreenBluePurple = Spec(
        hueRange = 150f..300f,
        saturationRange = 0.48f..0.78f,
        lightnessRangeLightTheme = 0.46f..0.62f,
        lightnessRangeDarkTheme = 0.38f..0.55f
    )

    val widerHue = Spec(
        hueRange = 75f..300f,
        saturationRange = 0.40f..0.50f,
        lightnessRangeLightTheme = 0.46f..0.62f,
        lightnessRangeDarkTheme = 0.38f..0.55f
    )

    fun forId(id: Long, isDark: Boolean, spec: Spec = widerHue, surface: Color? = null): Color {
        try {
            val r1 = rnd01(id, 0)
            val r2 = rnd01(id, 1)
            val r3 = rnd01(id, 2)

            val hue = lerp(spec.hueRange.start, spec.hueRange.endInclusive, r1)
            val sat = lerp(spec.saturationRange.start, spec.saturationRange.endInclusive, r2)
            val lr = if (isDark) spec.lightnessRangeDarkTheme else spec.lightnessRangeLightTheme
            var light = lerp(lr.start, lr.endInclusive, r3)

            var c = Color.hsl(hue = hue, saturation = sat, lightness = light)

            // surface 필드값 입력 있을 시 대비 보정
            if (surface != null && contrastRatio(c, surface) < spec.minContrastOnSurface) {
                // 어두운 테마면 조금 밝게, 밝은 테마면 조금 어둡게
                light = adjustLightnessForContrast(
                    initial = light,
                    targetContrast = spec.minContrastOnSurface,
                    isDark = isDark,
                    surface = surface,
                    hue = hue,
                    sat = sat
                )
                c = Color.hsl(hue, sat, light)
            }

            return c
        } catch (e: Exception) {
            return Color.Gray
        }
    }

    /**
     * 시스템 다크테마 감지 + surface 전달 없음
     */
    @Composable
    fun forId(id: Long, spec: Spec = widerHue): Color {
        val isDark = false
        return remember(id, isDark) { forId(id, isDark, spec) }
    }

    fun onColor(bg: Color): Color = if (bg.luminance() > 0.5f) Color.Black else Color.White

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun contrastRatio(a: Color, b: Color): Float {
        val l1 = a.luminance()
        val l2 = b.luminance()
        val (hi, lo) = max(l1, l2) to min(l1, l2)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    // 대비를 만족할 때까지 lightness를 작은 단위씩 보정
    private fun adjustLightnessForContrast(
        initial: Float,
        targetContrast: Float,
        isDark: Boolean,
        surface: Color,
        hue: Float,
        sat: Float
    ): Float {
        var l = initial
        // 다크 테마면 밝게(+), 라이트 테마면 어둡게(-)
        val step = if (isDark) +0.01f else -0.01f
        repeat(12) { // 과도한 루프 방지
            val test = Color.hsl(hue, sat, l)
            if (contrastRatio(test, surface) >= targetContrast) return l
            l = (l + step).coerceIn(0.15f, 0.85f)
        }
        return l
    }

    private fun rnd01(id: Long, stream: Int): Float {
        val u = splitMix64(id.toULong() + stream.toULong())
        val asDouble = (u.toDouble() / 18446744073709551616.0)
        return asDouble.toFloat().coerceIn(0f, 0.99999994f)
    }

    private fun splitMix64(x: ULong): ULong {
        var z = x + 0x9E3779B97F4A7C15uL
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }
}
