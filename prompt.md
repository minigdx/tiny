**Your Optimized Prompt:**

You are a Kotlin multiplatform Android developer expert.

Your task is to add Android support to the `tiny-engine` module of a game engine project. Follow these steps precisely and apply best practices for Kotlin Multiplatform and Android development.

### 🎯 Objectives:
Add Android compatibility to `tiny-engine` by introducing the necessary Android source set, platform-specific implementations, and build configuration.

---

### ✅ Step-by-Step Tasks:

1. **Configure Android Build:**
  - Apply the Android library plugin: `com.android.library`
  - Update `build.gradle.kts` in the `tiny-engine` module with appropriate `android {}` configuration and compile settings.

2. **Set Up `androidMain` Source Set:**
  - Create the `androidMain` source set in the correct KMP folder structure.
  - In this source set, implement the following components:
    - `TinyView`: a rendering surface for Android
    - `AndroidPlatform`: the Android-specific implementation of the platform interface
    - `TinyRenderer`: handles drawing on Android using the rendering backend you suggest
    - `AndroidInputHandler`: manage input from touch and key events. Take inspiration from:  
      https://github.com/minigdx/minigdx/blob/master/src/androidMain/kotlin/com/github/dwursteisen/minigdx/input/AndroidInputHandler.kt

3. **Package the Engine:**
  - Ensure the `tiny-engine` module can be built as an AAR (Android Archive) for distribution.

---

### 🛠️ Development Workflow:
- After **each major step**, run:
  ```bash
  make lintfix

Then commit with a clear and relevant commit message (e.g., “feat(android): add androidMain source set”).
•	At the end of your implementation, run:

./gradlew build



⸻

Constraints:
•	Use idiomatic Kotlin and KMP structure.
•	Keep Android-specific code in androidMain only.
•	Maintain modularity for future platform support.

⸻

🧠 Reminder:

Ensure your Android integration does not break the existing desktop or common builds.

⸻

Output:
Provide the updated files and commit messages in order. Describe briefly any design decisions made (e.g., rendering backend, input handling approach).

**Key Improvements:**
• Clear role and task assignment with context  
• Structured step-by-step instructions with URLs  
• Emphasis on best practices and separation of concerns  
• Included output expectations and quality checks

**Techniques Applied:**  
Role assignment, constraint-based planning, task decomposition, output formatting for developers.