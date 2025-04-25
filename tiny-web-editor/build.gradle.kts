import org.gradle.api.internal.artifacts.transform.UnzipTransform

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
}

configurations.create("tinyWebEditorEngine") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val tinyResources =
    configurations.create("tinyResources") {
        isCanBeResolved = true
        isCanBeConsumed = false

        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "zip")
        }
    }

dependencies {
    registerTransform(UnzipTransform::class) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "zip")
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "unzip")
    }

    commonTestImplementation(kotlin("test"))

    jsMainImplementation(libs.luak)
    jsMainImplementation(libs.kotlin.coroutines)
    jsMainImplementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.20")
        ?.because("https://youtrack.jetbrains.com/issue/KT-57235")
    jsMainImplementation(project(":tiny-engine"))

    add(
        tinyResources.name,
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyResources",
            ),
        ),
    )
}

val tinyWebEditor =
    tasks.register("tinyWebEditor", Zip::class) {
        val tinyResources =
            tinyResources.incoming.artifactView {
                attributes {
                    attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "unzip")
                }
            }.files

        group = "tiny"
        from(tasks.getByName("jsBrowserDistribution"), tinyResources)
        this.destinationDirectory.set(project.layout.buildDirectory.dir("tiny-dist"))
        this.archiveVersion.set("")
    }

artifacts {
    add("tinyWebEditorEngine", tinyWebEditor)
}
