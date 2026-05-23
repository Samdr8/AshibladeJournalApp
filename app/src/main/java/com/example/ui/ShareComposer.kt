package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Trade
import java.io.File
import java.io.FileOutputStream

object ShareComposer {
    fun shareTradeSetup(context: Context, trade: Trade) {
        try {
            // 1. Create target bitmap coordinates (1080 x 1440 - Portrait Social Aspect Ratio)
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 2. Draw luxury gradient background (Obsidian deep dark blue-black)
            val bgPaint = Paint().apply { isAntiAlias = true }
            val bgGradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#04091A"), Color.parseColor("#0F1A35"),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = bgGradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 3. Draw a glowing cyan-blue double outer border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
                alpha = 64 // Translucent neon look
            }
            canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)

            // Draw top card block header line
            val linePaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                strokeWidth = 2f
                isAntiAlias = true
                alpha = 40
            }
            canvas.drawLine(50f, 150f, width - 50f, 150f, linePaint)

            // 4. Header title: ASHIBLADE TRADING JOURNAL
            val brandPaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                textSize = 36f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                letterSpacing = 0.2f
            }
            canvas.drawText("ASHIBLADE", 60f, 95f, brandPaint)

            val modelPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("SYNTHETIC INDEX JOURNAL", 320f, 92f, modelPaint)

            // 5. Draw trade setup title & timestamp info
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 48f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val displaySetup = if (trade.setupName.isNotBlank()) trade.setupName else "Unlabeled Setup"
            canvas.drawText(displaySetup, 60f, 230f, titlePaint)

            val subPaint = Paint().apply {
                color = Color.parseColor("#90A4AE")
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("${trade.dateStr}  ${trade.timeStr}", 60f, 275f, subPaint)

            // Outcome badge
            val badgePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            val badgeTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 34f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val badgeColor = when (trade.outcome.uppercase()) {
                "WIN" -> "#00E676"
                "LOSS" -> "#FF3366"
                "BREAK-EVEN", "BE" -> "#90A4AE"
                else -> "#FFB300"
            }
            badgePaint.color = Color.parseColor(badgeColor)

            // Draw a rounded rectangle for outcomes on top right
            val badgeRect = RectF(width - 320f, 200f, width - 60f, 270f)
            canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)
            canvas.drawText(trade.outcome.uppercase(), width - 190f, 248f, badgeTextPaint)

            // 6. Draw grid of metrics: Instrument, Direction, Lot Size, Entry, SL, TP, P&L
            val pNLColor = if (trade.pnl >= 0) "#00E676" else "#FF3366"
            val displayPnl = String.format("$%.2f", trade.pnl)

            val gridItems = listOf(
                Pair("INSTRUMENT", trade.instrument),
                Pair("DIRECTION", trade.direction.uppercase()),
                Pair("LOT SIZE", trade.lotSize.toString()),
                Pair("DISCIPLINE GRADE", "${trade.grade}/10"),
                Pair("ENTRY PRICE", String.format("%.4f", trade.entryPrice)),
                Pair("STOP LOSS", if (trade.stopLoss > 0) String.format("%.4f", trade.stopLoss) else "None"),
                Pair("TAKE PROFIT", if (trade.takeProfit > 0) String.format("%.4f", trade.takeProfit) else "None"),
                Pair("RESULT P&L", displayPnl)
            )

            // Draw clean 2x4 card grid cells
            val cellWidth = 460f
            val cellHeight = 125f
            val startX = 60f
            val startY = 320f
            val labelColor = Color.parseColor("#90A4AE")

            val titleFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val regularFont = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

            gridItems.forEachIndexed { idx, pair ->
                val row = idx / 2
                val col = idx % 2
                val cx = startX + col * (cellWidth + 40f)
                val cy = startY + row * (cellHeight + 20f)

                // Render backgrounds
                val cellRect = RectF(cx, cy, cx + cellWidth, cy + cellHeight)
                val cellBgPaint = Paint().apply {
                    color = Color.parseColor("#142244")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    alpha = 130
                }
                canvas.drawRoundRect(cellRect, 12f, 12f, cellBgPaint)

                // Label Text
                val labelPaint = Paint().apply {
                    color = labelColor
                    textSize = 22f
                    typeface = regularFont
                    isAntiAlias = true
                }
                canvas.drawText(pair.first, cx + 25f, cy + 45f, labelPaint)

                // Value Text
                val valuePaint = Paint().apply {
                    color = if (pair.first == "RESULT P&L") Color.parseColor(pNLColor) else Color.WHITE
                    textSize = 34f
                    typeface = titleFont
                    isAntiAlias = true
                }
                canvas.drawText(pair.second, cx + 25f, cy + 95f, valuePaint)
            }

            // 7. Composite setup screenshot photo if available, otherwise draw a striking dark placeholder
            val imgY = 920f
            val imgW = 960f
            val imgH = 340f
            val imgRect = RectF(60f, imgY, 60f + imgW, imgY + imgH)

            var photoLoaded = false
            if (!trade.photoUri.isNullOrBlank()) {
                try {
                    val file = File(trade.photoUri)
                    if (file.exists()) {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        
                        // Calculate sample size
                        val outWidth = options.outWidth
                        val outHeight = options.outHeight
                        var scale = 1
                        while (outWidth / scale / 2 >= imgW && outHeight / scale / 2 >= imgH) {
                            scale *= 2
                        }
                        
                        options.inJustDecodeBounds = false
                        options.inSampleSize = scale
                        val srcBmp = BitmapFactory.decodeFile(file.absolutePath, options)
                        if (srcBmp != null) {
                            // Scale cropped or styled aspect
                            val scaledBmp = Bitmap.createScaledBitmap(srcBmp, imgW.toInt(), imgH.toInt(), true)
                            
                            // Draw with rounded corners using shader
                            val path = Path().apply {
                                addRoundRect(imgRect, 16f, 16f, Path.Direction.CW)
                            }
                            canvas.save()
                            canvas.clipPath(path)
                            canvas.drawBitmap(scaledBmp, 60f, imgY, null)
                            canvas.restore()
                            photoLoaded = true
                            srcBmp.recycle()
                            scaledBmp.recycle()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Ashiblade", "Error compositing photo", e)
                }
            }

            if (!photoLoaded) {
                // Draw a beautiful tech fallback placeholder
                val pBldPaint = Paint().apply {
                    color = Color.parseColor("#142244")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(imgRect, 16f, 16f, pBldPaint)

                // Subtle inner grid texture or mock candlesticks
                val decorPaint = Paint().apply {
                    color = Color.parseColor("#00E5FF")
                    strokeWidth = 2f
                    isAntiAlias = true
                    alpha = 25
                }
                // Draw decorative lines representing a simulated trading chart grid
                for (i in 1..4) {
                    val stepY = imgY + i * (imgH / 5)
                    canvas.drawLine(imgRect.left, stepY, imgRect.right, stepY, decorPaint)
                    val stepX = imgRect.left + i * (imgW / 5)
                    canvas.drawLine(stepX, imgRect.top, stepX, imgRect.bottom, decorPaint)
                }

                // Inner details
                val infoPaint = Paint().apply {
                    color = Color.parseColor("#90A4AE")
                    textSize = 28f
                    typeface = regularFont
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("No chart screenshot attached - direct ledger view", width / 2f, imgY + (imgH / 2f) + 10f, infoPaint)
            }

            // Draw a thin cyan line around the image/placeholder card
            val miniPaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
                alpha = 60
            }
            canvas.drawRoundRect(imgRect, 16f, 16f, miniPaint)

            // Draw small branding footer at the very bottom
            val footerPaint = Paint().apply {
                color = Color.parseColor("#90A4AE")
                textSize = 24f
                typeface = regularFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                alpha = 150
            }
            canvas.drawText("Empowered by Ashiblade Synthetic Suite", width / 2f, height - 50f, footerPaint)

            // 8. Save composite bitmap as file cache
            val cacheFolder = File(context.cacheDir, "shared")
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs()
            }
            val destinationFile = File(cacheFolder, "ashiblade_setup_${trade.id}.jpg")
            val outputStream = FileOutputStream(destinationFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            // 9. Create FileProvider sharing intent
            val contentUri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                destinationFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ashiblade Tech Setup - $displaySetup")
                putExtra(Intent.EXTRA_TEXT, "📊 Instrument: ${trade.instrument}\n📈 Direction: ${trade.direction.uppercase()}\n🎯 Yield: $displayPnl\n🔥 Sent via Ashiblade Portfolio Journal")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val intentChooser = Intent.createChooser(shareIntent, "Share Trading Set-up")
            intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intentChooser)

        } catch (e: Exception) {
            Log.e("Ashiblade", "Sharing failed", e)
            Toast.makeText(context, "Failed to compile image setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
