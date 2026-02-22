package com.squareup.gifencoder

import com.github.minigdx.tiny.graphic.ColorPalette
import java.io.IOException
import java.io.OutputStream
import java.util.BitSet

class FastGifEncoder(
    private val outputStream: OutputStream,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val loopCount: Int,
    private val rgbPalette: ColorPalette,
) {
    private val colorTable =
        ColorTable.fromColors(
            (0 until rgbPalette.size).map { index -> rgbPalette.getRGAasInt(index) }
                .map { rgb -> Color.fromRgbInt(rgb) }
                .toSet(),
        )

    init {
        HeaderBlock.write(outputStream)
        LogicalScreenDescriptorBlock.write(
            outputStream, screenWidth, screenHeight, false, 1, false, 0,
            0, 0,
        )
        NetscapeLoopingExtensionBlock.write(outputStream, loopCount)
    }

    /**
     * Add an image to the GIF file.
     *
     * @param rgbData an image buffer in RGB format
     * @param width the number of pixels per row in the pixel array
     * @param options options to be applied to this image
     * @return this instance for chaining
     * @throws IOException if there was a problem writing to the given output stream
     */
    @Throws(IOException::class)
    fun addImage(
        rgbData: IntArray,
        width: Int,
        options: ImageOptions,
    ): FastGifEncoder {
        addImage(Image.fromRgb(rgbData, width), options)
        return this
    }

    /**
     * Add a frame from pre-indexed palette data, skipping color quantization.
     *
     * @param indexedData a ByteArray where each byte is a palette index
     * @param width the number of pixels per row
     * @param options options to be applied to this image
     * @return this instance for chaining
     */
    @Synchronized
    @Throws(IOException::class)
    fun addIndexedImage(
        indexedData: ByteArray,
        width: Int,
        options: ImageOptions,
    ): FastGifEncoder {
        val height = indexedData.size / width
        require(
            !(options.left + width > screenWidth || options.top + height > screenHeight),
        ) { "Image does not fit in screen." }

        var paddedColorCount = 2
        while (paddedColorCount < rgbPalette.size) {
            paddedColorCount *= 2
        }
        GraphicsControlExtensionBlock.write(
            outputStream,
            options.disposalMethod,
            false,
            false,
            options.delayCentiseconds,
            0,
        )
        ImageDescriptorBlock.write(
            outputStream, options.left, options.top, width,
            height, true, false, false, getColorTableSizeField(paddedColorCount),
        )
        // Write color table directly from rgbPalette to preserve duplicate RGB entries
        for (i in 0 until rgbPalette.size) {
            val rgb = rgbPalette.getRGAasInt(i)
            outputStream.write(rgb shr 16 and 0xFF)
            outputStream.write(rgb shr 8 and 0xFF)
            outputStream.write(rgb and 0xFF)
        }
        for (i in rgbPalette.size until paddedColorCount) {
            outputStream.write(0)
            outputStream.write(0)
            outputStream.write(0)
        }

        // Convert ByteArray palette indices to IntArray for LZW encoder
        val colorIndices = IntArray(indexedData.size) { indexedData[it].toInt() and 0xFF }

        val lzwEncoder = FastLzwEncoder(paddedColorCount)
        val lzwData = lzwEncoder.encode(colorIndices)
        ImageDataBlock.write(outputStream, lzwEncoder.minimumCodeSize, lzwData)
        return this
    }

    /**
     * Writes the trailer. This should be called exactly once per GIF file.
     *
     *
     * This method does not close the input stream. We consider it the caller's responsibility to
     * close it at the appropriate time, which often (but not always) will be just after calling this
     * method.
     */
    @Synchronized
    @Throws(IOException::class)
    fun finishEncoding() {
        // The trailer block indicates when you've hit the end of the file.
        outputStream.write(0x3B)
    }

    @Synchronized
    @Throws(IOException::class)
    fun addImage(
        image: Image,
        options: ImageOptions,
    ) {
        require(
            !(
                options.left + image.width > screenWidth ||
                    options.top + image.height > screenHeight
            ),
        ) { "Image does not fit in screen." }

        val paddedColorCount = colorTable.paddedSize()
        val colorIndices = colorTable.getIndices(image)
        GraphicsControlExtensionBlock.write(
            outputStream,
            options.disposalMethod,
            false,
            false,
            options.delayCentiseconds,
            0,
        )
        ImageDescriptorBlock.write(
            outputStream, options.left, options.top, image.width,
            image.height, true, false, false, getColorTableSizeField(paddedColorCount),
        )
        colorTable.write(outputStream)

        val lzwEncoder = FastLzwEncoder(colorTable.paddedSize())
        val lzwData = lzwEncoder.encode(colorIndices)
        ImageDataBlock.write(outputStream, lzwEncoder.minimumCodeSize, lzwData)
    }

    /**
     * Compute the "size of the color table" field as the spec defines it:
     *
     * <blockquote>this field is used to calculate the number of bytes contained in the Global Color
     * Table. To determine that actual size of the color table, raise 2 to [the value of the field +
     * 1]</blockquote>
     */
    private fun getColorTableSizeField(actualTableSize: Int): Int {
        var size = 0
        while (1 shl size + 1 < actualTableSize) {
            ++size
        }
        return size
    }

    companion object {
        private const val MAX_COLOR_COUNT = 256
    }
}

/**
 * LZW encoder using an integer-array trie instead of HashMap for zero-allocation encoding.
 *
 * For background, see Appendix F of the
 * [GIF spec](http://www.w3.org/Graphics/GIF/spec-gif89a.txt).
 */
internal class FastLzwEncoder(private val colorTableSize: Int) {
    val minimumCodeSize: Int

    private val outputBits = BitSet()
    private var position = 0
    private var codeSize = 0

    // Trie: each node has colorTableSize child slots. -1 means no child.
    // Node 0..colorTableSize-1 are single-color root nodes.
    // clearCode and endOfInfoCode are special codes with no trie node.
    private var trieChildren: Array<IntArray> = emptyArray()
    private var nodeCodes: IntArray = IntArray(0)
    private var nextNodeIndex = 0
    private var nextCode = 0

    private val clearCode: Int
    private val endOfInfoCode: Int

    // Current trie node (-1 means empty buffer)
    private var currentNode = -1

    init {
        require(GifMath.isPowerOfTwo(colorTableSize)) { "Color table size must be a power of 2" }
        minimumCodeSize = computeMinimumCodeSize(colorTableSize)
        clearCode = 1 shl minimumCodeSize
        endOfInfoCode = clearCode + 1
        resetCodeTableAndCodeSize()
    }

    fun encode(indices: IntArray): ByteArray {
        writeCode(clearCode)
        for (index in indices) {
            processIndex(index)
        }
        // Flush remaining buffer
        if (currentNode >= 0) {
            writeCode(nodeCodes[currentNode])
        }
        writeCode(endOfInfoCode)
        return toBytes()
    }

    private fun processIndex(index: Int) {
        if (currentNode < 0) {
            // Empty buffer: start with the single-color root node
            currentNode = index
            return
        }
        val childNode = trieChildren[currentNode][index]
        if (childNode >= 0) {
            // Extend: the sequence is already in the trie
            currentNode = childNode
        } else {
            // Output code for current sequence
            writeCode(nodeCodes[currentNode])
            if (nextCode == MAX_CODE_TABLE_SIZE) {
                // Table full: emit clear code, reset
                writeCode(clearCode)
                resetCodeTableAndCodeSize()
            } else {
                // Add new trie node for currentNode + index
                val newNode = nextNodeIndex++
                trieChildren[currentNode][index] = newNode
                nodeCodes[newNode] = nextCode
                if (nextCode == 1 shl codeSize) {
                    ++codeSize
                }
                nextCode++
            }
            // Reset buffer to single-color root node
            currentNode = index
        }
    }

    private fun writeCode(code: Int) {
        for (shift in 0 until codeSize) {
            val bit = code ushr shift and 1 != 0
            outputBits[position++] = bit
        }
    }

    private fun toBytes(): ByteArray {
        val bitCount = position
        val result = ByteArray((bitCount + 7) / 8)
        for (i in 0 until bitCount) {
            val byteIndex = i / 8
            val bitIndex = i % 8
            result[byteIndex] = (result[byteIndex].toInt() or ((if (outputBits[i]) 1 else 0) shl bitIndex)).toByte()
        }
        return result
    }

    private fun resetCodeTableAndCodeSize() {
        // Max 4096 entries per GIF spec
        trieChildren = Array(MAX_CODE_TABLE_SIZE) { IntArray(colorTableSize) { -1 } }
        nodeCodes = IntArray(MAX_CODE_TABLE_SIZE)

        val colorsInCodeTable = 1 shl minimumCodeSize
        // Initialize root nodes (single-color entries)
        for (i in 0 until colorsInCodeTable) {
            nodeCodes[i] = i
        }
        nextNodeIndex = colorsInCodeTable
        // clearCode = colorsInCodeTable, endOfInfoCode = colorsInCodeTable + 1
        nextCode = endOfInfoCode + 1

        // Code size starts one bit larger to accommodate clear and end-of-info codes
        codeSize = minimumCodeSize + 1

        currentNode = -1
    }

    companion object {
        private const val MAX_CODE_TABLE_SIZE = 1 shl 12

        private fun computeMinimumCodeSize(colorTableSize: Int): Int {
            var size = 2
            while (colorTableSize > 1 shl size) {
                ++size
            }
            return size
        }
    }
}
