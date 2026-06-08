package com.example.bbc.logic

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.bbc.domain.ModelParameters
import com.example.bbc.domain.ReliabilityTestRecord
import com.example.bbc.domain.SRGMType
import java.io.File
import java.io.FileOutputStream

interface PdfExporter {
    fun exportReport(
        records: List<ReliabilityTestRecord>,
        selectedModel: SRGMType,
        params: ModelParameters,
        summaryContent: String
    )
}

class AndroidPdfExporter(private val context: Context) : PdfExporter {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 50f
    private val contentWidth = pageWidth - 2 * margin

    override fun exportReport(
        records: List<ReliabilityTestRecord>,
        selectedModel: SRGMType,
        params: ModelParameters,
        summaryContent: String
    ) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var yPos = margin

            yPos = drawHeader(canvas, yPos)
            yPos = drawTitle(canvas, "SRGM НАДЕЖДНОСТ — АНАЛИЗАТОРСКИ ОТЧЕТ", yPos)
            yPos += 20f

            val sections = parseSections(summaryContent)
            for ((heading, body) in sections) {
                val neededHeight = estimateSectionHeight(heading, body)
                if (yPos + neededHeight > pageHeight - margin) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = margin
                }
                yPos = drawSection(canvas, heading, body, yPos)
            }

            drawFooter(canvas, pageNumber)
            document.finishPage(page)

            val file = File(context.cacheDir, "Отчет.pdf")
            FileOutputStream(file).use { document.writeTo(it) }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Запазване на Отчет.pdf").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }

    private fun drawHeader(canvas: Canvas, yPos: Float): Float {
        val paint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            style = Paint.Style.FILL
        }
        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 4f, paint)
        return yPos + 16f
    }

    private fun drawTitle(canvas: Canvas, text: String, yPos: Float): Float {
        val paint = Paint().apply {
            color = Color.rgb(20, 20, 80)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(text, margin, yPos + paint.textSize, paint)
        return yPos + paint.textSize + 8f
    }

    private fun drawSection(canvas: Canvas, heading: String, body: String, startY: Float): Float {
        var yPos = startY + 12f

        val headingPaint = Paint().apply {
            color = Color.rgb(20, 20, 80)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(heading, margin, yPos + headingPaint.textSize, headingPaint)
        yPos += headingPaint.textSize + 4f

        val dividerPaint = Paint().apply {
            color = Color.rgb(180, 180, 220)
            strokeWidth = 1f
        }
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, dividerPaint)
        yPos += 8f

        val bodyPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 9.5f
            isAntiAlias = true
        }

        for (line in body.lines()) {
            val wrapped = wrapText(line.trim(), bodyPaint, contentWidth)
            for (wrappedLine in wrapped) {
                canvas.drawText(wrappedLine, margin, yPos + bodyPaint.textSize, bodyPaint)
                yPos += bodyPaint.textSize + 4f
            }
        }

        return yPos + 4f
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val paint = Paint().apply {
            color = Color.rgb(130, 130, 130)
            textSize = 8f
            isAntiAlias = true
        }
        val text = "BBC Надеждност  •  Страница $pageNumber"
        canvas.drawText(text, margin, pageHeight - 20f, paint)
    }

    private fun parseSections(content: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val sectionRegex = Regex("""(\d+\.\s+[^\n]+)\n[-=]+\n([\s\S]*?)(?=\n\d+\.\s|\Z)""")
        for (match in sectionRegex.findAll(content)) {
            val heading = match.groupValues[1].trim()
            val body = match.groupValues[2].trim()
            result.add(heading to body)
        }
        if (result.isEmpty()) {
            result.add("ОТЧЕТ" to content.trim())
        }
        return result
    }

    private fun estimateSectionHeight(heading: String, body: String): Float {
        val lineHeight = 9.5f + 4f
        val lineCount = body.lines().size.coerceAtLeast(1)
        return 12f + 11f + 4f + 8f + lineCount * lineHeight + 4f + 16f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf("") }
    }
}

@Composable
fun rememberPdfExporter(): PdfExporter {
    val context = LocalContext.current
    return remember(context) { AndroidPdfExporter(context) }
}
