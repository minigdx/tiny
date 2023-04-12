package com.github.minigdx.tiny.file

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.suspendCoroutine

class AjaxStream(private val url: String) : SourceStream<ByteArray> {
    // https://youtrack.jetbrains.com/issue/KT-30098
    private fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).unsafeCast<ByteArray>()

    override suspend fun read(): ByteArray {
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
}
