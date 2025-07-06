package com.github.minigdx.tiny.platform.android

import android.content.Context
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.TargetStream
import java.io.File

class AndroidLocalFile(
    private val context: Context,
    private val name: String,
    private val parentDirectory: String?,
) : LocalFile {
    private val file: File by lazy {
        val parent = if (parentDirectory != null) {
            File(context.filesDir, parentDirectory).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        } else {
            context.filesDir
        }
        File(parent, name)
    }

    override fun name(): String = name

    override fun isFile(): Boolean = file.isFile

    override fun isDirectory(): Boolean = file.isDirectory

    override fun mkdirs(): Boolean = file.mkdirs()

    override fun writeAll(
        data: ByteArray,
        append: Boolean,
    ): Int {
        return try {
            if (append) {
                file.appendBytes(data)
            } else {
                file.writeBytes(data)
            }
            data.size
        } catch (e: Exception) {
            -1
        }
    }

    override fun readAll(): ByteArray? {
        return try {
            if (file.exists() && file.isFile) {
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun stream(): TargetStream {
        return AndroidFileStream(file)
    }

    override fun list(): List<LocalFile> {
        return if (file.isDirectory) {
            file.listFiles()?.map { child ->
                AndroidLocalFile(context, child.name, file.path.removePrefix(context.filesDir.path).removePrefix("/"))
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun delete(): Boolean = file.delete()

    override fun deleteRecursively(): Boolean = file.deleteRecursively()

    override fun exists(): Boolean = file.exists()

    override fun get(name: String): LocalFile {
        val childParent = if (parentDirectory != null) {
            "$parentDirectory/${this.name}"
        } else {
            this.name
        }
        return AndroidLocalFile(context, name, childParent)
    }

    override fun length(): Long = file.length()
}

class AndroidFileStream(private val file: File) : TargetStream {
    private val outputStream = file.outputStream()

    override fun write(data: ByteArray): Int {
        return try {
            outputStream.write(data)
            data.size
        } catch (e: Exception) {
            -1
        }
    }

    override fun close() {
        outputStream.close()
    }
}
