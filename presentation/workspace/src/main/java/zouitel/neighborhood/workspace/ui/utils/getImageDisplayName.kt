package zouitel.neighborhood.workspace.ui.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun getImageDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)

    return context.applicationContext.contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            cursor.getString(columnIndex)
        } else {
            null
        }
    }
}

fun getVideoDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)

    return context.applicationContext.contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            cursor.getString(columnIndex)
        } else {
            null
        }
    }
}