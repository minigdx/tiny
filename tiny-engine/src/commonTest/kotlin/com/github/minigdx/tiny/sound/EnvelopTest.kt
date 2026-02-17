package com.github.minigdx.tiny.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvelopTest {
    @Test
    fun noteOn_attack_phase_uses_quadratic_curve() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        // Quadratic: value = (progress/attack)^2
        assertEquals(0.0f, envelope.noteOn(0), 0.001f)
        // 25/100 = 0.25, 0.25^2 = 0.0625
        assertEquals(0.0625f, envelope.noteOn(25), 0.001f)
        // 50/100 = 0.5, 0.5^2 = 0.25
        assertEquals(0.25f, envelope.noteOn(50), 0.001f)
        // 100/100 = 1.0, 1.0^2 = 1.0
        assertEquals(1.0f, envelope.noteOn(100), 0.001f)
    }

    @Test
    fun noteOn_attack_is_below_linear() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        // Quadratic attack should be below the linear ramp at all midpoints
        for (progress in 1..99) {
            val actual = envelope.noteOn(progress)
            val linear = progress.toFloat() / 100f
            assertTrue(actual <= linear, "Quadratic attack should be <= linear at progress $progress")
        }
    }

    @Test
    fun noteOn_decay_phase_decreases_to_sustain() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        // At attack end: 1.0
        assertEquals(1.0f, envelope.noteOn(100), 0.001f)
        // Decay uses inverse-square: sustain + (1-sustain) * (1-linear)^2
        // At midpoint (25/50=0.5): 0.7 + 0.3 * (0.5)^2 = 0.7 + 0.075 = 0.775
        assertEquals(0.775f, envelope.noteOn(125), 0.001f)
        // At end (50/50=1.0): 0.7 + 0.3 * 0^2 = 0.7
        assertEquals(0.7f, envelope.noteOn(150), 0.001f)
    }

    @Test
    fun noteOn_sustain_phase_stays_constant() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(0.7f, envelope.noteOn(150), 0.001f)
        assertEquals(0.7f, envelope.noteOn(200), 0.001f)
        assertEquals(0.7f, envelope.noteOn(1000), 0.001f)
    }

    @Test
    fun noteOn_zero_attack_starts_at_full_volume() {
        val envelope = Envelop(attack0 = { 0 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(1.0f, envelope.noteOn(0), 0.001f)
    }

    @Test
    fun noteOn_zero_decay_jumps_to_sustain() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 0 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(1.0f, envelope.noteOn(100), 0.001f)
        assertEquals(0.7f, envelope.noteOn(101), 0.001f)
        assertEquals(0.7f, envelope.noteOn(200), 0.001f)
    }

    @Test
    fun noteOn_negative_progress_returns_zero() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(0.0f, envelope.noteOn(-10), 0.001f)
        assertEquals(0.0f, envelope.noteOn(-1), 0.001f)
    }

    @Test
    fun noteOff_release_phase_uses_quadratic_curve() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        // Quadratic release: sustain * (1 - progress/release)^2
        assertEquals(0.7f, envelope.noteOff(0), 0.001f)
        // At 50/200 = 0.25: 0.7 * (0.75)^2 = 0.7 * 0.5625 = 0.39375
        assertEquals(0.39375f, envelope.noteOff(50), 0.01f)
        // At 100/200 = 0.5: 0.7 * (0.5)^2 = 0.7 * 0.25 = 0.175
        assertEquals(0.175f, envelope.noteOff(100), 0.01f)
        // At 200/200 = 1.0: 0.7 * 0^2 = 0.0
        assertEquals(0.0f, envelope.noteOff(200), 0.001f)
    }

    @Test
    fun noteOff_after_release_stays_silent() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(0.0f, envelope.noteOff(200), 0.001f)
        assertEquals(0.0f, envelope.noteOff(250), 0.001f)
        assertEquals(0.0f, envelope.noteOff(1000), 0.001f)
    }

    @Test
    fun noteOff_zero_release_uses_minimum_release_for_click_prevention() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 0 })

        // With minimum release (88 samples), noteOff at 0 should still return sustain
        assertEquals(0.7f, envelope.noteOff(0), 0.001f)
        // Should reach 0 at the minimum release duration
        assertEquals(0.0f, envelope.noteOff(Envelop.MIN_RELEASE_SAMPLES), 0.001f)
    }

    @Test
    fun noteOff_negative_progress_returns_zero() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(0.0f, envelope.noteOff(-10), 0.001f)
        assertEquals(0.0f, envelope.noteOff(-1), 0.001f)
    }

    @Test
    fun envelope_with_full_sustain() {
        val envelope = Envelop(attack0 = { 50 }, decay0 = { 50 }, sustain0 = { 1.0f }, release0 = { 100 })

        assertEquals(0.0f, envelope.noteOn(0), 0.001f)
        assertEquals(1.0f, envelope.noteOn(50), 0.001f)
        assertEquals(1.0f, envelope.noteOn(100), 0.001f)
        assertEquals(1.0f, envelope.noteOn(200), 0.001f)

        // noteOff from sustain level (1.0) with quadratic release
        assertEquals(1.0f, envelope.noteOff(0), 0.001f)
        // At 50/100 = 0.5: 1.0 * (0.5)^2 = 0.25
        assertEquals(0.25f, envelope.noteOff(50), 0.001f)
        assertEquals(0.0f, envelope.noteOff(100), 0.001f)
    }

    @Test
    fun envelope_with_zero_sustain() {
        val envelope = Envelop(attack0 = { 50 }, decay0 = { 50 }, sustain0 = { 0.0f }, release0 = { 100 })

        assertEquals(0.0f, envelope.noteOn(0), 0.001f)
        assertEquals(1.0f, envelope.noteOn(50), 0.001f)
        assertEquals(0.0f, envelope.noteOn(100), 0.001f)
        assertEquals(0.0f, envelope.noteOn(200), 0.001f)

        // noteOff from sustain level (0.0) stays at 0
        assertEquals(0.0f, envelope.noteOff(0), 0.001f)
        assertEquals(0.0f, envelope.noteOff(50), 0.001f)
        assertEquals(0.0f, envelope.noteOff(100), 0.001f)
    }

    @Test
    fun noteOn_values_always_between_zero_and_one() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 100 }, sustain0 = { 0.5f }, release0 = { 100 })

        for (progress in 0..500) {
            val value = envelope.noteOn(progress)
            assertTrue(value >= 0.0f, "noteOn value should be >= 0 at progress $progress, got $value")
            assertTrue(value <= 1.0f, "noteOn value should be <= 1 at progress $progress, got $value")
        }
    }

    @Test
    fun noteOff_values_always_between_zero_and_sustain() {
        val sustain = 0.8f
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 100 }, sustain0 = { sustain }, release0 = { 200 })

        for (progress in 0..300) {
            val value = envelope.noteOff(progress)
            assertTrue(value >= 0.0f, "noteOff value should be >= 0 at progress $progress, got $value")
            assertTrue(value <= sustain + 0.001f, "noteOff value should be <= sustain at progress $progress, got $value")
        }
    }
}
