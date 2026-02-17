# Tiny Engine — SFX Sound Synthesis Improvement Guide

This guide catalogs practical, easy-to-implement improvements to the sound synthesis pipeline.
Each improvement is rated by audible impact and implementation effort.
The focus is on "better retro sound" — not studio quality.

All changes target files under `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/`.

---

## Table of Contents

1. [Waveform Generation](#1-waveform-generation)
2. [Envelope (ADSR)](#2-envelope-adsr)
3. [Effects and Modulation](#3-effects-and-modulation)
4. [Mixing and Output](#4-mixing-and-output)
5. [DC Offset Handling](#5-dc-offset-handling)
6. [Harmonics Refinement](#6-harmonics-refinement)
7. [Implementation Priority Order](#7-implementation-priority-order)
8. [Testing Strategy](#8-testing-strategy)
9. [Backward Compatibility Notes](#9-backward-compatibility-notes)

---

## 1. Waveform Generation

### 1.1 Fix Broken Waveforms in `Instrument.kt`

| Impact | Effort |
|--------|--------|
| **HIGH** | **Easy** |

**What is wrong now:**

Three waveforms in `Instrument.generate()` are mathematically incorrect. This is the code path
used by `SoundManager.convert()` for all pre-rendered musical bars and sequences — the majority
of game audio.

- **SAW_TOOTH** (`Instrument.kt:122-125`): Computes `sin(TWO_PI * freq * time) * 2 - 1`.
  The `sin()` call already produces a value in `[-1, 1]`, so `* 2 - 1` maps it to `[-3, 1]`.
  This is a distorted, offset sine wave — not a sawtooth. The range exceeds `[-1, 1]`,
  causing downstream hard clipping.

- **TRIANGLE** (`Instrument.kt:97-99`): Uses `sin()` to compute a "phase" via
  `(sin(...) + 1.0) % 1.0`. The `sin()` output ranges `[-1, 1]`, so `+ 1.0` yields `[0, 2]`,
  and `% 1.0` folds that into a non-linear pattern. The result is a warped triangle with
  harmonic content that does not match a true triangle wave.

- **PULSE** (`Instrument.kt:112-119`): Uses a complex formula involving `abs()`, modular
  arithmetic on a `sin()` value, and a magic constant `128.0`. This produces an unpredictable
  waveform that does not resemble a pulse wave.

**What to change:**

Port the correct phase-based implementations from `Oscillator.kt` into `Instrument.generate()`.
Both classes should use the same algorithms:

```kotlin
// In Instrument.generate():

SAW_TOOTH -> {
    val phase = (harmonicFreq * time) % 1.0f
    (2.0f * phase) - 1.0f
}

TRIANGLE -> {
    val phase = (harmonicFreq * time) % 1.0f
    if (phase < 0.5f) {
        (4.0f * phase) - 1.0f
    } else {
        3.0f - (4.0f * phase)
    }
}

PULSE -> {
    val phase = (harmonicFreq * time) % 1.0f
    val dutyCycle = 0.25f
    if (phase < dutyCycle) 1.0f else -1.0f
}
```

**Why it helps:**

This is the single highest-impact fix. Every pre-rendered sound using SAW_TOOTH, TRIANGLE, or
PULSE is currently producing wrong timbres. The "obos" preset instrument uses SAW_TOOTH and will
sound correctly after this fix. The "clarinet" preset uses TRIANGLE and will also be corrected.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Instrument.kt` — `generate()` method, lines 95–125

---

### 1.2 Unify Waveform Generation Into a Shared Function

| Impact | Effort |
|--------|--------|
| **Medium** | **Easy** |

**What is wrong now:**

Two separate implementations exist: `Oscillator.emit()` and `Instrument.generate()`. They
diverge in correctness (as shown above) and in how modulations are applied. This duplication
invites future drift.

**What to change:**

Extract the core waveform math into a single top-level or companion function:

```kotlin
fun generateWaveform(waveType: WaveType, frequency: Float, time: Float): Float
```

`Oscillator.emit()` would convert its `progress: Sample` to `time` then call this.
`Instrument.generate()` would call it directly after applying modulations.
The NOISE waveform requires state (low-pass filter), so it would remain in the respective
classes but share the same algorithm shape.

**Why it helps:**

Prevents future bugs from divergent implementations. Makes testing simpler since there is one
source of truth.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Oscillator.kt`
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Instrument.kt`

---

### 1.3 Fix Wasteful Noise Generation in `Oscillator.kt`

| Impact | Effort |
|--------|--------|
| **Low** (performance) | **Easy** |

**What is wrong now:**

In `Oscillator.kt:93-94`, the NOISE branch creates a new `Random(seed)` instance for every
single sample:

```kotlin
val seed = (time * 12345.0f + progress * 67890.0f).toLong()
val white = Random(seed).nextFloat() * 2f - 1f
```

This allocates a new `Random` object 44,100 times per second per active noise voice.
Additionally, the deterministic seed produces periodic patterns rather than true white noise.

**What to change:**

Use a single `Random` instance stored as a class field (similar to `Instrument.kt:141`):

```kotlin
// Class field:
private val random = Random(42)

// In NOISE branch:
val white = random.nextFloat() * 2f - 1f
```

**Why it helps:**

Eliminates thousands of object allocations per second. The noise will also sound better because
sequential `Random` calls produce more uniformly distributed white noise than deterministic
re-seeding.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Oscillator.kt` — NOISE branch

---

## 2. Envelope (ADSR)

### 2.1 Add Exponential Curves to Attack and Decay

| Impact | Effort |
|--------|--------|
| **HIGH** | **Easy** |

**What is wrong now:**

Both `Envelop.kt` and `SoundManager.envelopeFilter()` use purely linear ramps. Linear attack
sounds like a volume slider being pushed up mechanically. Linear decay sounds unnatural because
human hearing perceives loudness logarithmically. This makes every instrument sound "digital"
and flat.

In `Envelop.noteOn()` (line 29):
```kotlin
progress.toFloat() / attack.toFloat()  // linear attack
```

In `SoundManager.envelopeFilter()` (line 236):
```kotlin
currentSample.toFloat() / attackSamples  // linear attack
```

**What to change:**

Apply a simple power curve. The cheapest approach is to square the linear progress:

```kotlin
// Attack (slow start, fast finish — good for pads):
val linearProgress = progress.toFloat() / attack.toFloat()
val attackValue = linearProgress * linearProgress  // x^2 curve

// Decay (fast start, slow tail — natural decay):
val linearDecay = decayProgress / decay.toFloat()
val decayValue = sustain + (1.0f - sustain) * (1.0f - linearDecay) * (1.0f - linearDecay)

// Release (fast drop, slow tail):
val releaseProgress = progress.toFloat() / release.toFloat()
val releaseValue = sustain * (1.0f - releaseProgress) * (1.0f - releaseProgress)
```

For a more precise approach using `exp()`:

```kotlin
val k = 5.0f // Controls curve steepness; 5 is a good starting point
val releaseValue = sustain * exp(-k * releaseProgress)
```

**Why it helps:**

This is the biggest perceived quality improvement after fixing waveforms. Exponential envelopes
make instruments sound organic and musical. Attack with a curve gives instruments a "bloom"
instead of a sharp ramp. Decay with exponential falloff matches how real instruments lose energy.
This single change transforms the engine from "chip-tune bleeps" to "retro but pleasant."

**Important:** Both `Envelop.kt` and `SoundManager.envelopeFilter()` must use the same curve
shape, or real-time and pre-rendered sounds will not match.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Envelop.kt` — `noteOn()` and `noteOff()`
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/SoundManager.kt` — `envelopeFilter()` (lines 218–251)

---

### 2.2 Smooth Note Transitions to Prevent Clicks

| Impact | Effort |
|--------|--------|
| **HIGH** | **Easy** |

**What is wrong now:**

When a note ends abruptly (zero release time, or the buffer boundary falls mid-waveform), the
sample value jumps discontinuously from a non-zero value to zero. This produces an audible click
or pop.

The pre-rendered path in `SoundManager.convert()` (line 210–211) performs additive mixing with
no crossfade:

```kotlin
result[i] = min(max(-1f, result[i] + buffer[index++]), 1f)
```

In the real-time path, `InstrumentPlayer.generate()` (line 134) returns the raw result with no
fade-out guard.

**What to change:**

Add a tiny fade-out ramp (2–4 ms = 88–176 samples at 44100 Hz) at the end of every note buffer.
This is distinct from the ADSR release — it is a safety fade to prevent clicks even when release
is set to 0:

```kotlin
// After the main sample generation loop in SoundManager.convert():
val fadeOutSamples = 88 // ~2ms at 44100 Hz
for (i in 0 until fadeOutSamples) {
    val fadeIndex = numberOfSamples - fadeOutSamples + i
    if (fadeIndex >= 0 && fadeIndex < buffer.size) {
        val fadeFactor = 1.0f - (i.toFloat() / fadeOutSamples.toFloat())
        buffer[fadeIndex] *= fadeFactor
    }
}
```

For the real-time path, enforce a minimum release duration of ~2ms in `Envelop`:

```kotlin
// In Envelop.noteOff():
val minReleaseSamples = 88 // ~2ms click prevention
val effectiveRelease = max(minReleaseSamples, release0.invoke())
```

**Why it helps:**

Eliminates pops and clicks that break immersion. Especially audible with percussion and short
staccato notes. This is a "table stakes" improvement that all synthesizers implement.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Envelop.kt` — `noteOff()` to enforce minimum release
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/SoundManager.kt` — `convert()`, add fade-out guard after buffer generation

---

## 3. Effects and Modulation

### 3.1 Add Tremolo Effect (Amplitude Modulation)

| Impact | Effort |
|--------|--------|
| **Medium** | **Easy** |

**What is wrong now:**

The engine supports Sweep (linear pitch change) and Vibrato (sinusoidal pitch modulation) in
`Effect.kt`, but has no amplitude modulation. Tremolo is the amplitude counterpart to vibrato
and is essential for many retro sounds (electric piano, guitar, organ).

**What to change:**

Add a `Tremolo` class in `Effect.kt`. Since tremolo modulates amplitude (not frequency), it
does not fit the existing `Modulation` interface. Add it as a separate effect:

```kotlin
@Serializable
class Tremolo(
    var frequency: Frequency = 0f,  // LFO rate in Hz (typically 2-10 Hz)
    var depth: Percent = 0f,        // 0.0 = no effect, 1.0 = full tremolo
) {
    var active: Boolean = false

    fun apply(time: Seconds, sample: Float): Float {
        if (!active || depth == 0f) return sample
        val lfo = (1.0f - depth) + depth * ((sin(TWO_PI * frequency * time) + 1.0f) * 0.5f)
        return sample * lfo
    }
}
```

Add a `tremolo` field to `Instrument` and apply it in `generate()` after computing the waveform.
In `InstrumentPlayer.generate()`, apply tremolo similarly after the harmonizer output.

**Why it helps:**

Adds an expressive dimension that is currently completely missing. Tremolo is one of the most
recognizable retro game audio effects. Low effort because it is a simple multiplication after
the existing waveform generation.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Effect.kt` — add `Tremolo` class
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Instrument.kt` — add `tremolo` field, apply in `generate()`
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/InstrumentPlayer.kt` — apply tremolo after harmonizer

---

### 3.2 Make Pulse Duty Cycle Configurable

| Impact | Effort |
|--------|--------|
| **Medium** | **Easy** |

**What is wrong now:**

The pulse wave duty cycle is hardcoded at 25% in `Oscillator.kt:74`:

```kotlin
val dutyCycle = 0.25f // 25% duty cycle
```

Pulse width is one of the most important timbral controls for retro synths. A 50% duty cycle is
a square wave; 25% gives a nasal quality; 12.5% gives a thin, reedy sound. Classic consoles
(NES, Game Boy) supported 12.5%, 25%, 50%, and 75% duty cycles.

**What to change:**

Add a `dutyCycle` parameter to `Instrument`:

```kotlin
// In Instrument.kt:
var dutyCycle: Percent = 0.5f  // Default to 50% (square wave equivalent)
```

Pass it through to both `Oscillator` and `Instrument.generate()`:

```kotlin
// Oscillator takes duty cycle via a lambda:
class Oscillator(
    val waveType0: () -> Instrument.WaveType,
    val dutyCycle0: () -> Float = { 0.5f },
) {
    // In PULSE branch:
    Instrument.WaveType.PULSE -> {
        val phase = (frequency * time) % 1.0f
        if (phase < dutyCycle0()) 1.0f else -1.0f
    }
}
```

Wire it in `InstrumentPlayer`:

```kotlin
private val oscillator = Oscillator(
    waveType0 = { instrument.wave },
    dutyCycle0 = { instrument.dutyCycle },
)
```

**Why it helps:**

Unlocks significant timbral variety from a single parameter change. Users can create thin, reedy
leads (12.5%), classic square basses (50%), or nasal chip sounds (25%). This is one of the
defining features of retro sound.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Instrument.kt` — add `dutyCycle` field
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Oscillator.kt` — accept duty cycle parameter
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/InstrumentPlayer.kt` — wire duty cycle

**Serialization note:** Since `Instrument` is `@Serializable`, adding a new field with a default
value is backward compatible. Existing serialized instruments will use the default.

---

## 4. Mixing and Output

### 4.1 Replace Hard Clipping With Soft Saturation

| Impact | Effort |
|--------|--------|
| **HIGH** | **Easy** |

**What is wrong now:**

Every output stage uses hard clipping: `max(-1f, min(1f, value))`. When multiple notes overlap
and the sum exceeds 1.0, the waveform is abruptly truncated. This introduces harsh harmonic
distortion. Locations:

- `SoundManager.kt:195` — per-sample in `convert()`: `buffer[i] = max(-1.0f, min(1.0f, sampleValue))`
- `SoundManager.kt:211` — additive mixing: `result[i] = min(max(-1f, result[i] + buffer[index++]), 1f)`
- `SoundManager.kt:130` — sequence mixing: `result[index] = max(-1f, min(1f, mixedSample))`

**What to change:**

Replace with a `tanh`-based soft saturation function. `tanh` naturally compresses values
approaching the limits while leaving quiet signals nearly unaffected:

```kotlin
// Add to SoundManager companion object or as a top-level function:
fun softClip(sample: Float): Float {
    return kotlin.math.tanh(sample * 1.5f) / kotlin.math.tanh(1.5f)
}
```

The division by `tanh(1.5f)` normalizes so that an input of 1.0 maps to approximately 1.0.

For a cheaper approximation (no transcendental functions):

```kotlin
fun softClip(sample: Float): Float {
    return when {
        sample > 1.0f -> 1.0f
        sample < -1.0f -> -1.0f
        sample > 0.5f -> 1.0f - (2.0f * (1.0f - sample) * (1.0f - sample)) / 3.0f
        sample < -0.5f -> -1.0f + (2.0f * (1.0f + sample) * (1.0f + sample)) / 3.0f
        else -> sample
    }
}
```

Apply this at every location that currently uses hard clipping.

**Why it helps:**

Soft saturation produces warm, pleasant distortion when signals exceed the dynamic range, instead
of harsh buzzing. Especially important when multiple instruments play simultaneously. The
improvement is immediately audible on chords and polyphonic passages.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/SoundManager.kt` — add `softClip()`, replace all hard clip sites

---

### 4.2 Normalize Harmonic Sum in `Harmonizer.kt`

| Impact | Effort |
|--------|--------|
| **Medium** | **Easy** |

**What is wrong now:**

`Harmonizer.generate()` sums harmonics weighted by their amplitudes but does not normalize the
result. Normalization happens externally in `SoundManager.convert()` (line 171):

```kotlin
val maxPossibleAmplitude = instrument.harmonics.sum()
val normalizationFactor = 1.0f / max(1.0f, maxPossibleAmplitude)
```

However, the real-time path through `InstrumentPlayer.generate()` does **not** normalize at all.
The harmonizer output is used raw (`InstrumentPlayer.kt:88-98`). An instrument with harmonics
summing above 1.0 (like "clarinet" which sums to ~2.1) will produce samples exceeding `[-1, 1]`
in the real-time path, relying entirely on downstream hard clipping.

**What to change:**

Move normalization into `Harmonizer.generate()` so both code paths benefit:

```kotlin
fun generate(
    note: Note,
    sample: Sample,
    generator: (Float, Sample) -> Float,
): Frequency {
    val harmonics = harmonics0.invoke()
    var sampleValue = 0f
    var totalAmplitude = 0f

    harmonics.forEachIndexed { index, relativeAmplitude ->
        val harmonicNumber = index + 1
        val harmonicFreq = note.frequency * harmonicNumber
        val value = generator.invoke(harmonicFreq, sample)
        sampleValue += relativeAmplitude * value
        totalAmplitude += relativeAmplitude
    }

    val normFactor = if (totalAmplitude > 1.0f) 1.0f / totalAmplitude else 1.0f
    return sampleValue * normFactor
}
```

Then remove the external normalization in `SoundManager.convert()` (lines 170–171 and 191).

**Why it helps:**

Fixes a correctness bug in the real-time path where instruments with rich harmonics (clarinet,
violon) clip. Also simplifies the code by having one normalization point.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Harmonizer.kt` — add normalization
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/SoundManager.kt` — remove external normalization

---

### 4.3 Make Master Volume Configurable

| Impact | Effort |
|--------|--------|
| **Low** | **Easy** |

**What is wrong now:**

Master volume is a compile-time constant at 0.5f (`SoundManager.kt:278`):

```kotlin
const val MASTER_VOLUME = 0.5f
```

All audio is immediately halved. Combined with normalization and envelope scaling, actual output
levels can be quite low. Users have no way to adjust this.

**What to change:**

Make it a mutable instance variable on `SoundManager`:

```kotlin
var masterVolume: Float = 0.5f
```

Keep the companion `const` as a default, and use the instance variable in all calculations.
This allows the Lua API to expose volume control later.

**Why it helps:**

Gives developers and players control over audio levels.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/SoundManager.kt`

---

## 5. DC Offset Handling

### 5.1 Add DC Offset Removal for Pulse and Noise Waves

| Impact | Effort |
|--------|--------|
| **Medium** | **Easy** |

**What is wrong now:**

Pulse waves with non-50% duty cycles have inherent DC offset. A 25% duty cycle pulse spends
25% of its time at +1 and 75% at -1, giving a DC offset of
`(0.25 * 1) + (0.75 * -1) = -0.5`. This DC bias wastes headroom and causes the waveform to be
asymmetric, which can produce bass buildup in downstream mixing.

The filtered noise generator also accumulates DC offset because the single-pole low-pass filter
does not reject DC.

**What to change:**

For pulse waves, subtract the DC offset analytically:

```kotlin
// In both Oscillator and Instrument PULSE branches:
val dutyCycle = 0.25f // or instrument.dutyCycle
val dcOffset = (2.0f * dutyCycle) - 1.0f
val raw = if (phase < dutyCycle) 1.0f else -1.0f
return raw - dcOffset
```

For noise, add a simple high-pass DC blocker after the low-pass filter:

```kotlin
// Class fields:
private var dcBlockerPrev: Float = 0f
private var dcBlockerOut: Float = 0f

// After low-pass filter:
val result = alpha * white + (1.0f - alpha) * lastOutput
lastOutput = result

// DC blocker (single-pole high-pass, ~20 Hz cutoff at 44100 Hz)
val dcAlpha = 0.997f
dcBlockerOut = dcAlpha * (dcBlockerOut + result - dcBlockerPrev)
dcBlockerPrev = result
return dcBlockerOut
```

**Why it helps:**

Removes inaudible but harmful DC bias that reduces dynamic range and can cause speaker "pumping"
in longer compositions. The noise DC blocker also prevents low-frequency drift that can make
filtered noise sound muddy.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Oscillator.kt` — PULSE and NOISE branches
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Instrument.kt` — PULSE and NOISE branches

---

## 6. Harmonics Refinement

### 6.1 Fix Harmonics Documentation

| Impact | Effort |
|--------|--------|
| **Low** | **Easy** |

**What is wrong now:**

The `Harmonizer.kt` KDoc (lines 15–16) states:

> Index 0 represents the first harmonic (2x fundamental), index 1 represents the second
> harmonic (3x fundamental)

But the code on line 46 computes `harmonicNumber = index + 1`, so index 0 produces
`fundamentalFreq * 1 = fundamentalFreq`. This is the fundamental, not the 2nd harmonic.

The preset instruments confirm this: all presets have their highest amplitude at index 0
(e.g., clarinet has `harmonics = floatArrayOf(1.1f, 0.3f, ...)`).

**What to change:**

Fix the KDoc comment:

```
Index 0 = fundamental (1x), index 1 = 2nd harmonic (2x), index 2 = 3rd harmonic (3x), etc.
```

This is documentation-only; the code behavior is correct.

**Files to modify:**
- `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/sound/Harmonizer.kt` — update KDoc

---

## 7. Implementation Priority Order

Ordered by "bang for buck" — highest impact with lowest effort first:

| Priority | Improvement | Impact | Effort | Section |
|----------|------------|--------|--------|---------|
| 1 | Fix broken waveforms in Instrument.kt | HIGH | Easy | 1.1 |
| 2 | Exponential ADSR curves | HIGH | Easy | 2.1 |
| 3 | Click prevention (minimum release) | HIGH | Easy | 2.2 |
| 4 | Soft saturation (replace hard clipping) | HIGH | Easy | 4.1 |
| 5 | Normalize harmonics in Harmonizer | Medium | Easy | 4.2 |
| 6 | DC offset removal for pulse/noise | Medium | Easy | 5.1 |
| 7 | Configurable pulse duty cycle | Medium | Easy | 3.2 |
| 8 | Add tremolo effect | Medium | Easy | 3.1 |
| 9 | Unify waveform implementations | Medium | Easy | 1.2 |
| 10 | Fix noise random allocation | Low | Easy | 1.3 |
| 11 | Make master volume configurable | Low | Easy | 4.3 |
| 12 | Fix harmonics documentation | Low | Easy | 6.1 |

---

## 8. Testing Strategy

Every improvement should be accompanied by tests. Key test patterns:

1. **Waveform fixes (1.1):** Add tests that verify `Instrument.generate()` produces output
   matching `Oscillator.emit()` for the same inputs. Test that SAW_TOOTH values stay in
   `[-1, 1]` and have the correct linear ramp shape.

2. **Envelope curves (2.1):** Add tests verifying that attack values follow a convex curve
   (below the linear ramp) and decay values follow a concave curve (above the linear ramp
   toward sustain).

3. **Click prevention (2.2):** Test with a zero-release instrument to verify the minimum
   fade-out still applies and the final samples approach zero.

4. **Soft saturation (4.1):** Test that `softClip(0.5f) ≈ 0.5f` (passthrough for quiet
   signals), `softClip(2.0f) < 1.0f` but `> 0.9f` (compressed), and
   `softClip(x) == -softClip(-x)` (symmetry).

5. **Consistency tests:** Generate the same note with both the `Instrument.generate()` path
   and the `Oscillator.emit()` path, assert the waveforms match within tolerance.

---

## 9. Backward Compatibility Notes

- **Serialization:** `Instrument` is `@Serializable`. Any new fields (`dutyCycle`, `tremolo`)
  must have default values matching current behavior. Existing `.sfx` files will deserialize
  correctly.

- **Audio output:** The preset instruments (clarinet, violon, obos, drum, custom1–4) will
  sound different after waveform fixes. This is intentional — they will sound **correct**.
  Existing games that rely on the current (wrong) timbre may perceive this as a change.
  Document in release notes.

- **Envelope changes:** Switching from linear to exponential curves changes the character of
  all existing sounds. This is universally an improvement, but should be noted. If backward
  compatibility is critical, consider adding a `curveType` enum to `Instrument` with `LINEAR`
  and `EXPONENTIAL` options, defaulting to `EXPONENTIAL` for new instruments.
