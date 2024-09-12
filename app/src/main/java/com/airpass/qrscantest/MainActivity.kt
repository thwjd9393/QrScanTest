package com.airpass.qrscantest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageBitmap: Bitmap? = null
    private lateinit var btnCreate : Button
    private lateinit var openGalleryButton : Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnCreate = findViewById(R.id.btnCreate)
        openGalleryButton = findViewById(R.id.openGalleryButton)

        btnCreate.setOnClickListener {
            selectedImageBitmap?.let { bitmap ->
                GlobalScope.launch(Dispatchers.Main) {
                    val resizedBitmap = resizeBitmap(bitmap, 200, 200) // 이미지 리사이즈
                    val imagePath = withContext(Dispatchers.IO) {
                        saveImageToFile(resizedBitmap) // 리사이즈된 이미지를 파일로 저장
                    }
                    val compressedImagePath = withContext(Dispatchers.IO) {
                        compressImageWithFFmpeg(imagePath!!) // FFmpeg으로 이미지 압축
                    }
                    val intent = Intent(this@MainActivity, CreateQRActivity::class.java)
                    intent.putExtra("imagePath", compressedImagePath) // 압축된 파일 경로를 전달
                    startActivity(intent)
                }
            }
        }

        openGalleryButton.setOnClickListener {
            openGallery()
        }

    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val aspectRatio = width.toDouble() / height.toDouble()
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun saveImageToFile(bitmap: Bitmap): String? {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "resized_image.png")
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) // PNG 형식으로 저장
            outputStream.flush()
            outputStream.close()
            file.absolutePath // 파일 경로 반환
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImageWithFFmpeg(inputPath: String): String? {
        val outputPath = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "compressed_image.png").absolutePath
        val command = arrayOf(
            "-y", // 덮어쓰기 옵션
            "-i", inputPath, // 입력 파일
            "-vf", "scale=iw*0.4:ih*0.4", // 해상도 50%로 축소
            "-q:v", "1", // 높은 품질
            outputPath // 출력 파일
        )

        val result = FFmpeg.execute(command)
        return if (result == 0) {
            outputPath // 성공적으로 압축됨
        } else {
            Log.d("TAG","압축 실패")
            null // 압축 실패
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val imageUri = data?.data
            imageUri?.let {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
            }
        }
    }
}