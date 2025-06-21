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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ExportDesktopCommand : CliktCommand(name = "export-desktop") {
    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val outputDirectory by option("-o", "--output", help = "Output directory for the exported application")
        .file(canBeDir = true, canBeFile = false)
        .default(File("exported-game"))

    val platform by option("-p", "--platform", help = "Target platform")
        .choice("windows", "linux", "mac", "current")
        .default("current")

    val includeJdk by option("--include-jdk", help = "Include JDK in the package (requires jpackage)")
        .flag("--exclude-jdk", default = true)

    val appName by option("-n", "--name", help = "Application name (defaults to game name)")

    val appVersion by option("-v", "--version", help = "Application version")
        .default("1.0.0")

    override fun help(context: Context) = "Export your game as a standalone desktop application."

    override fun run() {
        echo("\uD83D\uDC77 Export Desktop Application from ${gameDirectory.absolutePath}")

        val configFile = gameDirectory.resolve("_tiny.json")
        val gameParameters = Json.decodeFromStream<GameParameters>(FileInputStream(configFile))
        val finalAppName = appName ?: gameParameters.name

        if (includeJdk && !isJpackageAvailable()) {
            echo("\uD83D\uDE31 jpackage is not available. Please use Java 14 or later with jpackage support.")
            echo("\uD83D\uDCA1 Alternatively, use --no-include-jdk to create a portable JAR launcher.")
            return
        }

        val targetPlatform = when (platform) {
            "current" -> detectCurrentPlatform()
            else -> platform
        }

        outputDirectory.mkdirs()

        if (includeJdk) {
            createStandaloneAppWithJdk(gameDirectory, outputDirectory, finalAppName, appVersion, targetPlatform)
        } else {
            createPortableJarLauncher(gameDirectory, outputDirectory, finalAppName)
        }

        echo("\uD83C\uDF89 Congratulations! Your desktop application has been exported to $outputDirectory")
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

        if (platform == "mac") {
            jpackageCommand.addAll(listOf("--mac-package-name", appName))
        }

        if (platform == "windows") {
            jpackageCommand.addAll(listOf("--win-dir-chooser", "--win-menu", "--win-shortcut"))
        }

        if (platform == "linux") {
            jpackageCommand.addAll(listOf("--linux-shortcut"))
        }

        echo("\uD83D\uDCBB Running jpackage for $platform...")
        val process = ProcessBuilder(jpackageCommand).inheritIO().start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            echo("\uD83D\uDE31 jpackage failed with exit code $exitCode")
        }

        tempDir.deleteRecursively()
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
        excludedDir: File
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
        val codeSource = ExportDesktopCommand::class.java.protectionDomain.codeSource
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
            "linux" -> "deb"
            else -> "app-image"
        }
    }
}
