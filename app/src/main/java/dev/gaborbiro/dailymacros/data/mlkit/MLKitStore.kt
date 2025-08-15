package dev.gaborbiro.dailymacros.data.mlkit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks.await
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.file.FileStore
import dev.gaborbiro.dailymacros.data.image.ImageStore
import kotlin.math.max

internal class MLKitStore(
    private val fileStore: FileStore,
    private val imageStore: ImageStore,
) {
    private lateinit var llm: LlmInference

    private val sessionOpts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setTemperature(0.0f)
        .setTopK(1)
        .setGraphOptions(
            GraphOptions.builder()
                .setEnableVisionModality(true)   // Gemma-3n *.task only
                .build()
        )
        .build()

    fun init() {
        val opts = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(fileStore.resolveFilePath("gemma-3n-E2B-it-int4.task"))
            .setMaxTokens(320)
            .setMaxNumImages(1)
            .build()
        llm = LlmInference.createFromOptions(App.appContext, opts)
    }

    fun fastTitleFromImage(imageFilename: String): List<Pair<String, Float>> {
        val bmp: Bitmap =
            decodeSoftwareArgb(imageFilename, maxSide = 1280)   // <— safe, rotated, downscaled
        val image = InputImage.fromBitmap(bmp, 0)
        val model = LocalModel.Builder()
            .setAssetFilePath("1.tflite")
            .build()
        val options = CustomImageLabelerOptions.Builder(model)
            .setConfidenceThreshold(0.0f)
            .setMaxResultCount(5)
            .build()
        val labeler = ImageLabeling.getClient(options)
        return await(labeler.process(image))
            .map { it.text to it.confidence }
    }

    fun titleFromImage(imageFilename: String): String {
        val bmp: Bitmap =
            decodeSoftwareArgb(imageFilename, maxSide = 1280)   // <— safe, rotated, downscaled
        val mpImage: MPImage = BitmapImageBuilder(bmp).build()

        return LlmInferenceSession.createFromOptions(llm, sessionOpts).use { session ->
            // Text first, then image tends to work best for Gemma-3n
            session.addQueryChunk("2–4 word meal title only")
            session.addImage(mpImage)
            session.generateResponse().trim()
        }
    }

    /** Decode as SOFTWARE ARGB_8888, apply EXIF rotation, optionally downscale. */
    private fun decodeSoftwareArgb(imageFilename: String, maxSide: Int): Bitmap {
        // 1) Bounds pass to compute inSampleSize
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        imageStore.read(imageFilename, thumbnail = true)
        imageStore.get(imageFilename, thumbnail = true)
            .use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        val (w, h) = bounds.outWidth to bounds.outHeight
        val sample = computeInSampleSize(w, h, maxSide)

        // 2) Real decode as ARGB_8888 (software)
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inSampleSize = sample
        }
        val decoded = imageStore.get(imageFilename, thumbnail = true)
            .use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            ?: error("Failed to decode bitmap: $imageFilename")

        // 3) Apply EXIF orientation (common for camera photos)
        val rotated = applyExifRotation(decoded, imageFilename)

        // 4) Ensure software & ARGB_8888 (some decoders still hand over hardware on newer APIs)
        return ensureSoftwareArgb8888(rotated)
    }

    private fun computeInSampleSize(w: Int, h: Int, maxSide: Int): Int {
        if (w <= 0 || h <= 0) return 1
        var sample = 1
        var mw = w
        var mh = h
        while (max(mw, mh) / 2 >= maxSide) {
            mw /= 2; mh /= 2; sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun applyExifRotation(src: Bitmap, imageFilename: String): Bitmap {
        return try {
            val exif =
                ExifInterface(fileStore.resolveFilePath(imageFilename))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                }
            }
            if (!matrix.isIdentity) Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true) else src
        } catch (_: Throwable) {
            src
        }
    }

    private fun ensureSoftwareArgb8888(bmp: Bitmap): Bitmap {
        // Reject hardware bitmaps or non-8888 configs
        val needsCopy =
            bmp.config != Bitmap.Config.ARGB_8888 || (bmp.config == Bitmap.Config.HARDWARE)
        if (!needsCopy) return bmp
        val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(bmp, 0f, 0f, null)
        return out
    }
}
