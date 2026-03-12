import org.asciidoctor.gradle.jvm.AsciidoctorTask

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("io.pebbletemplates:pebble:3.2.3")
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.asciidoctorj)
    alias(libs.plugins.minigdx.developer)
}

val asciidoctorResources by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val asciidoctorDependencies by configurations.creating {
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
        asciidoctorDependencies.name,
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyApiAsciidoctor",
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
}

val unzipAsciidoctorResources =
    tasks.maybeCreate("unzip-asciidoctorResources", Copy::class).also { cp ->
        asciidoctorResources.resolvedConfiguration.resolvedArtifacts.forEach {
            cp.from(zipTree(asciidoctorResources.incoming.artifacts.artifactFiles.files.first()))
        }
        cp.into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc"))
    }

val copyAsciidoctorDependencies =
    tasks.maybeCreate("copy-asciidoctorDependencies", Copy::class).also { cp ->
        // I'm bit lazy, I copy the result straight into the source directory :grimace:
        cp.into(project.projectDir.resolve("src/docs/asciidoc/dependencies"))
        cp.from(asciidoctorDependencies)
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
        include("api.html")
    }
    into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc"))
}

val renderPebbleTemplates = tasks.register("renderPebbleTemplates") {
    val templateDir = project.projectDir.resolve("src/docs/templates")
    val dataDir = project.projectDir.resolve("src/docs/asciidoc")
    val outputDir = project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc")

    inputs.dir(templateDir)
    inputs.files(
        dataDir.resolve("showcase.json"),
        dataDir.resolve("tutorials.json"),
        dataDir.resolve("document.json"),
    )
    outputs.files(
        outputDir.resolve("index.html"),
        outputDir.resolve("showcase.html"),
        outputDir.resolve("documentation.html"),
        outputDir.resolve("api.html"),
    )

    doLast {
        val slurper = groovy.json.JsonSlurper()

        @Suppress("UNCHECKED_CAST")
        val games = slurper.parseText(dataDir.resolve("showcase.json").readText()) as List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val tutorials = slurper.parseText(dataDir.resolve("tutorials.json").readText()) as List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val functions = slurper.parseText(dataDir.resolve("document.json").readText()) as List<Map<String, Any>>

        // Extract unique genres from showcase data
        val genreSet = mutableSetOf<String>()
        games.forEach { game ->
            @Suppress("UNCHECKED_CAST")
            val genres = game["genres"] as List<String>
            genres.forEach { genreSet.add(it) }
        }
        val genres = genreSet.sorted()

        // Extract unique libraries from function data
        val libSet = mutableSetOf<String>()
        functions.forEach { fn ->
            libSet.add(fn["library"] as String)
        }
        val libraries = libSet.sorted()

        // Raw JSON for embedding in documentation page
        val functionsJson = dataDir.resolve("document.json").readText()

        // Set up Pebble template engine
        val loader = io.pebbletemplates.pebble.loader.FileLoader()
        loader.setPrefix(templateDir.absolutePath + "/")
        loader.setSuffix("")
        val engine = io.pebbletemplates.pebble.PebbleEngine.Builder()
            .loader(loader)
            .autoEscaping(true)
            .build()

        outputDir.mkdirs()

        // Render index.html (no JSON data needed)
        val indexTemplate = engine.getTemplate("pages/index.peb")
        java.io.File(outputDir, "index.html").writer().use { writer ->
            indexTemplate.evaluate(writer, mapOf<String, Any>())
        }

        // Render showcase.html
        val showcaseTemplate = engine.getTemplate("pages/showcase.peb")
        java.io.File(outputDir, "showcase.html").writer().use { writer ->
            showcaseTemplate.evaluate(
                writer,
                mapOf<String, Any>(
                    "games" to games,
                    "genres" to genres,
                    "initialCount" to 6,
                ),
            )
        }

        // Render documentation.html
        val docTemplate = engine.getTemplate("pages/documentation.peb")
        java.io.File(outputDir, "documentation.html").writer().use { writer ->
            docTemplate.evaluate(
                writer,
                mapOf<String, Any>(
                    "tutorials" to tutorials,
                    "functions" to functions,
                    "functionsJson" to functionsJson,
                ),
            )
        }

        // Render api.html
        val apiTemplate = engine.getTemplate("pages/api.peb")
        java.io.File(outputDir, "api.html").writer().use { writer ->
            apiTemplate.evaluate(
                writer,
                mapOf<String, Any>(
                    "functions" to functions,
                    "libraries" to libraries,
                ),
            )
        }

        logger.lifecycle("Rendered 4 Pebble templates to ${outputDir.absolutePath}")
    }
}

tasks.withType(AsciidoctorTask::class.java).configureEach {
    this.baseDirFollowsSourceDir()
    this.notCompatibleWithConfigurationCache("AsciidoctorJ plugin is not compatible with configuration cache")

    this.dependsOn(
        unzipAsciidoctorResources.dependsOn(":tiny-web-editor:tinyWebEditor"),
        copyAsciidoctorDependencies,
        copyJsonApi,
        copySample,
        copyResources,
        copySeoFiles,
        renderPebbleTemplates,
    )
}
