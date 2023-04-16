plugins {
    id("com.github.minigdx.gradle.plugin.developer.mpp")
}
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.danielgergely.com/releases/")
    }
}
dependencies {
    this.commonTestImplementation(kotlin("test"))
    // jsMainImplementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")

    jsMainImplementation(project(":tiny-engine"))
    jsMainImplementation(npm("prismjs", "1.23.0 "))
}
