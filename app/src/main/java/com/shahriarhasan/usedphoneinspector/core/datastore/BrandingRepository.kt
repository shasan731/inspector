package com.shahriarhasan.usedphoneinspector.core.datastore

import android.content.Context
import android.net.Uri
import com.shahriarhasan.usedphoneinspector.core.database.BrandingProfileEntity
import com.shahriarhasan.usedphoneinspector.core.database.InspectionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val EmptyBranding = BrandingProfileEntity(
    businessName = "",
    logoPath = "",
    address = "",
    phone = "",
    email = "",
    website = "",
    reportTitle = "",
    footerText = "",
)

interface BrandingRepository {
    val branding: Flow<BrandingProfileEntity>
    suspend fun update(profile: BrandingProfileEntity)
    suspend fun updateLogo(uri: Uri): String
}

@Singleton
class LocalBrandingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: InspectionDao,
) : BrandingRepository {
    override val branding: Flow<BrandingProfileEntity> = dao.observeBranding().map { it ?: EmptyBranding }
    override suspend fun update(profile: BrandingProfileEntity) = dao.upsertBranding(profile.copy(id = "default"))
    override suspend fun updateLogo(uri: Uri): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "branding").apply { mkdirs() }
        val target = File(directory, "logo")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            target.outputStream().use(input::copyTo)
        }
        require(target.length() in 1..5L * 1024 * 1024) { "Invalid logo" }
        target.absolutePath
    }
}

