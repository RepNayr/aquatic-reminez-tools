package com.repp.aquaticremineztools.domain.compressor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

object ImageTransformHelper {

    fun rotate(sourceFile: File, degrees: Float): File? {
        return transform(sourceFile) { bitmap ->
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    fun flip(sourceFile: File, horizontal: Boolean): File? {
        return transform(sourceFile) { bitmap ->
            val scaleX = if (horizontal) -1f else 1f
            val scaleY = if (!horizontal) -1f else 1f
            val matrix = Matrix().apply { postScale(scaleX, scaleY) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    private inline fun transform(sourceFile: File, transformAction: (Bitmap) -> Bitmap): File? {
        if (!sourceFile.exists()) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
            val transformed = transformAction(bitmap)
            if (transformed != bitmap) {
                bitmap.recycle()
            }
            val outFile = File.createTempFile("transformed_", ".jpg", sourceFile.parentFile)
            FileOutputStream(outFile).use { out ->
                transformed.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            transformed.recycle()
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
