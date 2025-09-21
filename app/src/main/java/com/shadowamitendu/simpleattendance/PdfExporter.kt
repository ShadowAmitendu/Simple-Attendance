package com.shadowamitendu.simpleattendance

import android.app.Activity
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.graphics.toColorInt
import com.shadowamitendu.simpleattendance.data.StudentEntity
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter {

    fun exportAttendanceToUri(activity: Activity, students: List<StudentEntity>, uri: Uri, date: String) {
        val outputStream = activity.contentResolver.openOutputStream(uri)
            ?: throw Exception("Unable to open file for writing")

        val pdfDocument = PdfDocument()
        val pageWidth = 595   // A4 width
        val pageHeight = 842  // A4 height
        var pageNumber = 1

        // Modern color scheme
        val primaryColor = "#2E7D32".toColorInt() // Green
        val accentColor = "#4CAF50".toColorInt()  // Light green
        val textDark = "#212121".toColorInt()     // Dark gray
        val textLight = "#757575".toColorInt()    // Light gray
        val backgroundColor = "#F8F9FA".toColorInt() // Light background

        // Paint objects with modern styling
        val paintTitle = Paint().apply {
            textSize = 26f
            color = primaryColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            textSize = 14f
            color = textLight
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            textSize = 12f
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintText = Paint().apply {
            textSize = 11f
            color = textDark
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintPresent = Paint().apply {
            textSize = 11f
            color = "#2E7D32".toColorInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintAbsent = Paint().apply {
            textSize = 11f
            color = "#C62828".toColorInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintStats = Paint().apply {
            textSize = 11f
            color = textLight
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintFooter = Paint().apply {
            textSize = 10f
            color = textLight
            isAntiAlias = true
        }

        fun drawPageHeader(canvas: Canvas, pageNum: Int) {
            var y = 40

            // Title (no background box)
            canvas.drawText("ATTENDANCE REPORT", 50f, y + 20f, paintTitle)
            y += 35

            // Date and generation info
            canvas.drawText("Date: $date", 50f, y + 10f, paintSubtitle)
            val currentTime = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generated: $currentTime", 50f, y + 25f, paintSubtitle)

            y = 120

            // Statistics section (only on first page)
            if (pageNum == 1) {
                val presentCount = students.count { it.isPresent }
                val absentCount = students.size - presentCount
                val attendanceRate = if (students.isNotEmpty()) (presentCount * 100f / students.size) else 0f

                val statsRect = RectF(30f, y.toFloat(), pageWidth - 30f, y + 60f)
                val statsPaint = Paint().apply {
                    color = backgroundColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(statsRect, 8f, 8f, statsPaint)

                val borderPaint = Paint().apply {
                    color = accentColor
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(statsRect, 8f, 8f, borderPaint)

                val summaryPaint = Paint().apply {
                    textSize = 12f
                    color = primaryColor
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("SUMMARY", 50f, y + 20f, summaryPaint)

                val statsPaintGreen = Paint().apply {
                    textSize = 11f
                    color = "#2E7D32".toColorInt()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val statsPaintRed = Paint().apply {
                    textSize = 11f
                    color = "#C62828".toColorInt()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                canvas.drawText("Total Students: ${students.size}", 50f, y + 40f, paintStats)
                canvas.drawText("Present: $presentCount", 160f, y + 40f, statsPaintGreen)
                canvas.drawText("Absent: $absentCount", 250f, y + 40f, statsPaintRed)
                canvas.drawText("Attendance Rate: ${String.format("%.1f", attendanceRate)}%", 330f, y + 40f, paintStats)

                y += 80
            }

            // Table header with lighter background
            val tableHeaderRect = RectF(30f, y.toFloat(), pageWidth - 30f, y + 30f)
            val tableHeaderPaint = Paint().apply {
                color = "#E8F5E8".toColorInt() // Light green background
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(tableHeaderRect, 4f, 4f, tableHeaderPaint)

            val borderPaint = Paint().apply {
                color = primaryColor
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(tableHeaderRect, 4f, 4f, borderPaint)

            val headerTextPaint = Paint().apply {
                textSize = 12f
                color = primaryColor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            canvas.drawText("S.No.", 45f, y + 20f, headerTextPaint)
            canvas.drawText("Roll No.", 90f, y + 20f, headerTextPaint)
            canvas.drawText("Student Name", 180f, y + 20f, headerTextPaint)
            canvas.drawText("Status", 480f, y + 20f, headerTextPaint)
        }

        // Start first page
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        drawPageHeader(canvas, pageNumber)
        var y = if (pageNumber == 1) 240 else 190 // Added extra space for first page, adjusted for subsequent pages
        var serialNumber = 1

        for (student in students) {
            // Check if we need a new page
            if (y > pageHeight - 70) {
                // Footer for current page
                canvas.drawText("Simple Attendance App", 50f, pageHeight - 30f, paintFooter)
                canvas.drawText("Page $pageNumber", pageWidth - 100f, pageHeight - 30f, paintFooter)

                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawPageHeader(canvas, pageNumber)
                y = 190
            }

            // Alternating row colors
            if (serialNumber % 2 == 0) {
                val rowRect = RectF(30f, y - 6f, pageWidth - 30f, y + 18f)
                val rowPaint = Paint().apply {
                    color = "#F8F9FA".toColorInt()
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rowRect, rowPaint)
            }

            // Row content with improved spacing
            canvas.drawText("$serialNumber.", 45f, y + 8f, paintText)

            // Roll number (truncated if too long)
            val displayRoll = if (student.roll.length > 12) {
                student.roll.substring(0, 9) + "..."
            } else {
                student.roll
            }
            canvas.drawText(displayRoll, 90f, y + 8f, paintText)

            // Student name (truncated if too long)
            val displayName = if (student.name.length > 28) {
                student.name.substring(0, 25) + "..."
            } else {
                student.name.uppercase()
            }
            canvas.drawText(displayName, 180f, y + 8f, paintText)

            // Status with different colors and dot indicator
            val status = if (student.isPresent) "Present" else "Absent"
            val statusPaint = if (student.isPresent) paintPresent else paintAbsent
            canvas.drawText(status, 480f, y + 8f, statusPaint)

            // Status indicator dot
            val dotPaint = Paint().apply {
                color = if (student.isPresent) "#4CAF50".toColorInt() else "#F44336".toColorInt()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(465f, y + 4f, 3.5f, dotPaint)

            y += 22
            serialNumber++
        }

        // Footer for last page
        canvas.drawText("Simple Attendance App", 50f, pageHeight - 30f, paintFooter)
        canvas.drawText("Page $pageNumber", pageWidth - 100f, pageHeight - 30f, paintFooter)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()
    }

    // Fallback method for direct file saving
    fun exportAttendance(context: android.content.Context, students: List<StudentEntity>, date: String) {
        val file = java.io.File(context.getExternalFilesDir(null), "Attendance_$date.pdf")

        try {
            val outputStream = java.io.FileOutputStream(file)

            // Create a mock URI for the file-based export
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Use similar logic as exportAttendanceToUri but with FileOutputStream
            // This is a simplified version for backward compatibility
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitle = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = "#2E7D32".toColorInt()
            }
            val paintText = Paint().apply {
                textSize = 12f
                color = "#212121".toColorInt()
            }

            var y = 50
            canvas.drawText("Attendance - $date", 50f, y.toFloat(), paintTitle)
            y += 40

            canvas.drawText("S.No.  Roll No.       Student Name                    Status", 50f, y.toFloat(), paintText)
            y += 30

            var serialNumber = 1
            for (student in students) {
                if (y > pageHeight - 50) break // Simple page overflow handling

                val status = if (student.isPresent) "Present" else "Absent"
                val line = "${serialNumber}.     ${student.roll}       ${student.name}       $status"
                canvas.drawText(line, 50f, y.toFloat(), paintText)
                y += 20
                serialNumber++
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            android.widget.Toast.makeText(context, "PDF saved: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Error saving PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}