package com.example.cs567_3d_ui_project.argis.mlutils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import com.otaliastudios.opengl.extensions.toBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv


/**
 * Helper class used to efficiently convert a [Media.Image] object from
 * [ImageFormat.YUV_420_888] format to an RGB [Bitmap] object.
 *
 * The [yuvToRgb] method is able to achieve the same FPS as the CameraX image
 * analysis use case on a Pixel 3 XL device at the default analyzer resolution,
 * which is 30 FPS with 640x480.
 *
 * NOTE: This has been tested in a limited number of devices and is not
 * considered production-ready code. It was created for illustration purposes,
 * since this is not an efficient camera pipeline due to the multiple copies
 * required to convert each frame.
 */
class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context)
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var pixelCount: Int = -1
    private lateinit var yuvBuffer: ByteArray
    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {

        // Ensure that the intermediate output byte buffer is allocated
        if (!::yuvBuffer.isInitialized) {
            pixelCount = image.width * image.height
            // Bits per pixel is an average for the whole image, so it's useful to compute the size
            // of the full buffer but should not be used to determine pixel offsets
            val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
            val other = (pixelCount * 1.5f).toInt()
            //yuvBuffer = ByteArray(pixelCount * pixelSizeBits / 8)

            yuvBuffer = ByteArray((pixelCount * 1.5f).toInt())
        }

        // Get the YUV data in byte array form using NV21 format
        imageToByteArray(image, yuvBuffer)

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.size)
        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvBuffer)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(output)
    }

    //https://en.wikipedia.org/wiki/Clamp_(function)
    fun clamp(x: Int, min: Int, max: Int): Int{
        if(x < min){
            return min
        }
        if(x > max){
            return max
        }
        return x
    }
    //https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/#theory


    //https://stackoverflow.com/questions/60066025/getting-rgb-values-from-yuv-420-888-image

    fun yToR(y: Float, u: Float, v: Float): IntArray{
        val rgb = IntArray(3)

        val rTemp = ((y - 16) * 1.164 + (v - 128) * 1.596).toInt()
        val gTemp = ((y - 16) * 1.164 - (u - 128) * 0.392 - (v - 128) * 0.813).toInt()
        val bTemp = ((y - 16) * 1.164 + (u - 128) * 2.017).toInt()

        rgb[0] = clamp(rTemp, 0, 255)
        rgb[1] = clamp(gTemp, 0, 255)
        rgb[2] = clamp(bTemp, 0, 255)

        return rgb
    }

    fun yuvToRgb3(image: Image): Bitmap{
        require(image.format === ImageFormat.YUV_420_888) {
            "Invalid image format" }

        val imageWidth = image.width
        val imageHeight = image.height
        // ARGB array needed by Bitmap static factory method I use below.
        // ARGB array needed by Bitmap static factory method I use below.
        val argbArray = IntArray(imageWidth * imageHeight)
        val yBuffer = image.planes[0].buffer
        yBuffer.position(0)

        // A YUV Image could be implemented with planar or semi planar layout.
        // A planar YUV image would have following structure:
        // YYYYYYYYYYYYYYYY
        // ................
        // UUUUUUUU
        // ........
        // VVVVVVVV
        // ........
        //
        // While a semi-planar YUV image would have layout like this:
        // YYYYYYYYYYYYYYYY
        // ................
        // UVUVUVUVUVUVUVUV   <-- Interleaved UV channel
        // ................
        // This is defined by row stride and pixel strides in the planes of the
        // image.

        // Plane 1 is always U & plane 2 is always V
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888

        val uBuffer = image.planes[1].buffer
        uBuffer.position(0)
        val vBuffer = image.planes[2].buffer
        vBuffer.position(0)

        // The U/V planes are guaranteed to have the same row stride and pixel
        // stride.
        // The U/V planes are guaranteed to have the same row stride and pixel
        // stride.
        val yRowStride = image.planes[0].rowStride
        val yPixelStride = image.planes[0].pixelStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride

        var r: Int
        var g: Int
        var b: Int
        var yValue: Int
        var uValue: Int
        var vValue: Int

        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val yIndex = y * yRowStride + x * yPixelStride
                // Y plane should have positive values belonging to [0...255]
                yValue = yBuffer[yIndex].toInt() and 0xff
                val uvx = x / 2
                val uvy = y / 2
                // U/V Values are subsampled i.e. each pixel in U/V chanel in a
                // YUV_420 image act as chroma value for 4 neighbouring pixels
                val uvIndex = uvy * uvRowStride + uvx * uvPixelStride

                // U/V values ideally fall under [-0.5, 0.5] range. To fit them into
                // [0, 255] range they are scaled up and centered to 128.
                // Operation below brings U/V values to [-128, 127].
                uValue = (uBuffer[uvIndex].toInt() and 0xff) - 128
                vValue = (vBuffer[uvIndex].toInt() and 0xff) - 128

                // Compute RGB values per formula above.
                r = (yValue + 1.370705f * vValue).toInt()
                g = (yValue - 0.698001f * vValue - 0.337633f * uValue).toInt()
                b = (yValue + 1.732446f * uValue).toInt()

                r = clamp(r, 0, 255)
                g = clamp(g, 0, 255)
                b = clamp(b, 0, 255)

                // Use 255 for alpha value, no transparency. ARGB values are
                // positioned in each byte of a single 4 byte integer
                // [AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB]
                val argbIndex = y * imageWidth + x
                argbArray[argbIndex] =
                    255 shl 24 or (r and 255 shl 16) or (g and 255 shl 8) or (b and 255)
            }
        }

        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        bitmap.setPixels(argbArray, 0, image.width, 0, 0, image.width, image.height)
        return bitmap
    }

    fun yuvToRgb2(image: Image): Bitmap {
        val subImageWidth = image.width
        val subImageHeight = image.height

        val bb = ByteBuffer.allocateDirect(image.width * image.height * 3 * 4)
        bb.order(ByteOrder.nativeOrder())

        val plane0 = image.planes[0]
        val plane1 = image.planes[1]
        val plane2 = image.planes[2]

        val width = plane0.rowStride
        val height = plane0.buffer.capacity() / plane0.rowStride

        val xOffset = (width - image.width) / 2
        val yOffset = (height - image.height) / 2

        for(y in 0 until height)
        {
            for(x in 0 until width)
            {
                val yb = plane0.buffer[(x + xOffset) + width + (y + yOffset)].toFloat()
                val ub = plane1.buffer[((x + xOffset)/2) * 2 + plane1.rowStride * ((y + yOffset)/2)].toFloat()
                val vb = plane2.buffer[((x + xOffset)/2) * 2 + plane2.rowStride * ((y + yOffset)/2)].toFloat()

                val rgb = yToR(yb, ub, vb)

                bb.putInt(rgb[0])
                bb.putInt(rgb[1])
                bb.putInt(rgb[2])
            }
        }

        bb.rewind()

        val bitmap = Bitmap.createBitmap(subImageWidth, subImageHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(image.width * image.height)
        for(i in 0 until image.width*image.height)
        {
            val a = 0xFF
            val r: Int = bb.int
            val g: Int = bb.int
            val b: Int = bb.int
            pixels[i] = a shl 24 or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)

        return bitmap
    }

    fun imageToByteArray3(image: Image): ByteArray {
        val crop = image.cropRect
        val format = image.format
        val width = image.width
        val height = image.height

        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)

        var channelOffset = 0
        var outputStride = 1

        for(i in planes.indices){
            when(i){
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }

            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride

            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift

            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))

            for(row in 0 until h){
                var length: Int
                if(pixelStride == 1 && outputStride == 1)
                {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                }
                else
                {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w)
                    {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }

                if(row < h - 1){
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }

        return data
    }

    //https://stackoverflow.com/questions/52726002/camera2-captured-picture-conversion-from-yuv-420-888-to-nv21/52740776#52740776
    fun imageToByteArray2(image: Image): ByteArray {

        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4

        val nv21 = ByteArray(ySize + uvSize * 2)

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer


        val vSize = vBuffer.remaining()
        val newVByteArray = ByteArray(vSize)
        vBuffer.get(newVByteArray, 0, vSize)

        val newVBuffer = newVByteArray.toBuffer()

        var rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride === 1)

        var pos = 0

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        }
        else {
            var yBufferPos = -rowStride; // not an actual position

            while(pos < ySize){
                pos += width
                yBufferPos += rowStride;
                yBuffer.position(yBufferPos);
                yBuffer.get(nv21, pos, width);
            }

        }

        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride

        assert(rowStride == image.planes[1].rowStride)
        assert(pixelStride == image.planes[1].pixelStride)

        if (pixelStride == 2 && rowStride === width && uBuffer[0] == newVBuffer[1]) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            val savePixel = newVBuffer[1]
            try {
                newVBuffer.put(1, savePixel.inv())
                if (uBuffer[0] == savePixel.inv()) {
                    newVBuffer.put(1, savePixel)
                    newVBuffer.position(0)
                    uBuffer.position(0)
                    newVBuffer[nv21, ySize, 1]
                    uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            newVBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = newVBuffer[vuPos]
                nv21[pos++] = uBuffer[vuPos]
            }
        }

        return nv21
    }


    fun imageToByteArray(image: Image, outputBuffer: ByteArray) {
        assert(image.format == ImageFormat.YUV_420_888)

        val imageCrop = Rect(0, 0, image.width, image.height)
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }
}