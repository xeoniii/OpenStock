package com.openstock.app.ui.sales

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.openstock.app.data.model.SaleGroup
import com.openstock.app.data.model.SaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object BillGenerator {

    suspend fun generateBills(
        context: Context,
        groups: List<SaleGroup>,
        viewModel: SalesViewModel
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (group in groups) {
            val file = generateSingleBill(context, group, viewModel)
            if (file != null) count++
        }
        count
    }

    suspend fun generateSingleBill(
        context: Context,
        group: SaleGroup,
        viewModel: SalesViewModel
    ): File? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val shopName = prefs.getString("shop_name", "OpenStock") ?: "OpenStock"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFolder = File(downloadsDir, "OpenStock_Bills")
        if (!outputFolder.exists()) outputFolder.mkdirs()

        val items = viewModel.getSaleItemsByGroupSync(group.id)
        if (items.isEmpty()) return@withContext null

        val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date(group.createdAt))
        val fileName = "${group.name.replace(" ", "_")}_${dateStr}_@OpenStock.pdf"
        val file = File(outputFolder, fileName)

        try {
            if (file.exists()) file.delete()
            
            val document = PdfDocument()
            val pageWidth = 226 // 80mm
            
            var calculatedTotal = 0.0
            for (item in items) {
                calculatedTotal += item.quantity * item.retailPriceAtSale
            }

            // Calculate height
            var estimatedHeight = 180 + (items.size * 45) + 120
            if (group.overrideTotalRetail != null) {
                estimatedHeight += 40 
            }
            
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, estimatedHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            
            val textPaint = TextPaint().apply { 
                textSize = 9f
                color = Color.BLACK
                typeface = Typeface.MONOSPACE
            }
            val titlePaint = TextPaint().apply { 
                textSize = 14f
                isFakeBoldText = true
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }
            val boldPaint = TextPaint().apply { 
                textSize = 9f
                isFakeBoldText = true
                color = Color.BLACK
                typeface = Typeface.MONOSPACE
            }

            var y = 30f
            canvas.drawText(shopName.uppercase(), (pageWidth / 2).toFloat(), y, titlePaint)
            y += 20f
            
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("RECEIPT", (pageWidth / 2).toFloat(), y, textPaint)
            y += 20f
            
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("SALE: ${group.name.uppercase()}", 10f, y, textPaint)
            y += 15f
            canvas.drawText("DATE: ${SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(group.createdAt))}", 10f, y, textPaint)
            y += 15f
            
            val dashPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            canvas.drawLine(10f, y, (pageWidth - 10).toFloat(), y, dashPaint)
            y += 15f

            for (item in items) {
                val product = viewModel.getProductById(item.productId)
                val unit = product?.unit ?: "pcs"
                val name = product?.name?.uppercase() ?: "PRODUCT"
                val lineTotal = item.quantity * item.retailPriceAtSale

                val staticLayout = StaticLayout.Builder.obtain(name, 0, name.length, boldPaint, pageWidth - 20)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                
                canvas.save()
                canvas.translate(10f, y)
                staticLayout.draw(canvas)
                canvas.restore()
                
                y += staticLayout.height + 10f

                val priceText = "${"%.1f".format(item.quantity)} $unit @ AED ${"%.2f".format(item.retailPriceAtSale)}"
                val totalText = "AED %.2f".format(lineTotal)
                
                canvas.drawText(priceText, 10f, y, textPaint)
                val totalWidth = textPaint.measureText(totalText)
                canvas.drawText(totalText, pageWidth - 10f - totalWidth, y, textPaint)
                
                y += 15f
            }

            y += 5f
            canvas.drawLine(10f, y, (pageWidth - 10).toFloat(), y, dashPaint)
            y += 20f
            
            val finalTotal = group.overrideTotalRetail ?: calculatedTotal
            
            if (group.overrideTotalRetail != null) {
                canvas.drawText("SUBTOTAL:", 10f, y, textPaint)
                val subtotalStr = "AED %.2f".format(calculatedTotal)
                canvas.drawText(subtotalStr, pageWidth - 10f - textPaint.measureText(subtotalStr), y, textPaint)
                y += 15f
                
                val diff = group.overrideTotalRetail - calculatedTotal
                if (diff < 0) {
                    canvas.drawText("DISCOUNT:", 10f, y, textPaint)
                    val discountStr = "-AED %.2f".format(Math.abs(diff))
                    canvas.drawText(discountStr, pageWidth - 10f - textPaint.measureText(discountStr), y, textPaint)
                    y += 15f
                } else if (diff > 0) {
                    canvas.drawText("FEES:", 10f, y, textPaint)
                    val feesStr = "+AED %.2f".format(diff)
                    canvas.drawText(feesStr, pageWidth - 10f - textPaint.measureText(feesStr), y, textPaint)
                    y += 15f
                }
            }

            boldPaint.textSize = 12f
            canvas.drawText("TOTAL:", 10f, y, boldPaint)
            val grandTotalValue = "AED %.2f".format(finalTotal)
            val valueWidth = boldPaint.measureText(grandTotalValue)
            canvas.drawText(grandTotalValue, pageWidth - 10f - valueWidth, y, boldPaint)
            
            y += 35f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("THANK YOU FOR PURCHASING WITH US!", (pageWidth / 2).toFloat(), y, textPaint)
            y += 15f
            canvas.drawText("Powered by OpenStock", (pageWidth / 2).toFloat(), y, textPaint)

            document.finishPage(page)
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            fos.close()
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
