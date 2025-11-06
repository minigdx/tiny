package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParameters.Companion.JSON
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.resources.ldtk.Ldtk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportCommand : CliktCommand(name = "export") {
    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val platform by option("-p", "--platform", help = "Target platform")
        .choice("web", "desktop", "all")
        .default("web")

    // Web-specific options
    val archive by option(help = "The name of the exported archive (web only)")
        .default("tiny-export.zip")

    // Desktop-specific options
    val outputDirectory by option(
        "-o",
        "--output",
        help = "Output directory for the exported application (desktop only)",
    )
        .file(canBeDir = true, canBeFile = false)
        .default(File("exported-game"))

    val desktopPlatform by option("--desktop-platform", help = "Target desktop platform")
        .choice("windows", "linux", "mac", "current")
        .default("current")

    val includeJdk by option("--include-jdk", help = "Include JDK in the package (requires jpackage, desktop only)")
        .flag("--exclude-jdk", default = true)

    val appName by option("-n", "--name", help = "Application name (defaults to game name, desktop only)")

    val appVersion by option("-v", "--version", help = "Application version (desktop only)")
        .default("1.0.0")

    val debug by option("--debug", help = "Debug the game (desktop only)")
        .flag(default = false)

    private val gameExporter = GameExporter()

    override fun help(context: Context) = "Export your game for web or desktop platforms."

    override fun run() {
        echo("\uD83D\uDC77 Export ${gameDirectory.absolutePath}")

        when (platform) {
            "web" -> exportWeb()
            "desktop" -> exportDesktop()
            "all" -> {
                exportWeb()
                exportDesktop()
            }
        }
    }

    private fun exportWeb() {
        echo("\uD83C\uDF0D Exporting for web platform...")
        gameExporter.export(gameDirectory, archive)
        echo("\uD83C\uDF89 Web export completed! Your game has been exported in the $archive file.")
    }

    private fun exportDesktop() {
        echo("\uD83D\uDDA5\uFE0F Exporting for desktop platform...")

        val configFile = gameDirectory.resolve("_tiny.json")
        val gameParameters = Json.decodeFromStream<GameParameters>(FileInputStream(configFile))
        val finalAppName = appName ?: gameParameters.name

        if (includeJdk && !isJpackageAvailable()) {
            echo("\uD83D\uDE31 jpackage is not available. Please use Java 14 or later with jpackage support.")
            echo("\uD83D\uDCA1 Alternatively, use --exclude-jdk to create a portable JAR launcher.")
            return
        }

        val targetPlatform = when (desktopPlatform) {
            "current" -> detectCurrentPlatform()
            else -> desktopPlatform
        }

        outputDirectory.mkdirs()

        if (includeJdk) {
            createStandaloneAppWithJdk(
                gameDir = gameDirectory,
                outputDir = outputDirectory,
                appName = finalAppName,
                appVersion = appVersion,
                platform = targetPlatform,
                debug = debug,
            )
        } else {
            createPortableJarLauncher(
                gameDir = gameDirectory,
                outputDir = outputDirectory,
                appName = finalAppName,
            )
        }

        echo("\uD83C\uDF89 Desktop export completed! Your desktop application has been exported to $outputDirectory")
    }

    private fun isJpackageAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("jpackage", "--version").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun detectCurrentPlatform(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "windows"
            os.contains("mac") || os.contains("darwin") -> "mac"
            else -> "linux"
        }
    }

    private fun createStandaloneAppWithJdk(
        gameDir: File,
        outputDir: File,
        appName: String,
        appVersion: String,
        platform: String,
        debug: Boolean = false,
    ) {
        echo("\uD83D\uDCE6 Creating standalone application with bundled JDK...")

        val tempDir = Files.createTempDirectory("tiny-export").toFile()
        val appJarFile = File(tempDir, "$appName.jar")

        createLauncherJar(gameDir, outputDir, appJarFile)

        val jpackageCommand = mutableListOf(
            "jpackage",
            "--type", getPackageType(platform),
            "--input", tempDir.absolutePath,
            "--dest", outputDir.absolutePath,
            "--name", appName,
            "--app-version", appVersion,
            "--main-jar", appJarFile.name,
            "--main-class", "com.github.minigdx.tiny.cli.MainKt",
        )

        // Add platform-specific arguments for game location
        when (platform) {
            "mac" -> {
                // For macOS, the application is installed in /Applications/[AppName].app/
                // The executable is in Contents/MacOS/ and we want to access the game directory in Contents/Resources/game
                jpackageCommand.addAll(
                    listOf(
                        "--arguments",
                        "run",
                        "--arguments",
                        "--mac-from-jpackage",
                    ),
                )

                // Add macOS-specific options
                jpackageCommand.addAll(listOf("--mac-package-name", appName))

                // Add macOS-specific JVM options
                jpackageCommand.addAll(listOf("--java-options", "-XstartOnFirstThread"))
            }

            "windows" -> {
                // For Windows, use a relative path from the executable location
                jpackageCommand.addAll(
                    listOf(
                        "--arguments",
                        "run",
                        "--arguments",
                        "game",
                    ),
                )

                // Add Windows-specific options
                jpackageCommand.addAll(listOf("--win-dir-chooser", "--win-menu", "--win-shortcut"))
            }

            "linux" -> {
                // For Linux, use a relative path from the executable location
                jpackageCommand.addAll(
                    listOf(
                        "--arguments",
                        "run",
                        "--arguments",
                        "game",
                    ),
                )

                // Add Linux-specific options
                jpackageCommand.addAll(listOf("--linux-shortcut"))
            }

            else -> {
                // Default case
                jpackageCommand.addAll(
                    listOf(
                        "--arguments",
                        "run",
                        "--arguments",
                        "game",
                    ),
                )
            }
        }

        echo("\uD83D\uDCBB Running jpackage for $platform...")
        if (debug) {
            echo("\uD83D\uDCBB Command: " + jpackageCommand.joinToString(" "))
        }
        val process = ProcessBuilder(jpackageCommand).inheritIO().start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            echo("\uD83D\uDE31 jpackage failed with exit code $exitCode")
        }

        if (!debug) {
            tempDir.deleteRecursively()
        }
    }

    private fun createPortableJarLauncher(
        gameDir: File,
        outputDir: File,
        appName: String,
    ) {
        echo("\uD83D\uDCE6 Creating portable JAR launcher...")

        val jarFile = File(outputDir, "$appName.jar")
        createLauncherJar(gameDir, outputDir, jarFile)

        val launcherScript = if (detectCurrentPlatform() == "windows") {
            createWindowsBatchLauncher(outputDir, appName)
        } else {
            createUnixShellLauncher(outputDir, appName)
        }

        echo("\uD83D\uDCC4 Created launcher script: $launcherScript")
        echo("\uD83D\uDCA1 Users will need Java 11+ installed to run the application")
    }

    private fun createLauncherJar(
        gameDir: File,
        excludedDir: File,
        outputJar: File,
    ) {
        val cliJar = locateCliJar()
        if (cliJar == null || !cliJar.exists()) {
            echo("\uD83D\uDE31 Could not find tiny-cli JAR.")
            echo("\uD83D\uDCA1 The CLI must be installed before using export-desktop.")
            echo("\uD83D\uDCA1 Run 'make install' to install the CLI.")
            return
        }

        Files.copy(cliJar.toPath(), outputJar.toPath(), StandardCopyOption.REPLACE_EXISTING)

        // Copy all dependencies to the output directory
        val dependencies = getDependencies()
        echo("\uD83D\uDCE6 Copying ${dependencies.size} dependencies...")

        // Get the canonical path of the excluded directory for reliable comparison
        val excludedDirPath = excludedDir.canonicalPath

        for (dependency in dependencies) {
            val dependencyFile = File(dependency)
            // Get the canonical path of the dependency file
            val dependencyFilePath = dependencyFile.canonicalPath

            if (
                !dependencyFilePath.startsWith(excludedDirPath) &&
                dependencyFile.exists() && dependencyFile.isFile && dependencyFile.name.endsWith(".jar")
            ) {
                val targetFile = File(outputJar.parent, dependencyFile.name)
                echo("  - Copying ${dependencyFile.name}")
                Files.copy(dependencyFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        val gameResourcesDir = File(outputJar.parent, "game")
        gameResourcesDir.mkdirs()
        copyRecursivelyExcluding(gameDir, gameResourcesDir, excludedDir)
    }

    /**
     * Recursively copies files and directories from source to target,
     * excluding any files or directories located within the excludedDir.
     */
    private fun copyRecursivelyExcluding(
        source: File,
        target: File,
        excludedDir: File,
    ) {
        // Get the canonical path of the excluded directory for reliable comparison
        val excludedDirPath = excludedDir.canonicalPath

        // Ensure the target directory exists
        target.mkdirs()

        // Iterate through the contents of the source directory
        source.listFiles()?.forEach { sourceFile ->
            // Get the canonical path of the current source file/directory
            val sourceFilePath = sourceFile.canonicalPath

            // Check if the current source file/directory is inside the excluded directory
            if (sourceFilePath.startsWith(excludedDirPath)) {
                echo("  - Skipping excluded game resource: ${sourceFile.relativeTo(source).path}") // Use relative path for better message
                return@forEach // Skip this file/directory
            }

            // Construct the target file/directory path
            val targetFile = File(target, sourceFile.name)

            if (sourceFile.isDirectory) {
                // If it's a directory, recursively copy it
                copyRecursivelyExcluding(sourceFile, targetFile, excludedDir)
            } else {
                // If it's a file, copy it
                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    echo("  - Copying game resource: ${sourceFile.relativeTo(source).path}") // Use relative path for better message
                } catch (e: Exception) {
                    echo("\uD83D\uDE2D Error copying game resource ${sourceFile.relativeTo(source).path}: ${e.message}")
                    // Decide how to handle errors - maybe continue or throw?
                    // For now, just log and continue.
                }
            }
        }
    }

    private fun getDependencies(): List<String> {
        val classPath = System.getProperty("java.class.path")
        return classPath.split(":")
            .filter { it.endsWith(".jar") }
            .filter { !it.contains("tiny-cli-") } // Exclude the CLI jar as it's already copied
            .distinct()
    }

    private fun locateCliJar(): File? {
        // Get the JAR file from the current runtime classpath
        val codeSource = ExportCommand::class.java.protectionDomain.codeSource
        if (codeSource != null) {
            val location = codeSource.location
            if (location != null && location.protocol == "file") {
                val file = File(location.toURI())
                if (file.isFile && file.name.endsWith(".jar")) {
                    return file
                }
            }
        }

        // If not running from JAR, CLI is not properly installed
        return null
    }

    private fun createWindowsBatchLauncher(
        outputDir: File,
        appName: String,
    ): File {
        val scriptFile = File(outputDir, "$appName.bat")
        scriptFile.writeText(
            """
            @echo off
            setlocal EnableDelayedExpansion
            set CLASSPATH="%~dp0\$appName.jar"
            for %%i in ("%~dp0\*.jar") do (
                if NOT "%%~nxi"=="$appName.jar" set CLASSPATH=!CLASSPATH!;%%i
            )
            java -cp !CLASSPATH! com.github.minigdx.tiny.cli.MainKt run "%~dp0\game"
            """.trimIndent(),
        )
        return scriptFile
    }

    private fun createUnixShellLauncher(
        outputDir: File,
        appName: String,
    ): File {
        val scriptFile = File(outputDir, "$appName.sh")
        scriptFile.writeText(
            """
            #!/bin/bash
            DIR="${'$'}( cd "${'$'}( dirname "${'$'}{BASH_SOURCE[0]}" )" && pwd )"
            CLASSPATH="${'$'}DIR/$appName.jar"
            for jar in "${'$'}DIR"/*.jar; do
                if [ "${'$'}jar" != "${'$'}DIR/$appName.jar" ]; then
                    CLASSPATH="${'$'}CLASSPATH:${'$'}jar"
                fi
            done

            # Initialize the variable as empty
            MACOS_SPECIFIC_ARGS=""

            # Condition 1: OS must be macOS
            if [ `uname -s` = "Darwin" ]; then
                MACOS_SPECIFIC_ARGS="-XstartOnFirstThread"
            fi

            java ${'$'}MACOS_SPECIFIC_ARGS -cp "${'$'}CLASSPATH" com.github.minigdx.tiny.cli.MainKt run "${'$'}DIR/game"
            """.trimIndent(),
        )
        scriptFile.setExecutable(true)
        return scriptFile
    }

    private fun getPackageType(platform: String): String {
        return when (platform) {
            "windows" -> "exe"
            "mac" -> "dmg"
            "linux" -> "pkg"
            else -> "app-image"
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
class GameExporter {
    fun export(
        gameDirectory: File,
        archive: String,
    ) {
        val configFile = gameDirectory.resolve("_tiny.json")
        val gameParameters = JSON.decodeFromStream<GameParameters>(FileInputStream(configFile))

        val exportedGame = ZipOutputStream(FileOutputStream(gameDirectory.resolve(archive)))

        val exportedFile = mutableSetOf<String>()
        // Extract all engine files from the tiny-engine-js.jar
        val engineJarStream = ExportCommand::class.java
            .getResourceAsStream("/tiny-engine-js.zip")
            ?: throw IllegalStateException("Could not find tiny-engine-js.zip in classpath")

        lateinit var indexContent: String

        JarInputStream(engineJarStream).use { jarInput ->
            var entry = jarInput.nextJarEntry
            while (entry != null) {
                if (!entry.isDirectory && !entry.name.startsWith("META-INF/") && entry.name != "index.html") {
                    exportedGame.putNextEntry(ZipEntry(entry.name))
                    jarInput.copyTo(exportedGame)
                    exportedGame.closeEntry()
                    exportedFile += entry.name
                } else if (entry.name == "index.html") {
                    indexContent = jarInput.readAllBytes().decodeToString()
                }
                jarInput.closeEntry()
                entry = jarInput.nextJarEntry
            }
        }

        // Add all engine files into the zip
        ENGINE_FILES.forEach { name ->
            val content = ExportCommand::class.java.getResourceAsStream("/$name")
            val bytes = content?.readAllBytes() ?: throw IllegalStateException("Could not read engine files '$name'")
            exportedGame.putNextEntry(ZipEntry(name))
            exportedGame.write(bytes)
            exportedGame.closeEntry()

            exportedFile += name
        }

        // Add all game specific file into the zip
        exportedGame.putNextEntry(ZipEntry("_tiny.json"))
        exportedGame.write(configFile.readBytes())
        exportedGame.closeEntry()

        exportedFile += "_tiny.json"

        when (gameParameters) {
            is GameParametersV1 -> {
                (gameParameters.scripts)
                    .filterNot { exportedFile.contains(it) }
                    .forEach { name ->
                        exportedGame.putNextEntry(ZipEntry(name))
                        exportedGame.write(gameDirectory.resolve(name).readBytes())
                        exportedGame.closeEntry()

                        exportedFile += name
                    }
                gameParameters.spritesheets
                    .filterNot { exportedFile.contains(it) }
                    .forEach { name ->
                        exportedGame.putNextEntry(ZipEntry(name))
                        exportedGame.write(gameDirectory.resolve(name).readBytes())
                        exportedGame.closeEntry()

                        exportedFile += name
                    }
                listOfNotNull(gameParameters.sound)
                    .filterNot { exportedFile.contains(it) }
                    .forEach { name ->
                        exportedGame.putNextEntry(ZipEntry(name))
                        exportedGame.write(gameDirectory.resolve(name).readBytes())
                        exportedGame.closeEntry()

                        exportedFile += name
                    }
                gameParameters.levels
                    .filterNot { exportedFile.contains(it) }
                    .forEach { name ->
                        exportedGame.putNextEntry(ZipEntry(name))
                        exportedGame.write(gameDirectory.resolve(name).readBytes())
                        exportedGame.closeEntry()

                        exportedFile += name

                        val ldtk = Ldtk.read(gameDirectory.resolve(name).readText())
                        ldtk.levels.flatMap { level -> level.layerInstances }
                            .mapNotNull { it.__tilesetRelPath }
                            .map { gameDirectory.resolve(it) }
                            .filterNot { file -> exportedFile.contains(file.relativeTo(gameDirectory).name) }
                            .toSet()
                            .forEach { file ->
                                exportedGame.putNextEntry(ZipEntry(file.relativeTo(gameDirectory).name))
                                exportedGame.write(file.readBytes())
                                exportedGame.closeEntry()

                                exportedFile += name
                            }
                    }

                // Add index.html

                var template = indexContent
                template = template.replace("{GAME_ID}", gameParameters.id)
                template = template.replace("{GAME_NAME}", gameParameters.name)
                template = template.replace("{GAME_WIDTH}", gameParameters.resolution.width.toString())
                template = template.replace("{GAME_HEIGHT}", gameParameters.resolution.height.toString())
                template = template.replace("{GAME_ZOOM}", gameParameters.zoom.toString())
                template = template.replace("{GAME_SPRW}", gameParameters.sprites.width.toString())
                template = template.replace("{GAME_SPRH}", gameParameters.sprites.height.toString())
                template = template.replace("{GAME_HIDE_MOUSE}", gameParameters.hideMouseCursor.toString())

                template = replaceList(
                    template,
                    gameParameters.scripts,
                    "{GAME_SCRIPT}",
                    "GAME_SCRIPT",
                )
                template = replaceList(
                    template,
                    gameParameters.spritesheets,
                    "{GAME_SPRITESHEET}",
                    "GAME_SPRITESHEET",
                )
                template = replaceList(template, gameParameters.levels, "{GAME_LEVEL}", "GAME_LEVEL")
                template = replaceList(template, listOfNotNull(gameParameters.sound), "{GAME_SOUND}", "GAME_SOUND")

                template = template.replace("{GAME_COLORS}", gameParameters.colors.joinToString(","))

                exportedGame.putNextEntry(ZipEntry("index.html"))
                exportedGame.write(template.toByteArray())
                exportedGame.closeEntry()
            }
        }

        exportedGame.close()
    }

    private fun replaceList(
        template: String,
        values: List<String>,
        tag: String,
        delimiter: String,
    ): String {
        val pattern = ("<!-- $delimiter -->(.*?)<!-- ${delimiter}_END -->").toRegex(RegexOption.DOT_MATCHES_ALL)
        val delimiterTag = pattern.find(template)!!.groupValues[1]

        var result = ""
        values.forEach { script ->
            result += delimiterTag.replace(tag, script)
        }
        return template.replace(delimiterTag, result)
    }

    companion object {
        val ENGINE_FILES =
            setOf(
                "_boot.lua",
                "_boot.png",
                "_engine.lua",
            )
    }
}
