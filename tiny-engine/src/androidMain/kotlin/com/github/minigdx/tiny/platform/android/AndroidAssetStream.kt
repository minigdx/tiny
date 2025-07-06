package com.github.minigdx.tiny.platform.android

import android.content.Context
import android.graphics.BitmapFactory
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.platform.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AndroidAssetStream(
    private val context: Context,
    private val name: String,
) : SourceStream<ByteArray> {
    override suspend fun read(): ByteArray = withContext(Dispatchers.IO) {
        context.assets.open(name).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.toByteArray()
            }
        }
    }
}

class AndroidImageStream(
    private val context: Context,
    private val name: String,
) : SourceStream<ImageData> {
    override suspend fun read(): ImageData = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.assets.open(name).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

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
        } catch (_: Exception) {
            // Try from resources if not in assets
            val resourceId = context.resources.getIdentifier(
                name.removeSuffix(".${name.substringAfterLast(".")}"),
                "drawable",
                context.packageName,
            )
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
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

        }
    }
}
