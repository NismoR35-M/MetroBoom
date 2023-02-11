package com.metroN.boomingC.image

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Size
import com.metroN.boomingC.image.extractor.SquareFrameTransform
import com.metroN.boomingC.music.Song

/**
 * A utility to provide bitmaps in a race-less manner.
 *
 * When it comes to components that load images manually as [Bitmap] instances, queued
 * [ImageRequest]s may cause a race condition that results in the incorrect image being drawn. This
 * utility resolves this by keeping track of the current request, and disposing it as soon as a new
 * request is queued or if another, competing request is newer.
 *
 * @param context [Context] required to load images.
 */
class BitmapProvider(private val context: Context) {
    /**
     * An extension of [Disposable] with an additional [Target] to deliver the final [Bitmap] to.
     */
    private data class Request(val disposable: Disposable, val callback: Target)

    /** The target that will receive the requested [Bitmap]. */
    interface Target {
        /**
         * Configure the [ImageRequest.Builder] to enable [Target]-specific configuration.
         * @param builder The [ImageRequest.Builder] that will be used to request the desired
         * [Bitmap].
         * @return The same [ImageRequest.Builder] in order to easily chain configuration methods.
         */
        fun onConfigRequest(builder: ImageRequest.Builder): ImageRequest.Builder = builder

        /**
         * Called when the loading process is completed.
         * @param bitmap The loaded bitmap, or null if the bitmap could not be loaded.
         */
        fun onCompleted(bitmap: Bitmap?)
    }

    private var currentRequest: Request? = null
    private var currentHandle = 0L

    /** If this provider is currently attempting to load something. */
    val isBusy: Boolean
        get() = currentRequest?.run { !disposable.isDisposed } ?: false

    /**
     * Load the Album cover [Bitmap] from a [Song].
     * @param song The song to load a [Bitmap] of it's album cover from.
     * @param target The [Target] to deliver the [Bitmap] to asynchronously.
     */
    @Synchronized
    fun load(song: Song, target: Target) {
        // Increment the handle, indicating a newer request has been created
        val handle = ++currentHandle
        currentRequest?.run { disposable.dispose() }
        currentRequest = null

        val imageRequest =
            target
                .onConfigRequest(
                    ImageRequest.Builder(context)
                        .data(song)
                        // Use ORIGINAL sizing, as we are not loading into any View-like component.
                        .size(Size.ORIGINAL)
                        .transformations(SquareFrameTransform.INSTANCE))
                // Override the target in order to deliver the bitmap to the given
                // listener.
                .target(
                    onSuccess = {
                        synchronized(this) {
                            if (currentHandle == handle) {
                                // Has not been superceded by a new request, can deliver
                                // this result.
                                target.onCompleted(it.toBitmap())
                            }
                        }
                    },
                    onError = {
                        synchronized(this) {
                            if (currentHandle == handle) {
                                // Has not been superceded by a new request, can deliver
                                // this result.
                                target.onCompleted(null)
                            }
                        }
                    })
        currentRequest = Request(context.imageLoader.enqueue(imageRequest.build()), target)
    }

    /** Release this instance, cancelling any currently running operations. */
    @Synchronized
    fun release() {
        ++currentHandle
        currentRequest?.run { disposable.dispose() }
        currentRequest = null
    }
}
