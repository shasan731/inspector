package com.shahriarhasan.usedphoneinspector.core.reporting

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.shahriarhasan.usedphoneinspector.BuildConfig
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.billing.EntitlementPolicy
import com.shahriarhasan.usedphoneinspector.core.database.InspectionWithDetails
import com.shahriarhasan.usedphoneinspector.core.datastore.AppSettings
import com.shahriarhasan.usedphoneinspector.core.model.ScoreEngine
import com.shahriarhasan.usedphoneinspector.core.model.ScorableResult
import com.shahriarhasan.usedphoneinspector.core.model.TestCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeneratedReport(val file: File, val filename: String, val fullReport: Boolean)

interface PdfReportGenerator {
    suspend fun generate(
        details: InspectionWithDetails,
        isPro: Boolean,
        settings: AppSettings,
        localeTag: String,
    ): GeneratedReport
}

object ReportFilename {
    fun create(brand: String, model: String, completedAt: Long, reportId: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(completedAt))
        val shortId = reportId.filter(Char::isLetterOrDigit).takeLast(8).ifBlank { "local" }
        val safeBrand = sanitize(brand).ifBlank { "Device" }
        val safeModel = sanitize(model).ifBlank { "Unknown" }
        return "Inspection_${safeBrand}_${safeModel}_${date}_${shortId}.pdf"
    }

    fun sanitize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}._-]+"), "_")
        .trim('_', '.', ' ')
        .take(50)
}

@Singleton
class AndroidPdfReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) : PdfReportGenerator {
    override suspend fun generate(
        details: InspectionWithDetails,
        isPro: Boolean,
        settings: AppSettings,
        localeTag: String,
    ): GeneratedReport = withContext(Dispatchers.IO) {
        val locale = Locale.forLanguageTag(localeTag.ifBlank { Locale.getDefault().toLanguageTag() })
        val localizedConfiguration = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val localizedContext = context.createConfigurationContext(localizedConfiguration)
        val inspection = details.inspection
        val completedAt = inspection.completedAt ?: inspection.updatedAt
        val filename = ReportFilename.create(inspection.brand, inspection.model, completedAt, inspection.reportId.orEmpty())
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(directory, filename)
        val score = ScoreEngine.calculate(
            inspection.profile,
            details.testResults.filter { it.category !in setOf(TestCategory.IDENTITY, TestCategory.SELLER) }
                .map { ScorableResult(it.category, it.status, it.required) },
        )
        val document = PdfDocument()
        val writer = PdfWriter(document, localizedContext, !isPro)
        try {
            writer.heading(localizedContext.getString(R.string.report_title), 22f)
            writer.line(localizedContext.getString(R.string.report_id), inspection.reportId.orEmpty())
            writer.line(localizedContext.getString(R.string.inspection_date), formatDate(completedAt, locale))
            writer.line(localizedContext.getString(R.string.inspection_profile), localizedContext.getString(inspection.profile.labelRes))
            writer.line(localizedContext.getString(R.string.brand), inspection.brand)
            writer.line(localizedContext.getString(R.string.model), inspection.model)
            writer.line(localizedContext.getString(R.string.storage_variant), inspection.storageVariant)
            writer.line(localizedContext.getString(R.string.ram_variant), inspection.ramVariant)
            writer.line(localizedContext.getString(R.string.asking_price), "${inspection.askingPrice} ${inspection.currency}".trim())
            writer.line(localizedContext.getString(R.string.agreed_price), "${inspection.finalPrice} ${inspection.currency}".trim())
            if (settings.includeImei) {
                writer.line(localizedContext.getString(R.string.imei_one), inspection.imei1)
                writer.line(localizedContext.getString(R.string.imei_two), inspection.imei2)
                writer.line(localizedContext.getString(R.string.serial_number), inspection.serialNumber)
            }
            writer.heading(localizedContext.getString(R.string.condition_score))
            writer.line(localizedContext.getString(R.string.condition_score), localizedContext.getString(R.string.score_format, score.score))
            writer.line(localizedContext.getString(R.string.inspection_coverage), localizedContext.getString(R.string.coverage_format, score.coveragePercent))
            writer.line(localizedContext.getString(R.string.condition_grade), localizedContext.getString(score.grade.labelRes))
            if (score.coveragePercent < 60) writer.paragraph(localizedContext.getString(R.string.insufficient_coverage))

            if (isPro) {
                writer.heading(localizedContext.getString(R.string.review_results))
                score.categoryScores.forEach { (category, categoryScore) ->
                    writer.line(localizedContext.getString(category.labelRes), localizedContext.getString(R.string.score_format, categoryScore))
                }
                details.testResults.forEach { result ->
                    val definition = com.shahriarhasan.usedphoneinspector.core.model.InspectionProfiles.allTests
                        .firstOrNull { it.id == result.testId }
                    val label = definition?.let { localizedContext.getString(it.titleRes) } ?: result.testId
                    writer.line(label, localizedStatus(localizedContext, result.status))
                    if (result.notes.isNotBlank()) writer.paragraph(result.notes)
                    if (result.readingsJson != "{}") writer.paragraph(result.readingsJson)
                }
                writer.heading(localizedContext.getString(R.string.review_physical))
                details.physicalChecks.forEach { writer.line(it.itemKey, it.condition.name) }
                if (settings.includeSeller) {
                    details.sellers.firstOrNull()?.let { seller ->
                        writer.heading(localizedContext.getString(R.string.review_seller))
                        writer.line(localizedContext.getString(R.string.seller_name), seller.name)
                        writer.line(localizedContext.getString(R.string.business_name), seller.businessName)
                        writer.line(localizedContext.getString(R.string.phone_number), seller.phone)
                        writer.line(localizedContext.getString(R.string.address), seller.address)
                    }
                }
                details.snapshots.firstOrNull()?.let {
                    writer.heading(localizedContext.getString(R.string.device_information_title))
                    writer.paragraph(it.valuesJson)
                }
            } else {
                writer.paragraph(localizedContext.getString(R.string.report_watermark))
            }
            if (inspection.notes.isNotBlank()) {
                writer.heading(localizedContext.getString(R.string.notes))
                writer.paragraph(inspection.notes)
            }
            if (settings.includePhotos) {
                details.photos.filterNot { it.excludeFromReport }
                    .take(EntitlementPolicy.reportPhotoLimit(isPro))
                    .forEach { photo -> writer.image(File(photo.filePath), photo.description) }
            }
            writer.heading(localizedContext.getString(R.string.details))
            writer.line(localizedContext.getString(R.string.generated_at), formatDate(System.currentTimeMillis(), locale))
            writer.line(localizedContext.getString(R.string.app_version), BuildConfig.VERSION_NAME)
            writer.paragraph(localizedContext.getString(R.string.report_disclaimer))
            writer.finish()
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        GeneratedReport(file, filename, isPro)
    }

    private fun formatDate(time: Long, locale: Locale): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(time))

    private fun localizedStatus(context: Context, status: com.shahriarhasan.usedphoneinspector.core.model.TestStatus): String =
        context.getString(
            when (status) {
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.NOT_STARTED -> R.string.status_not_started
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.IN_PROGRESS -> R.string.status_in_progress
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.PASS -> R.string.status_pass
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.WARNING -> R.string.status_warning
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.FAIL -> R.string.status_fail
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.SKIPPED -> R.string.status_skipped
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.UNSUPPORTED -> R.string.status_unsupported
                com.shahriarhasan.usedphoneinspector.core.model.TestStatus.PERMISSION_DENIED -> R.string.status_permission_denied
            },
        )
}

private class PdfWriter(
    private val document: PdfDocument,
    private val context: Context,
    private val watermark: Boolean,
) {
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 42, 46)
        textSize = 10f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val headingPaint = Paint(bodyPaint).apply {
        color = Color.rgb(0, 88, 108)
        textSize = 15f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = TOP

    fun heading(text: String, size: Float = 15f) {
        ensureSpace(32f)
        val old = headingPaint.textSize
        headingPaint.textSize = size
        drawWrapped(text, headingPaint, lineHeight = size + 5f)
        headingPaint.textSize = old
        y += 6f
    }

    fun line(label: String, value: String) {
        if (value.isBlank()) return
        paragraph("$label: $value")
    }

    fun paragraph(text: String) {
        text.split('\n').forEach { drawWrapped(it, bodyPaint, 14f) }
        y += 3f
    }

    fun image(file: File, caption: String) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        try {
            val maxWidth = PAGE_WIDTH - LEFT - RIGHT
            val maxHeight = 300f
            val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height, 1f)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            ensureSpace(height + 30f)
            page?.canvas?.drawBitmap(bitmap, null, android.graphics.RectF(LEFT, y, LEFT + width, y + height), null)
            y += height + 5f
            if (caption.isNotBlank()) paragraph(caption)
        } finally {
            bitmap.recycle()
        }
    }

    fun finish() { closePage() }

    private fun drawWrapped(text: String, paint: Paint, lineHeight: Float) {
        var remaining = text.ifBlank { " " }
        while (remaining.isNotEmpty()) {
            ensureSpace(lineHeight)
            var count = paint.breakText(remaining, true, PAGE_WIDTH - LEFT - RIGHT, null).coerceAtLeast(1)
            if (count < remaining.length) {
                val breakAt = remaining.substring(0, count).lastIndexOf(' ')
                if (breakAt > 0) count = breakAt
            }
            val line = remaining.take(count).trim()
            page?.canvas?.drawText(line, LEFT, y, paint)
            y += lineHeight
            remaining = remaining.drop(count).trimStart()
        }
    }

    private fun ensureSpace(height: Float) {
        if (page == null) newPage()
        if (y + height > PAGE_HEIGHT - BOTTOM) {
            closePage()
            newPage()
        }
    }

    private fun newPage() {
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create())
        y = TOP
        page?.canvas?.drawText(context.getString(R.string.report_title), LEFT, 28f, headingPaint)
        if (watermark) {
            val watermarkPaint = Paint(bodyPaint).apply { color = Color.LTGRAY; textSize = 26f; alpha = 80 }
            page?.canvas?.save()
            page?.canvas?.rotate(-35f, PAGE_WIDTH / 2, PAGE_HEIGHT / 2)
            page?.canvas?.drawText(context.getString(R.string.report_watermark), 70f, PAGE_HEIGHT / 2, watermarkPaint)
            page?.canvas?.restore()
        }
    }

    private fun closePage() {
        val current = page ?: return
        current.canvas.drawText(context.getString(R.string.page_number, pageNumber), PAGE_WIDTH - 90f, PAGE_HEIGHT - 24f, bodyPaint)
        document.finishPage(current)
        page = null
    }

    private companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val LEFT = 44f
        const val RIGHT = 44f
        const val TOP = 52f
        const val BOTTOM = 44f
    }
}
