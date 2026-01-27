package zouitel.neighborhood.ui.markdown

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private const val TAG = "ContextUtil"

fun Context.exportImage(
    activity: Activity,
    bitmap: Bitmap,
    fileName: String = "RikkaHub_${System.currentTimeMillis()}.png"
) {
    // 检查存储权限（Android 9及以下需要）
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            return
        }
    }

    // 保存到相册
    var outputStream: OutputStream? = null
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                outputStream = contentResolver.openOutputStream(it)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
            }
        } else {
            // Android 9及以下直接写入文件
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, fileName)
            outputStream = FileOutputStream(image)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

            // 通知图库更新
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(image)
            sendBroadcast(mediaScanIntent)
        }
        Log.i(TAG, "Image saved successfully: $fileName")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save image", e)
    } finally {
        outputStream?.close()
    }
}