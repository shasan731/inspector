package com.shahriarhasan.usedphoneinspector.core.reporting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.shahriarhasan.usedphoneinspector.core.database.InspectionDao
import com.shahriarhasan.usedphoneinspector.core.database.InspectionPhotoEntity
import com.shahriarhasan.usedphoneinspector.core.model.PhotoType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface EvidenceRepository {
    suspend fun storeFile(inspectionId: String, source: File, type: PhotoType, description: String = ""): InspectionPhotoEntity
    suspend fun importUri(inspectionId: String, uri: Uri, type: PhotoType): InspectionPhotoEntity
    suspend fun delete(photo: InspectionPhotoEntity)
}

@Singleton
class LocalEvidenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: InspectionDao,
) : EvidenceRepository {
    override suspend fun storeFile(
        inspectionId: String,
        source: File,
        type: PhotoType,
        description: String,
    ): InspectionPhotoEntity = withContext(Dispatchers.IO) {
        require(source.exists() && source.length() in 1..MAX_IMPORT_BYTES) { "Invalid evidence file" }
        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "evidence/$inspectionId").apply { mkdirs() }
        val target = File(directory, "$id.jpg")
        val thumbnail = File(directory, "$id-thumb.jpg")
        val bitmap = decodeScaled(source, 2_048) ?: throw IllegalArgumentException("Unsupported image")
        val rotated = rotateFromExif(bitmap, source)
        target.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        val thumb = scaleInside(rotated, 384)
        thumbnail.outputStream().use { thumb.compress(Bitmap.CompressFormat.JPEG, 78, it) }
        if (thumb !== rotated) thumb.recycle()
        if (rotated !== bitmap) rotated.recycle()
        bitmap.recycle()
        val currentCount = dao.getInspectionWithDetails(inspectionId)?.photos?.size ?: 0
        val entity = InspectionPhotoEntity(
            id = id,
            inspectionId = inspectionId,
            type = type,
            filePath = target.absolutePath,
            thumbnailPath = thumbnail.absolutePath,
            description = description.take(200),
            excludeFromReport = false,
            sortOrder = currentCount,
            createdAt = System.currentTimeMillis(),
        )
        dao.upsertPhoto(entity)
        entity
    }

    override suspend fun importUri(inspectionId: String, uri: Uri, type: PhotoType): InspectionPhotoEntity =
        withContext(Dispatchers.IO) {
            val temp = File(context.cacheDir, "import-${UUID.randomUUID()}.tmp")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open image" }
                temp.outputStream().use { output -> input.copyTo(output, bufferSize = 64 * 1024) }
            }
            try { storeFile(inspectionId, temp, type) } finally { temp.delete() }
        }

    override suspend fun delete(photo: InspectionPhotoEntity) = withContext(Dispatchers.IO) {
        File(photo.filePath).delete()
        File(photo.thumbnailPath).delete()
        dao.deletePhoto(photo)
    }

    private fun decodeScaled(file: File, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun rotateFromExif(bitmap: Bitmap, file: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        return if (degrees == 0f) bitmap else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true,
        )
    }

    private fun scaleInside(bitmap: Bitmap, maximum: Int): Bitmap {
        val scale = minOf(maximum.toFloat() / bitmap.width, maximum.toFloat() / bitmap.height, 1f)
        return if (scale == 1f) bitmap else Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true,
        )
    }

    private companion object { const val MAX_IMPORT_BYTES = 40L * 1024 * 1024 }
}

