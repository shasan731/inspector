package com.shahriarhasan.usedphoneinspector.core.database

import androidx.room.Embedded
import androidx.room.Relation

data class InspectionWithDetails(
    @Embedded val inspection: InspectionEntity,
    @Relation(parentColumn = "id", entityColumn = "inspection_id")
    val testResults: List<TestResultEntity>,
    @Relation(parentColumn = "id", entityColumn = "inspection_id")
    val physicalChecks: List<PhysicalCheckEntity>,
    @Relation(parentColumn = "id", entityColumn = "inspection_id")
    val sellers: List<SellerEntity>,
    @Relation(parentColumn = "id", entityColumn = "inspection_id")
    val photos: List<InspectionPhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "inspection_id")
    val snapshots: List<DeviceSnapshotEntity>,
    @Relation(parentColumn = "id", entityColumn = "inspection_id")
    val reports: List<ReportEntity>,
)

