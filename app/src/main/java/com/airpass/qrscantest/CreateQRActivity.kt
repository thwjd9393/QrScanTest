package com.airpass.qrscantest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class CreateQRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_qr)

        val imagePath = intent.getStringExtra("imagePath")
        imagePath?.let {
            GlobalScope.launch(Dispatchers.Main) {
                val file = File(it)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(it)
                    val base64Image = bitmapToBase64(bitmap) // Base64로 변환
                    val qrCodeBitmap = generateQRCodeFromText(base64Image) // QR 코드 생성
                    val imageViewQrCode: ImageView = findViewById(R.id.imageViewQrCode)
                    imageViewQrCode.setImageBitmap(qrCodeBitmap)
                }
            }
        }

    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream) // PNG 형식으로 변환
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun generateQRCodeFromText(text: String): Bitmap {
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400)
    }
}