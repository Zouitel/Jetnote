package zouitel.neighborhood.audio.audio

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeByteArray
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import zouitel.neighborhood.audio.model.Audio
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class LocalMediaDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    private val projection: Array<String>
        get() = arrayOf(
            MediaStore.Audio.AudioColumns._ID,
            MediaStore.Audio.AudioColumns.DATE_ADDED,
            MediaStore.Audio.AudioColumns.DISPLAY_NAME,
            MediaStore.Audio.AudioColumns.DURATION,
            MediaStore.Audio.AudioColumns.SIZE
        )

    suspend fun loadAudioFiles(query: String): List<Audio> = withContext(ioDispatcher) {
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val result = mutableListOf<Audio>()

        contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                cursor.toLocalAudio(collectionUri)?.let(result::add)
            }
        }
        result
    }

    private fun Cursor.toLocalAudio(collectionUri: Uri): Audio? {
        return try {
            val idColumn = getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val titleColumn = getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
            val durationColumn = getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
            val sizeColumn = getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)

            val contentId = getLong(idColumn)
            val itemUri = ContentUris.withAppendedId(collectionUri, contentId)
            val title = getString(titleColumn)?.takeIf(String::isNotBlank) ?: return null

            Audio(
                id = contentId,
                path = itemUri.toString(),
                title = title,
                duration = getLong(durationColumn),
                size = getLong(sizeColumn),
                uri = itemUri.toString()
            )
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun getAudioThumbnail(uriOrPath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(uriOrPath)
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(uriOrPath)
            }
            retriever.embeddedPicture?.let { decodeByteArray(it, 0, it.size) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
