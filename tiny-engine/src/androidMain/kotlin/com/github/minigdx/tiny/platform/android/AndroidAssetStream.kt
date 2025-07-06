package com.github.minigdx.tiny.platform.android

import android.content.Context
import android.graphics.BitmapFactory
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AndroidAssetStream(
    private val context: Context,
    private val name: String,
) : SourceStream<ByteArray> {
    override suspend fun read(): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(name).use { inputStream ->
                    ByteArrayOutputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.toByteArray()
                    }
                }
            } catch (e: Exception) {
                // Try from resources if not in assets
                try {
                    val resourceId = context.resources.getIdentifier(
                        name.removeSuffix(".${name.substringAfterLast(".")}"),
                        "raw",
                        context.packageName,
                    )
                    if (resourceId != 0) {
                        context.resources.openRawResource(resourceId).use { inputStream ->
                            ByteArrayOutputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                                outputStream.toByteArray()
                            }
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
}

class AndroidImageStream(
    private val context: Context,
    private val name: String,
) : SourceStream<ImageData> {
    override suspend fun read(): ImageData? =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = context.assets.open(name).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }

                if (bitmap != null) {
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                    // Convert from ARGB to RGBA
                    val data = ByteArray(width * height * 4)
                    for (i in pixels.indices) {
                        val pixel = pixels[i]
                        val offset = i * 4
                        data[offset] = ((pixel shr 16) and 0xFF).toByte() // R
                        data[offset + 1] = ((pixel shr 8) and 0xFF).toByte() // G
                        data[offset + 2] = (pixel and 0xFF).toByte() // B
                        data[offset + 3] = ((pixel shr 24) and 0xFF).toByte() // A
                    }

                    bitmap.recycle()
                    ImageData(data, width, height)
                } else {
                    null
                }
            } catch (e: Exception) {
                // Try from resources if not in assets
                try {
                    val resourceId = context.resources.getIdentifier(
                        name.removeSuffix(".${name.substringAfterLast(".")}"),
                        "drawable",
                        context.packageName,
                    )
                    if (resourceId != 0) {
                        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                        if (bitmap != null) {
                            val width = bitmap.width
                            val height = bitmap.height
                            val pixels = IntArray(width * height)
                            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                            // Convert from ARGB to RGBA
                            val data = ByteArray(width * height * 4)
                            for (i in pixels.indices) {
                                val pixel = pixels[i]
                                val offset = i * 4
                                data[offset] = ((pixel shr 16) and 0xFF).toByte() // R
                                data[offset + 1] = ((pixel shr 8) and 0xFF).toByte() // G
                                data[offset + 2] = (pixel and 0xFF).toByte() // B
                                data[offset + 3] = ((pixel shr 24) and 0xFF).toByte() // A
                            }

                            bitmap.recycle()
                            ImageData(data, width, height)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
}

class AndroidSoundStream(
    private val context: Context,
    private val name: String,
    private val soundManager: SoundManager,
) : SourceStream<SoundData> {
    override suspend fun read(): SoundData? =
        withContext(Dispatchers.IO) {
            try {
                // Read the sound file data
                val data = AndroidAssetStream(context, name).read()
                if (data != null) {
                    // Parse the sound data (assuming it's in the engine's format)
                    // For now, we'll create a simple placeholder
                    // In a real implementation, you'd parse the actual sound format
                    val music = Music(
                        notes = emptyList(),
                        instruments = emptyList(),
                        sfxs = emptyList(),
                    )

                    SoundData(
                        name = name,
                        soundManager = soundManager,
                        music = music,
                        musicalBars = emptyList(),
                        musicalSequences = emptyList(),
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
}
