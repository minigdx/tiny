package com.github.minigdx.tiny.sound

import kotlin.test.Test
import kotlin.test.assertEquals

class EnvelopTest {
    @Test
    fun noteOn_attack_phase_increases_linearly() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(0.0f, envelope.noteOn(0), 0.001f)
        assertEquals(0.25f, envelope.noteOn(25), 0.001f)
        assertEquals(0.5f, envelope.noteOn(50), 0.001f)
        assertEquals(1.0f, envelope.noteOn(100), 0.001f)
    }

    @Test
    fun noteOn_decay_phase_decreases_to_sustain() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        assertEquals(1.0f, envelope.noteOn(100), 0.001f)
        assertEquals(0.85f, envelope.noteOn(125), 0.001f)
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
        assertEquals(0.85f, envelope.noteOn(25), 0.001f)
        assertEquals(0.7f, envelope.noteOn(50), 0.001f)
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
    fun noteOff_release_phase_decreases_to_zero_from_sustain() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 200 })

        // noteOff now only handles the release phase from sustain level
        assertEquals(0.7f, envelope.noteOff(0), 0.001f)
        assertEquals(0.525f, envelope.noteOff(50), 0.001f)
        assertEquals(0.35f, envelope.noteOff(100), 0.001f)
        assertEquals(0.175f, envelope.noteOff(150), 0.001f)
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
    fun noteOff_zero_release_immediately_silent() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 50 }, sustain0 = { 0.7f }, release0 = { 0 })

        assertEquals(0.0f, envelope.noteOff(0), 0.001f)
        assertEquals(0.0f, envelope.noteOff(1), 0.001f)
        assertEquals(0.0f, envelope.noteOff(100), 0.001f)
    }

    @Test
    fun noteOff_from_sustain_level() {
        val envelope = Envelop(attack0 = { 100 }, decay0 = { 0 }, sustain0 = { 0.7f }, release0 = { 200 })

        // noteOff starts from sustain level and decreases linearly over release time
        assertEquals(0.7f, envelope.noteOff(0), 0.001f)
        assertEquals(0.525f, envelope.noteOff(50), 0.001f)
        assertEquals(0.35f, envelope.noteOff(100), 0.001f)
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

        // noteOff from sustain level (1.0) over release time
        assertEquals(1.0f, envelope.noteOff(0), 0.001f)
        assertEquals(0.5f, envelope.noteOff(50), 0.001f)
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
}
