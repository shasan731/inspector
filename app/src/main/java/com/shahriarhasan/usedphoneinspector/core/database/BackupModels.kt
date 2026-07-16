package com.shahriarhasan.usedphoneinspector.core.database

import kotlinx.serialization.Serializable

@Serializable
data class InspectionBackup(
    val formatVersion: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val inspection: InspectionEntitySurrogate,
    val tests: List<TestResultSurrogate>,
    val physicalChecks: List<PhysicalCheckSurrogate>,
    val seller: SellerSurrogate?,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class InspectionEntitySurrogate(
    val id: String,
    val profile: String,
    val status: String,
    val createdAt: Long,
    val completedAt: Long?,
    val reportId: String?,
    val brand: String,
    val model: String,
    val deviceCategory: String,
    val color: String,
    val storageVariant: String,
    val ramVariant: String,
    val askingPrice: String,
    val finalPrice: String,
    val currency: String,
    val purchaseSource: String,
    val imei1: String,
    val imei2: String,
    val serialNumber: String,
    val conditionScore: Int?,
    val coveragePercent: Int?,
    val grade: String?,
    val notes: String,
)

@Serializable
data class TestResultSurrogate(
    val testId: String,
    val category: String,
    val status: String,
    val required: Boolean,
    val notes: String,
    val issuesJson: String,
    val readingsJson: String,
    val durationMillis: Long,
)

@Serializable
data class PhysicalCheckSurrogate(
    val itemKey: String,
    val categoryKey: String,
    val condition: String,
    val notes: String,
)

@Serializable
data class SellerSurrogate(
    val name: String,
    val phone: String,
    val alternatePhone: String,
    val email: String,
    val businessName: String,
    val location: String,
    val address: String,
    val paymentMethod: String,
    val warranty: String,
    val purchaseDate: String,
    val sellerNotes: String,
    val buyerNotes: String,
    val repairDetailsJson: String,
)

