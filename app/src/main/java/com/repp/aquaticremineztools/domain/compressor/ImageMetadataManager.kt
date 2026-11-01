package com.repp.aquaticremineztools.domain.compressor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

object ImageMetadataManager {

    fun getExifOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        } catch (e: Exception) {
            e.printStackTrace()
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }

    fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
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
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    fun stripGpsMetadata(outputFile: File) {
        try {
            val exif = ExifInterface(outputFile.absolutePath)
            val gpsTags = arrayOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_SPEED,
                ExifInterface.TAG_GPS_SPEED_REF,
                ExifInterface.TAG_GPS_TRACK,
                ExifInterface.TAG_GPS_TRACK_REF,
                ExifInterface.TAG_GPS_IMG_DIRECTION,
                ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                ExifInterface.TAG_GPS_MAP_DATUM,
                ExifInterface.TAG_GPS_DEST_LATITUDE,
                ExifInterface.TAG_GPS_DEST_LONGITUDE
            )
            for (tag in gpsTags) {
                exif.setAttribute(tag, null)
            }
            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
