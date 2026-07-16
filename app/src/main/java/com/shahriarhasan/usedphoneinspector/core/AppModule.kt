package com.shahriarhasan.usedphoneinspector.core

import android.content.Context
import androidx.room.Room
import com.shahriarhasan.usedphoneinspector.BuildConfig
import com.shahriarhasan.usedphoneinspector.core.billing.BillingRepository
import com.shahriarhasan.usedphoneinspector.core.billing.FakeBillingRepository
import com.shahriarhasan.usedphoneinspector.core.billing.PlayBillingRepository
import com.shahriarhasan.usedphoneinspector.core.database.AppDatabase
import com.shahriarhasan.usedphoneinspector.core.database.InspectionDao
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.database.RoomInspectionRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.DataStoreSettingsRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.BrandingRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.LocalBrandingRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.SettingsRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidBatteryRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidCameraRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidConnectivityRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidDeviceInfoRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidSensorRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidTelephonyRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.AndroidVibrationRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.BatteryRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.CameraXTestController
import com.shahriarhasan.usedphoneinspector.core.hardware.ConnectivityRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.DeviceInfoRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.SensorRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.TelephonyRepository
import com.shahriarhasan.usedphoneinspector.core.hardware.VibrationRepository
import com.shahriarhasan.usedphoneinspector.core.reporting.EvidenceRepository
import com.shahriarhasan.usedphoneinspector.core.reporting.LocalEvidenceRepository
import com.shahriarhasan.usedphoneinspector.core.reporting.AndroidPdfReportGenerator
import com.shahriarhasan.usedphoneinspector.core.reporting.PdfReportGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds abstract fun bindInspectionRepository(implementation: RoomInspectionRepository): InspectionRepository
    @Binds abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun bindBrandingRepository(implementation: LocalBrandingRepository): BrandingRepository
    @Binds abstract fun bindDeviceInfoRepository(implementation: AndroidDeviceInfoRepository): DeviceInfoRepository
    @Binds abstract fun bindBatteryRepository(implementation: AndroidBatteryRepository): BatteryRepository
    @Binds abstract fun bindSensorRepository(implementation: AndroidSensorRepository): SensorRepository
    @Binds abstract fun bindConnectivityRepository(implementation: AndroidConnectivityRepository): ConnectivityRepository
    @Binds abstract fun bindTelephonyRepository(implementation: AndroidTelephonyRepository): TelephonyRepository
    @Binds abstract fun bindVibrationRepository(implementation: AndroidVibrationRepository): VibrationRepository
    @Binds abstract fun bindCameraRepository(implementation: AndroidCameraRepository): CameraRepository
    @Binds abstract fun bindCameraTestController(implementation: CameraXTestController): CameraTestController
    @Binds abstract fun bindEvidenceRepository(implementation: LocalEvidenceRepository): EvidenceRepository
    @Binds abstract fun bindPdfReportGenerator(implementation: AndroidPdfReportGenerator): PdfReportGenerator
}


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "inspection.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideInspectionDao(database: AppDatabase): InspectionDao = database.inspectionDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideBillingRepository(
        fake: FakeBillingRepository,
        real: PlayBillingRepository,
    ): BillingRepository = if (BuildConfig.USE_FAKE_BILLING) fake else real
}
