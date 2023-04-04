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


    private val colorTable = ColorTable.fromColors(
        (0 until rgbPalette.size).map { index -> rgbPalette.getRGAasInt(index) }
            .map { rgb -> Color.fromRgbInt(rgb) }
            .toSet()
    )

    init {
        HeaderBlock.write(outputStream)
        LogicalScreenDescriptorBlock.write(
            outputStream, screenWidth, screenHeight, false, 1, false, 0,
            0, 0
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
    fun addImage(rgbData: IntArray, width: Int, options: ImageOptions): FastGifEncoder {
        addImage(Image.fromRgb(rgbData, width), options)
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
    fun addImage(image: Image, options: ImageOptions) {
        require(
            !(options.left + image.width > screenWidth ||
                options.top + image.height > screenHeight)
        ) { "Image does not fit in screen." }

        val paddedColorCount = colorTable.paddedSize()
        val colorIndices = colorTable.getIndices(image)
        GraphicsControlExtensionBlock.write(
            outputStream, options.disposalMethod, false, false,
            options.delayCentiseconds, 0
        )
        ImageDescriptorBlock.write(
            outputStream, options.left, options.top, image.width,
            image.height, true, false, false, getColorTableSizeField(paddedColorCount)
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
 * For background, see Appendix F of the
 * [GIF spec](http://www.w3.org/Graphics/GIF/spec-gif89a.txt).
 */
internal class FastLzwEncoder(colorTableSize: Int) {
    val minimumCodeSize: Int
    private val outputBits = BitSet()
    private var position = 0
    private var codeTable: MutableMap<String, Int> = defaultCodeTable()
    private var codeSize = 0
    private var indexBuffer: String = ""

    /**
     * @param colorTableSize Size of the (padded) color table; must be a power of 2
     */
    init {
        require(GifMath.isPowerOfTwo(colorTableSize)) { "Color table size must be a power of 2" }
        minimumCodeSize = computeMinimumCodeSize(colorTableSize)
        resetCodeTableAndCodeSize()
    }

    fun encode(indices: IntArray): ByteArray {
        writeCode(codeTable[CLEAR_CODE]!!)
        for (index in indices) {
            processIndex(index)
            // writeCode(codeTable[indexBuffer]!!)
            // writeCode(codeTable[index.toChar().toString()]!!)
        }
        writeCode(codeTable[indexBuffer]!!)
        writeCode(codeTable[END_OF_INFO]!!)
        return toBytes()
    }

    private fun processIndex(index: Int) {
        val indexAsStr = index.toChar().toString()
        val extendedIndexBuffer = indexBuffer + indexAsStr
        indexBuffer = if (codeTable.containsKey(extendedIndexBuffer)) {
            extendedIndexBuffer
        } else {
            writeCode(codeTable[indexBuffer]!!)
            if (codeTable.size == MAX_CODE_TABLE_SIZE) {
                writeCode(codeTable[CLEAR_CODE]!!)
                resetCodeTableAndCodeSize()
            } else {
                addCodeToTable(extendedIndexBuffer)
            }
            indexAsStr
        }
    }

    /**
     * Write the given code to the output stream.
     */
    private fun writeCode(code: Int) {
        for (shift in 0 until codeSize) {
            val bit = code ushr shift and 1 != 0
            outputBits[position++] = bit
        }
    }

    /**
     * Convert our stream of bits into a byte array, as described in the spec.
     */
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

    private fun addCodeToTable(indices: String) {
        val newCode = codeTable.size
        codeTable[indices] = newCode
        if (newCode == 1 shl codeSize) {
            // The next code won't fit in {@code codeSize} bits, so we need to increment.
            ++codeSize
        }
    }

    private fun resetCodeTableAndCodeSize() {
        codeTable = defaultCodeTable()

        // We add an extra bit because of the special "clear" and "end of info" codes.
        codeSize = minimumCodeSize + 1
    }

    private fun defaultCodeTable(): MutableMap<String, Int> {
        val codeTable: MutableMap<String, Int> = HashMap(126 * 4 * 30 * 60)

        // The spec indicates that CLEAR_CODE must have a value of 2**minimumCodeSize. Thus we reserve
        // the first 2**minimumCodeSize codes for colors, even if our color table is smaller.
        val colorsInCodeTable = 1 shl minimumCodeSize
        for (i in 0 until colorsInCodeTable) {
            codeTable[i.toChar().toString()] = i
        }
        codeTable[CLEAR_CODE] = codeTable.size
        codeTable[(END_OF_INFO)] = codeTable.size
        return codeTable
    }

    companion object {
        // Dummy values to represent special, GIF-specific instructions.
        private val CLEAR_CODE = (-1).toChar().toString()
        private val END_OF_INFO = (-2).toChar().toString()

        /**
         * The specification stipulates that code size may not exceed 12 bits.
         */
        private const val MAX_CODE_TABLE_SIZE = 1 shl 12

        /**
         * This computes what the spec refers to as "code size". The actual starting code size will be one
         * bit larger than this, because of the special "clear" and "end of info" codes.
         */
        private fun computeMinimumCodeSize(colorTableSize: Int): Int {
            var size = 2 // LZW has a minimum code size of 2.
            while (colorTableSize > 1 shl size) {
                ++size
            }
            return size
        }

        private fun <T> append(list: List<T>, value: T): List<T> {
            return list + value
        }
    }
}


