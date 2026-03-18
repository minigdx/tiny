import org.asciidoctor.gradle.jvm.AsciidoctorTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.asciidoctorj)
    alias(libs.plugins.minigdx.jvm)
}

val asciidoctorResources by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val jsonApiDependencies by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(
        asciidoctorResources.name,
        project(
            mapOf(
                "path" to ":tiny-web-editor",
                "configuration" to "tinyWebEditorEngine",
            ),
        ),
    )

    add(
        jsonApiDependencies.name,
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyApiJson",
            ),
        ),
    )

    implementation(libs.pebble)
    implementation(libs.kotlin.serialization.json)
}

val unzipAsciidoctorResources =
    tasks.maybeCreate("unzip-asciidoctorResources", Copy::class).also { cp ->
        asciidoctorResources.resolvedConfiguration.resolvedArtifacts.forEach {
            cp.from(zipTree(asciidoctorResources.incoming.artifacts.artifactFiles.files.first()))
        }
        cp.into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc"))
    }

val copySample =
    tasks.register("copy-sample", Copy::class) {
        from(project.projectDir.resolve("src/docs/asciidoc/sample"))
        into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc/sample"))
    }

val copyResources =
    tasks.register("copy-resources", Copy::class) {
        from(project.projectDir.resolve("src/docs/asciidoc/resources"))
        into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc/resources"))
    }

val copyJsonApi =
    tasks.maybeCreate("copy-jsonApiDependencies", Copy::class).also { cp ->
        cp.into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc"))
        cp.from(jsonApiDependencies)
    }

val copySeoFiles = tasks.register("copy-seoFiles", Copy::class) {
    from(project.projectDir.resolve("src/docs/asciidoc")) {
        include("robots.txt")
        include("sitemap.xml")
        include(".nojekyll")
        include("tiny-nav.js")
        include("tiny-common.css")
        include("showcase.json")
        include("tutorials.json")
        include("document.json")
    }
    into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc"))
}

val renderPebbleTemplates = tasks.register("renderPebbleTemplates", JavaExec::class) {
    val templateDir = project.projectDir.resolve("src/docs/templates")
    val dataDir = project.projectDir.resolve("src/docs/asciidoc")
    val outputDir = project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc")
    val apiJsonFile = project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc/tiny-api.json")

    dependsOn(
        tasks.named("classes"),
        copyJsonApi,
        copySeoFiles,
        unzipAsciidoctorResources,
        copySample,
        copyResources,
    )

    classpath = project.the<SourceSetContainer>().getByName("main").runtimeClasspath
    mainClass.set("com.github.minigdx.tiny.doc.PebbleRendererKt")
    args = listOf(
        templateDir.absolutePath,
        dataDir.absolutePath,
        outputDir.absolutePath,
        apiJsonFile.absolutePath,
    )

    inputs.dir(templateDir)
    inputs.files(
        dataDir.resolve("showcase.json"),
        dataDir.resolve("tutorials.json"),
        dataDir.resolve("document.json"),
    )
    inputs.files(apiJsonFile)
    outputs.files(
        outputDir.resolve("index.html"),
        outputDir.resolve("showcase.html"),
        outputDir.resolve("documentation.html"),
        outputDir.resolve("api.html"),
    )
}

tasks.withType(AsciidoctorTask::class.java).configureEach {
    this.baseDirFollowsSourceDir()
    this.notCompatibleWithConfigurationCache("AsciidoctorJ plugin is not compatible with configuration cache")

    this.dependsOn(
        unzipAsciidoctorResources.dependsOn(":tiny-web-editor:tinyWebEditor"),
        copyJsonApi,
        copySample,
        copyResources,
        copySeoFiles,
        renderPebbleTemplates,
    )
}
