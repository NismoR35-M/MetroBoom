package com.metroN.boomingC.image.extractor

import android.graphics.Bitmap
import coil.size.Size
import coil.size.pxOrElse
import coil.transform.Transformation
import kotlin.math.min

/**
 * A transformation that performs a center crop-style transformation on an image. Allowing this
 * behavior to be intrinsic without any view configuration.
 */
class SquareFrameTransform : Transformation {
    override val cacheKey: String
        get() = "SquareFrameTransform"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Find the smaller dimension and then take a center portion of the image that
        // has that size.
        val dstSize = min(input.width, input.height)
        val x = (input.width - dstSize) / 2
        val y = (input.height - dstSize) / 2
        val dst = Bitmap.createBitmap(input, x, y, dstSize, dstSize)

        val desiredWidth = size.width.pxOrElse { dstSize }
        val desiredHeight = size.height.pxOrElse { dstSize }
        if (dstSize != desiredWidth || dstSize != desiredHeight) {
            // Image is not the desired size, upscale it.
            return Bitmap.createScaledBitmap(dst, desiredWidth, desiredHeight, true)
        }
        return dst
    }

    companion object {
        /** A re-usable instance. */
        val INSTANCE = SquareFrameTransform()
    }
}
