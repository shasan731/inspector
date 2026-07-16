package com.shahriarhasan.usedphoneinspector.core.reporting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import javax.inject.Inject

class ReportActions @Inject constructor() {
    fun share(context: Context, report: GeneratedReport) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", report.file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                report.filename,
            ),
        )
    }

    fun saveTo(context: Context, report: GeneratedReport, destination: Uri) {
        context.contentResolver.openOutputStream(destination, "w").use { output ->
            requireNotNull(output)
            report.file.inputStream().use { it.copyTo(output) }
        }
    }

    fun print(activity: Activity, report: GeneratedReport) {
        activity.getSystemService(PrintManager::class.java).print(
            report.filename,
            FilePrintAdapter(report.file, report.filename),
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
        )
    }
}

private class FilePrintAdapter(
    private val file: File,
    private val name: String,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) callback?.onLayoutCancelled()
        else callback?.onLayoutFinished(
            PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        if (destination == null || cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }
        runCatching {
            file.inputStream().use { input ->
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use(input::copyTo)
            }
        }.onSuccess { callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) }
            .onFailure { callback?.onWriteFailed(it.localizedMessage) }
    }
}

