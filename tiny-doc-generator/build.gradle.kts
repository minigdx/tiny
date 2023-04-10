plugins {
    id("com.github.minigdx.gradle.plugin.developer.mpp")
}

dependencies {
    jvmMainImplementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
    jvmMainImplementation(project(":tiny-doc-annotations"))
}
