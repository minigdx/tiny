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

    jsMainImplementation(project(":tiny-engine"))
    jsMainImplementation(npm("prismjs", "1.23.0"))
}

// FIXME: depends on the _boot.lua, ... from :tiny-engine instead of having a copy here.

configurations.create("tinyWebEditorEngine") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val tinyWebEditor = tasks.register("tinyWebEditor", Zip::class) {
    group = "tiny"
    from(tasks.getByName("jsBrowserDistribution"))
    this.destinationDirectory.set(project.buildDir.resolve("tiny-dist"))
}

artifacts {
    add("tinyWebEditorEngine", tinyWebEditor)
}
