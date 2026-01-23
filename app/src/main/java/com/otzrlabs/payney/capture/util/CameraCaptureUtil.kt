package com.otzrlabs.payney.capture.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Creates the content:// Uri the system camera app writes a captured receipt
 * photo to. Must be a content:// Uri backed by FileProvider (see
 * res/xml/file_paths.xml and the provider entry in AndroidManifest.xml) --
 * handing MediaStore.ACTION_IMAGE_CAPTURE a raw file:// Uri throws
 * FileUriExposedException on modern Android.
 */
object CameraCaptureUtil {

    private const val CAPTURES_DIR_NAME = "captures"
    private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    // Cap for the image actually uploaded for OCR -- receipts don't need more
    // resolution than this to read, and capping avoids shipping a multi-MB
    // camera-native photo over the network on every scan.
    private const val UPLOAD_MAX_DIMENSION = 2000
    private const val UPLOAD_JPEG_QUALITY = 90

    fun createCaptureUri(context: Context): Uri {
        val capturesDir = File(context.getExternalFilesDir(null), CAPTURES_DIR_NAME).apply { mkdirs() }
        val file = File(capturesDir, "receipt_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX,
            file,
        )
    }

    /**
     * Decodes [uri] downsampled to roughly [reqWidth]x[reqHeight] for a thumbnail
     * preview, corrected for EXIF orientation (see [applyExifOrientation]).
     */
    fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? =
        decodeOriented(context, uri, reqWidth, reqHeight)

    /**
     * Re-encodes [uri] as an upright JPEG (EXIF-corrected, capped to
     * [UPLOAD_MAX_DIMENSION]) for uploading to the receipt-extraction endpoint.
     * Without this, a photo whose orientation lives only in the EXIF tag -- which
     * BitmapFactory ignores -- would be sent to the server, and therefore to
     * Gemini's OCR, sideways.
     */
    fun readOrientedJpegBytes(context: Context, uri: Uri): ByteArray? {
        val bitmap = decodeOriented(context, uri, UPLOAD_MAX_DIMENSION, UPLOAD_MAX_DIMENSION) ?: return null
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, output)
            output.toByteArray()
        }
    }

    private fun decodeOriented(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Bounds-only pass: this deliberately returns a null Bitmap while
        // populating outWidth/outHeight, so the null-guard must be on whether
        // the *stream* opened -- guarding on the decode result (as before) made
        // this method always bail out here and never decode the real bitmap.
        val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        boundsOptions.inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
        boundsOptions.inJustDecodeBounds = false

        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return applyExifOrientation(bitmap, orientation)
    }

    // Many phone cameras save the sensor's native (often landscape) pixel data
    // unchanged and record how to display it upright in the EXIF orientation tag
    // instead of physically rotating it -- BitmapFactory ignores that tag, which
    // is why a naive decode comes out sideways. This applies it explicitly.
    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap // ORIENTATION_NORMAL / ORIENTATION_UNDEFINED -- nothing to do
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
