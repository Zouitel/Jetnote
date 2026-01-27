package zouitel.neighborhood.workspace.ui.features.details

import android.media.MediaMetadataRetriever
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.SettingsScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@Composable
fun VideoDetails(path: String?, onClose: () -> Unit) {
    if (path == null) return
    val context = LocalContext.current

    val settingsModel = koinInject<SettingsScreenModel>()
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    var videoProperties by remember { mutableStateOf<VideoProperties?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
    }.getOrElse {
        path.toUri()
    }

    LaunchedEffect(uri) {
        if (uri?.path.isNullOrEmpty()) {
            videoProperties = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        videoProperties = null

        val loadedInfo = withContext(Dispatchers.IO) {
            try {
                var mediaName: String? = null
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        mediaName = cursor.getString(nameIndex)
                    }
                } catch (_: Exception) {
                    return@withContext VideoProperties(error = "Something wrong with file: $uri")
                }

                val mediaPath = Environment.DIRECTORY_MOVIES?.let { context.getExternalFilesDir(it) }
                val file = File(mediaPath, mediaName)
                var currentVideoProperties = VideoProperties()

                // Check if file exists
                if (!file.exists()) {
                    return@withContext VideoProperties(error = "File does not exist: $uri",)
                }

                if (!file.canRead()) {
                    return@withContext VideoProperties(error = "Cannot read file: $uri")
                }

                // --- 1. Get basic file information ---
                try {
                    val fileSize = file.length()

                    currentVideoProperties = currentVideoProperties.copy(
                        sizeBytes = fileSize,
                        // You can add lastModified to your VideoProperties model if needed
                    )
                } catch (e: Exception) {
                    currentVideoProperties = currentVideoProperties.copy(
                        error = (currentVideoProperties.error ?: "") + "Error reading basic file info: ${e.message}\n",
                    )
                }

                // --- 2. Get EXIF NotificationData using ExifInterface ---
                try {
                    FileInputStream(file).use { stream ->
                        val exif = ExifInterface(stream)
                        // Extract EXIF data if available (though videos typically have limited EXIF data)
                        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                        val gpsLatRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
                        val gpsLat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                        val gpsLonRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
                        val gpsLon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                        
                        var gpsCoordinates: String? = null
                        if (gpsLat != null && gpsLon != null) {
                            gpsCoordinates = "$gpsLatRef $gpsLat, $gpsLonRef $gpsLon"
                        }
                        
                        currentVideoProperties = currentVideoProperties.copy(
                            exifDateTimeOriginal = dateTime,
                            gpsCoordinates = gpsCoordinates,
                        )
                    }
                } catch (e: Exception) {
                    // EXIF reading might fail for many video files, this is normal
                    currentVideoProperties = currentVideoProperties.copy(
                        error = (currentVideoProperties.error ?: "") + "Could not read EXIF data (normal for most video files): ${e.message}\n",
                    )
                }

                // --- 3. Get Video Specific Metadata using MediaMetadataRetriever ---
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath) // Use file path directly
                    
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    val videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val videoResolution = if (videoWidth != null && videoHeight != null) {
                        "${videoWidth}x${videoHeight}"
                    } else null
                    val videoGpsCoordinates = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                    val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    val descriptionTag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

                    currentVideoProperties = currentVideoProperties.copy(
                        exifDateTimeOriginal = date ?: currentVideoProperties.exifDateTimeOriginal,
                        format = mimeType,
                        resolution = videoResolution, // Use EXIF GPS if video metadata doesn't have it
                        bitrate = videoBitrate?.toLongOrNull(), // Use video date or keep EXIF date
                        frameRate = frameRate?.toDoubleOrNull(),
                        durationMillis = duration?.toLongOrNull(),
                        gpsCoordinates = videoGpsCoordinates ?: currentVideoProperties.gpsCoordinates,
                        description = descriptionTag,
                        rotation = rotation?.toIntOrNull()
                    )
                    retriever.release()
                } catch (e: Exception) {
                    currentVideoProperties = currentVideoProperties.copy(
                        error = (currentVideoProperties.error ?: "") + "Error reading video metadata: ${e.message}\n",
                    )
                }

                // Clean up error string
                if (currentVideoProperties.error?.endsWith("\n") == true) {
                    currentVideoProperties = currentVideoProperties.copy(
                        error = currentVideoProperties.error!!.trimEnd('\n'),
                    )
                }
                
                // Check if we have any useful information
                val hasUsefulInfo = currentVideoProperties.durationMillis != null ||
                        currentVideoProperties.resolution != null ||
                        currentVideoProperties.bitrate != null ||
                        currentVideoProperties.frameRate != null ||
                        currentVideoProperties.format != null ||
                        currentVideoProperties.sizeBytes != null && currentVideoProperties.sizeBytes!! > 0

                if (!hasUsefulInfo && currentVideoProperties.error.isNullOrEmpty()) {
                    currentVideoProperties = currentVideoProperties.copy(
                        error = "Could not retrieve any information for this video file.",
                    )
                }

                currentVideoProperties

            } catch (e: SecurityException) {
                VideoProperties(error = "Security error processing file: ${e.message}",)
            } catch (e: Exception) {
                VideoProperties(error = "An unexpected error occurred: ${e.message}",)
            }
        }
        videoProperties = loadedInfo
        isLoading = false
    }

    // Display the loading state or the loaded information
    ModalBottomSheet(onClose) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Loading video info...")
            } else {
                // Display error if present
                videoProperties?.error?.let { error ->
                    if (error.isNotBlank()) {
                        Text("Error:", fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(error, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Display info if we have valid data
                videoProperties?.let { properties ->
                    val hasValidData = properties.durationMillis != null ||
                            properties.resolution != null ||
                            properties.bitrate != null ||
                            properties.frameRate != null ||
                            properties.format != null ||
                            properties.sizeBytes != null && properties.sizeBytes > 0

                    if (hasValidData) {
                        // Display date if available
                        properties.exifDateTimeOriginal?.let { dateTimeString ->
                            runCatching {
                                val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                                val date = parser.parse(dateTimeString)
                                val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault())
                                Text(formatter.format(date), fontWeight = FontWeight.Bold)
                            }.onFailure {
                                // Try alternative date format
                                runCatching {
                                    val parser = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.getDefault())
                                    val date = parser.parse(dateTimeString)
                                    val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault())
                                    Text(formatter.format(date), fontWeight = FontWeight.Bold)
                                }.onFailure {
                                    Text(dateTimeString, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Display properties in chips
                        ContextualFlowRow(
                            modifier = Modifier,
                            itemCount = properties.properties().size
                        ) { index ->
                            val property = properties.properties().entries
                            var showKey by remember { mutableStateOf(false) }
                            AssistChip(
                                modifier = Modifier.padding(horizontal = 3.dp),
                                onClick = { showKey = !showKey },
                                label = {
                                    Text(
                                        text = if (showKey) property.elementAt(index).key
                                        else property.elementAt(index).value ?: "",
                                        modifier = Modifier.animateContentSize()
                                    )
                                },
                                shape = MaterialTheme.shapes.large,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if(showKey) CommonColors.azure else MaterialTheme.colorScheme.surface,
                                    labelColor = Color.LightGray,
                                ),
                                border = BorderStroke(1.dp, CommonColors.azure)
                            )
                        }
                    }
                } ?: run {
                    if (uri != null) {
                        Text("Could not load any information for the video file.")
                    } else {
                        Text("No file path provided.")
                    }
                }
            }
        }
    }
}