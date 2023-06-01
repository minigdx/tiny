package com.github.minigdx.tiny.file

import com.github.minigdx.tiny.platform.ImageData
import kotlinx.browser.document
import org.khronos.webgl.Int8Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.suspendCoroutine

class ImageDataStream(val url: String) : SourceStream<ImageData> {

    override suspend fun exists(): Boolean {
        return suspendCoroutine { continuation ->
            val jsonFile = XMLHttpRequest()
            jsonFile.responseType = XMLHttpRequestResponseType.Companion.ARRAYBUFFER
            jsonFile.open("HEAD", url, true)

            jsonFile.onload = { _ ->
                continuation.resumeWith(Result.success(jsonFile.status == 200.toShort()))
            }
            jsonFile.send()
        }
    }

    override suspend fun read(): ImageData {
        return suspendCoroutine { continuation ->

            val img = Image()
            img.addEventListener(
                "load",
                object : EventListener {
                    override fun handleEvent(event: Event) {
                        val canvas = document.createElement("canvas") as HTMLCanvasElement
                        val context = canvas.getContext("2d") as CanvasRenderingContext2D

                        canvas.width = img.width
                        canvas.height = img.height
                        context.drawImage(img, 0.0, 0.0)
                        val rawImageData = context.getImageData(0.0, 0.0, img.width.toDouble(), img.height.toDouble())
                        val data = Int8Array(rawImageData.data.buffer).unsafeCast<ByteArray>()
                        continuation.resumeWith(Result.success(ImageData(data, img.width, img.height)))
                    }
                },
            )
            img.src = url
        }
    }
}
