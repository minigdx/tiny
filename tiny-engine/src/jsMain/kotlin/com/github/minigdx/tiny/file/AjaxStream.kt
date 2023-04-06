package com.github.minigdx.tiny.file

import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.suspendCoroutine

class AjaxStream(private val url: String) : SourceStream<ByteArray> {
    // https://youtrack.jetbrains.com/issue/KT-30098
    private fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).unsafeCast<ByteArray>()

    private var updated = true

    override fun wasModified(): Boolean {
        return updated
    }

    override suspend fun read(): ByteArray {
        updated = false
        return suspendCoroutine { continuation ->
            val jsonFile = XMLHttpRequest()
            jsonFile.responseType = XMLHttpRequestResponseType.Companion.ARRAYBUFFER
            jsonFile.open("GET", url, true)

            jsonFile.onload = { _ ->
                if (jsonFile.readyState == 4.toShort() && jsonFile.status == 200.toShort()) {
                    val data = jsonFile.response as ArrayBuffer
                    val byteArray = data.toByteArray()

                    continuation.resumeWith(Result.success(byteArray))
                }
            }
            jsonFile.send()
        }
    }
}
