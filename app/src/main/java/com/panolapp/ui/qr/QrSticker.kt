package com.panolapp.ui.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.panolapp.domain.QrParser
import com.panolapp.domain.TabletSpecs
import java.io.File
import java.io.FileOutputStream

/** Genera la etiqueta QR de una tablet (CU-01) y la comparte como imagen PNG. */
object QrSticker {

    private const val LADO_QR = 800
    private const val ALTO_TEXTO = 150

    fun contenido(specs: TabletSpecs): String = QrParser.aJson(specs)

    /** QR + número de serie en texto legible debajo, listo para imprimir y pegar. */
    fun render(specs: TabletSpecs): Bitmap {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        )
        val matriz = QRCodeWriter().encode(contenido(specs), BarcodeFormat.QR_CODE, LADO_QR, LADO_QR, hints)
        val ancho = matriz.width
        val alto = matriz.height
        val pixeles = IntArray(ancho * alto)
        for (y in 0 until alto) {
            val fila = y * ancho
            for (x in 0 until ancho) {
                pixeles[fila + x] = if (matriz.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(ancho, alto + ALTO_TEXTO, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(Bitmap.createBitmap(pixeles, ancho, alto, Bitmap.Config.ARGB_8888), 0f, 0f, null)

        val pincelSerie = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pincelMarca = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(specs.serie, ancho / 2f, alto + 70f, pincelSerie)
        canvas.drawText("Panol-App", ancho / 2f, alto + 120f, pincelMarca)
        return bitmap
    }

    /** Escribe el PNG en la caché de la app y devuelve un Uri compartible (FileProvider). */
    fun guardarPng(contexto: Context, bitmap: Bitmap, serie: String): Uri {
        val carpeta = File(contexto.cacheDir, "qr").apply { mkdirs() }
        val nombre = "QR_" + serie.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".png"
        val archivo = File(carpeta, nombre)
        FileOutputStream(archivo).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        }
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
    }

    fun compartir(contexto: Context, uri: Uri, titulo: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(envio, titulo))
    }
}
