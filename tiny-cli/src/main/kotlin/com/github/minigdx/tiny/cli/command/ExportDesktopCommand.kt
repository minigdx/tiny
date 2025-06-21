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
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ExportDesktopCommand : CliktCommand(name = "export-desktop") {
    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val outputDirectory by option("-o", "--output", help = "Output directory for the exported application")
        .file(canBeDir = true, canBeFile = false)
        .default(File("export-desktop"))

    val platform by option("-p", "--platform", help = "Target platform")
        .choice("windows", "linux", "mac", "current")
        .default("current")

    val includeJdk by option("--include-jdk", help = "Include JDK in the package (requires jpackage)")
        .flag(default = true)

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

        createLauncherJar(gameDir, appJarFile)

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
        createLauncherJar(gameDir, jarFile)

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

        val gameResourcesDir = File(outputJar.parent, "game")
        gameResourcesDir.mkdirs()
        gameDir.copyRecursively(gameResourcesDir, overwrite = true)
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
            java -jar "%~dp0\$appName.jar" run "%~dp0\game"
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
            java -jar "${'$'}DIR/$appName.jar" run "${'$'}DIR/game"
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
