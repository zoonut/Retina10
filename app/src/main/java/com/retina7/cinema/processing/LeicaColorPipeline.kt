package com.retina7.cinema.processing

import com.retina7.cinema.data.model.LeicaColorProfile
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Leica Color Science & Optical Vignette / Peripheral Gain Pipeline
 * Calibrated for Xiaomi 12S Ultra (Sony IMX989 1" Main + Sony IMX586 Ultra-Wide).
 */
class LeicaColorPipeline {

    private val hlgLut = FloatArray(256)
    private val authenticLut = FloatArray(256)
    private val vibrantLut = FloatArray(256)

    init {
        initLuts()
    }

    private fun initLuts() {
        // ITU-R BT.2100 / ARIB STD-B67 Hybrid Log-Gamma
        val a = 0.17883277f
        val b = 0.28466892f
        val c = 0.55991073f

        for (i in 0..255) {
            val e = i / 255.0f

            // HLG Curve
            val hlgVal = if (e <= 1.0f / 12.0f) {
                sqrt(3.0f * e)
            } else {
                a * ln((12.0f * e - b).toDouble()).toFloat() + c
            }
            hlgLut[i] = hlgVal.coerceIn(0.0f, 1.0f)

            // Leica Authentic: S-curve with smooth shadow roll-off & gentle highlight compression
            val sCurve = if (e < 0.5f) {
                2.0f * e * e
            } else {
                1.0f - ((-2.0f * e + 2.0f).pow(2)) / 2.0f
            }
            authenticLut[i] = (0.7f * sCurve + 0.3f * e.pow(0.95f)).coerceIn(0.0f, 1.0f)

            // Leica Vibrant: Lifted shadows and saturated midtones
            vibrantLut[i] = (e.pow(0.82f) * 1.05f).coerceIn(0.0f, 1.0f)
        }
    }

    /**
     * Calculates optical vignette & torch beam compensation multiplier
     * @param normalizedDist Distance from optical center (0.0 center to 1.0 corner)
     * @param vignetteStrength User vignette compensation [0.0..1.0]
     * @param torchFlattening Center torch hotspot flattening [0.0..1.0]
     */
    fun calculatePerimeterGain(
        normalizedDist: Float,
        vignetteStrength: Float,
        torchFlattening: Float
    ): Float {
        // Anti-vignette boost: increases gain towards corners
        val antiVignetteGain = 1.0f + (normalizedDist * normalizedDist * vignetteStrength * 0.85f)

        // Center hotspot suppressor: gently pulls down harsh center light under torch
        val centerHotspotDampener = if (torchFlattening > 0f) {
            val centerProximity = max(0f, 1.0f - normalizedDist * 1.4f)
            1.0f - (centerProximity * torchFlattening * 0.35f)
        } else {
            1.0f
        }

        return antiVignetteGain * centerHotspotDampener
    }

    /**
     * Map RGB pixel through Leica profile curve
     */
    fun applyProfile(
        r: Float,
        g: Float,
        b: Float,
        profile: LeicaColorProfile
    ): Triple<Float, Float, Float> {
        val clampR = r.coerceIn(0f, 1f)
        val clampG = g.coerceIn(0f, 1f)
        val clampB = b.coerceIn(0f, 1f)

        return when (profile) {
            LeicaColorProfile.LEICA_HLG_BT2020 -> {
                val idxR = (clampR * 255).toInt()
                val idxG = (clampG * 255).toInt()
                val idxB = (clampB * 255).toInt()
                Triple(hlgLut[idxR], hlgLut[idxG], hlgLut[idxB])
            }
            LeicaColorProfile.LEICA_AUTHENTIC -> {
                val idxR = (clampR * 255).toInt()
                val idxG = (clampG * 255).toInt()
                val idxB = (clampB * 255).toInt()
                Triple(authenticLut[idxR], authenticLut[idxG], authenticLut[idxB])
            }
            LeicaColorProfile.LEICA_VIBRANT -> {
                val idxR = (clampR * 255).toInt()
                val idxG = (clampG * 255).toInt()
                val idxB = (clampB * 255).toInt()
                Triple(vibrantLut[idxR], vibrantLut[idxG], vibrantLut[idxB])
            }
            LeicaColorProfile.LEICA_MONOCHROM -> {
                // ITU-R BT.709 luminance weights
                val gray = (0.2126f * clampR + 0.7152f * clampG + 0.0722f * clampB)
                Triple(gray, gray, gray)
            }
            LeicaColorProfile.LEICA_MONOCHROM_HC -> {
                val gray = (0.2126f * clampR + 0.7152f * clampG + 0.0722f * clampB)
                // High contrast S-curve
                val hc = if (gray < 0.5f) {
                    2.2f * gray * gray
                } else {
                    1f - 2.2f * (1f - gray) * (1f - gray)
                }.coerceIn(0f, 1f)
                Triple(hc, hc, hc)
            }
        }
    }

    /**
     * Smooth Low-Light Shadow Lift Curve
     * Lifts dark shadows in poorly lit environments without washing out blacks or clipping highlights.
     * @param value Normalized channel value [0.0..1.0]
     * @param liftAmount Amount of shadow lift [0.0..1.0]
     */
    fun applyLowLightShadowLift(value: Float, liftAmount: Float): Float {
        if (liftAmount <= 0.001f) return value.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        // Adaptive shadow curve: maximum lift applied in low-mids (0.05 to 0.35)
        val shadowWeight = (1.0f - v).coerceIn(0f, 1f)
        val boosted = v + (shadowWeight * shadowWeight * liftAmount * 0.45f)
        return boosted.coerceIn(0f, 1f)
    }

    /**
     * Map RGB pixel through Leica profile curve with optional low-light shadow lift
     */
    fun applyProfile(
        r: Float,
        g: Float,
        b: Float,
        profile: LeicaColorProfile,
        shadowLift: Float = 0f,
        evExposureGain: Float = 1.0f
    ): Triple<Float, Float, Float> {
        val base = applyProfile(r * evExposureGain, g * evExposureGain, b * evExposureGain, profile)
        if (shadowLift <= 0.001f) return base

        return Triple(
            applyLowLightShadowLift(base.first, shadowLift),
            applyLowLightShadowLift(base.second, shadowLift),
            applyLowLightShadowLift(base.third, shadowLift)
        )
    }
}
