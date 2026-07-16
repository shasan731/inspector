package com.shahriarhasan.usedphoneinspector.core.database

import androidx.room.TypeConverter
import com.shahriarhasan.usedphoneinspector.core.model.ConditionGrade
import com.shahriarhasan.usedphoneinspector.core.model.DeviceCategory
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import com.shahriarhasan.usedphoneinspector.core.model.PhotoType
import com.shahriarhasan.usedphoneinspector.core.model.PhysicalCondition
import com.shahriarhasan.usedphoneinspector.core.model.TestCategory
import com.shahriarhasan.usedphoneinspector.core.model.TestStatus

class DatabaseConverters {
    @TypeConverter fun fromProfile(value: InspectionProfile): String = value.name
    @TypeConverter fun toProfile(value: String): InspectionProfile = InspectionProfile.valueOf(value)
    @TypeConverter fun fromInspectionStatus(value: InspectionStatus): String = value.name
    @TypeConverter fun toInspectionStatus(value: String): InspectionStatus = InspectionStatus.valueOf(value)
    @TypeConverter fun fromTestStatus(value: TestStatus): String = value.name
    @TypeConverter fun toTestStatus(value: String): TestStatus = TestStatus.valueOf(value)
    @TypeConverter fun fromCategory(value: TestCategory): String = value.name
    @TypeConverter fun toCategory(value: String): TestCategory = TestCategory.valueOf(value)
    @TypeConverter fun fromGrade(value: ConditionGrade?): String? = value?.name
    @TypeConverter fun toGrade(value: String?): ConditionGrade? = value?.let(ConditionGrade::valueOf)
    @TypeConverter fun fromDeviceCategory(value: DeviceCategory): String = value.name
    @TypeConverter fun toDeviceCategory(value: String): DeviceCategory = DeviceCategory.valueOf(value)
    @TypeConverter fun fromPhysicalCondition(value: PhysicalCondition): String = value.name
    @TypeConverter fun toPhysicalCondition(value: String): PhysicalCondition = PhysicalCondition.valueOf(value)
    @TypeConverter fun fromPhotoType(value: PhotoType): String = value.name
    @TypeConverter fun toPhotoType(value: String): PhotoType = PhotoType.valueOf(value)
}

