package com.example.cameraalbumtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.example.cameraalbumtest.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {


    private lateinit var mBinding: ActivityMainBinding

    private val takePhoto = 1
    private lateinit var mOutputImage: File
    private lateinit var mOutputImage2: File
    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        checkPermission()
        val startForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { uri ->
            if (uri.resultCode == RESULT_OK) {
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                mBinding.imageView.setImageBitmap(rotateIfRequired(bitmap, mOutputImage.path))
            }
        }
        val startToAlbum =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
                result?.let { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    inputStream?.let {
                        val file = writeInputStreamToFile(it, "output2_image.jpg")
                        Log.d("TAG", "onCreate: ${file.path}")
                        val bitmap =
                            BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                        mBinding.imageView.setImageBitmap(rotateIfRequired(bitmap, file.path))
                    }
                }
            }
        mBinding.takePhotoBtn.setOnClickListener {
            mOutputImage = File(externalCacheDir, "output_image.jpg")
            if (mOutputImage.exists()) {
                mOutputImage.delete()
            }
            mOutputImage.createNewFile()
            imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this, "com.example.cameraalbumtest.fileprovider", mOutputImage
                )
            } else {
                Uri.fromFile(mOutputImage)
            }
            val intent = Intent("android.media.action.IMAGE_CAPTURE")
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startForResult.launch(intent)
        }


        mBinding.fromAlbumBtn.setOnClickListener {
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//            intent.addCategory(Intent.CATEGORY_OPENABLE)
//            // 指定只显示图片
//            intent.type = "image/*"
//            startForResult.launch(intent)
            startToAlbum.launch(arrayOf("image/*"))
        }


    }


    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }


    private fun writeInputStreamToFile(inputStream: InputStream, fileName: String): File {
        val file = File(externalCacheDir, fileName).apply {
            if (exists()) {
                delete()
            }
            createNewFile()
        }
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outputStream?.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return file
    }

    /**
     * Bitmap to file
     * "output2_image.jpg"
     *
     * @param fileName
     */
    private fun bitmapToFile(bitmap: Bitmap, fileName: String) {
        mOutputImage2 = File(externalCacheDir, fileName).apply {
            if (exists()) {
                delete()
            }
            createNewFile()
        }
        FileOutputStream(mOutputImage2).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun rotateIfRequired(bitmap: Bitmap, path: String): Bitmap {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotateBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotateBitmap
    }

}