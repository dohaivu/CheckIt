package com.checkit.ui.quicknote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Owns the camera [ActivityResultLauncher]s, which must be registered from
 * an Activity. Initialized once from MainActivity; the Koin-provided
 * [AndroidQuickNoteCameraCapture] delegates here so ViewModels stay
 * platform-agnostic.
 *
 * Captured JPEGs are downsampled (max 2048px) and converted to
 * WEBP_LOSSY (API 30+, lossy WEBP below) before the path is returned.
 */
object QuickNoteCameraHolder {
    private const val TAG = "QuickNoteCamera"
    private const val IMAGES_SUBDIR = "quicknote_images"
    private const val MAX_DIMENSION = 1280
    private const val WEBP_QUALITY = 80

    private var appContext: Context? = null
    private var takePicture: ActivityResultLauncher<Uri>? = null
    private var requestPermission: ActivityResultLauncher<String>? = null
    private var pendingFile: File? = null
    private var pendingCallback: ((String?) -> Unit)? = null

    fun init(activity: ComponentActivity) {
        appContext = activity.applicationContext
        requestPermission = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Log.w(TAG, "Camera permission denied")
                finish(null)
            }
        }
        takePicture = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                finish(convertToWebp())
            } else {
                finish(null)
            }
        }
    }

    fun capture(callback: (String?) -> Unit) {
        val context = appContext
        if (context == null || takePicture == null) {
            callback(null)
            return
        }
        pendingCallback = callback
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestPermission?.launch(Manifest.permission.CAMERA) ?: callback(null)
        }
    }

    private fun launchCamera() {
        val context = appContext ?: run { finish(null); return }
        return try {
            val dir = imagesDir(context).apply { mkdirs() }
            val tmp = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tmp,
            )
            pendingFile = tmp
            takePicture?.launch(uri) ?: finish(null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch camera", e)
            finish(null)
        }
    }

    private fun convertToWebp(): String? {
        val context = appContext ?: return null
        val src = pendingFile ?: return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > MAX_DIMENSION ||
                bounds.outHeight / sample > MAX_DIMENSION
            ) {
                sample *= 2
            }
            val bitmap = BitmapFactory.decodeFile(
                src.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return null
            val upright = applyExifRotation(src.absolutePath, bitmap)
            val out = File(imagesDir(context), "${UUID.randomUUID()}.webp")
            FileOutputStream(out).use { stream ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                upright.compress(format, WEBP_QUALITY, stream)
            }
            if (!upright.isRecycled) upright.recycle()
            src.delete()
            Log.d(TAG, "Captured ${out.length()} bytes at ${out.absolutePath}")
            out.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "WEBP conversion failed", e)
            null
        }
    }

    /**
     * Camera JPEGs store orientation in EXIF, which BitmapFactory ignores.
     * Physically rotates the pixels so the saved WEBP displays upright.
     */
    private fun applyExifRotation(path: String, bitmap: Bitmap): Bitmap {
        val degrees = try {
            when (
                ExifInterface(path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true)
        if (!bitmap.isRecycled) bitmap.recycle()
        return rotated
    }

    private fun finish(result: String?) {
        pendingFile?.takeIf { result == null }?.delete()
        pendingFile = null
        pendingCallback?.invoke(result)
        pendingCallback = null
    }

    private fun imagesDir(context: Context): File = File(context.filesDir, IMAGES_SUBDIR)
}

class AndroidQuickNoteCameraCapture : QuickNoteCameraCapture {
    override fun capture(onResult: (localPath: String?) -> Unit) =
        QuickNoteCameraHolder.capture(onResult)

    override fun discard(localPath: String) {
        runCatching { File(localPath).delete() }
    }
}
