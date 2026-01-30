package zouitel.neighborhood.workspace.ui.features.details

import android.graphics.BitmapFactory
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

@Composable
fun ImageDetails(path: String?, onClose:() -> Unit) {
    if (path == null) return
    val context = LocalContext.current

    val settingsModel = koinInject<SettingsScreenModel>()
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    var imageProperties by remember { mutableStateOf<ImageProperties?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
    }.getOrElse {
        path.toUri()
    }

    LaunchedEffect(uri) {
        if (uri.path.isNullOrEmpty()) {
            imageProperties = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        imageProperties = null

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
                    return@withContext ImageProperties(error = "Something wrong with file: $uri")
                }

                val mediaPath =
                    Environment.DIRECTORY_PICTURES?.let { context.getExternalFilesDir(it) }
                val file = File(mediaPath, mediaName)
                var currentImageProperties = ImageProperties()

                // Check if file exists
                if (!file.exists()) {
                    return@withContext ImageProperties(error = "File does not exist: $uri")
                }

                if (!file.canRead()) {
                    return@withContext ImageProperties(error = "Cannot read file: $uri")
                }

                // --- 1. Get basic file information ---
                try {
                    val fileSize = file.length()
                    val lastModified = file.lastModified()
                    val fileName = file.nameWithoutExtension

                    currentImageProperties = currentImageProperties.copy(
                        mediaTitle = fileName,
                        sizeBytes = fileSize,
                        lastModified = lastModified,
                    )
                } catch (e: Exception) {
                    currentImageProperties = currentImageProperties.copy(
                        error = (currentImageProperties.error
                            ?: "") + "Error reading basic file info: ${e.message}\n"
                    )
                }

                // --- 2. Get Dimensions and MIME Type using BitmapFactory.Options ---
                try {
                    FileInputStream(file).use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(stream, null, options)

                        currentImageProperties =
                            if (options.outWidth > 0 && options.outHeight > 0) {
                                currentImageProperties.copy(
                                    mimeType = options.outMimeType,
                                    width = options.outWidth,
                                    height = options.outHeight
                                )
                            } else {
                                currentImageProperties.copy(
                                    error = (currentImageProperties.error
                                        ?: "") + "Could not decode image dimensions or invalid format.\n",
                                )
                            }
                    }
                } catch (e: Exception) {
                    currentImageProperties = currentImageProperties.copy(
                        error = (currentImageProperties.error
                            ?: "") + "Error during dimension decoding: ${e.message}\n",
                    )
                }

                // --- 3. Get EXIF NotificationData using ExifInterface ---
                try {
                    FileInputStream(file).use { stream ->
                        val exif = ExifInterface(stream)

                        // Extract common tags
                        val dateTimeOriginal =
                            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                        val dateTimeDigitized =
                            exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                        val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                        val isoSpeedRatings = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                        val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                        val exposureCompensation =
                            exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE)

                        // Calculate megapixels
                        val exifWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                        val exifHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                        var megapixels: Double? = null
                        if (exifWidth > 0 && exifHeight > 0) {
                            megapixels = (exifWidth * exifHeight) / 1_000_000.0
                        } else if (currentImageProperties.width > 0 && currentImageProperties.height > 0) {
                            megapixels =
                                (currentImageProperties.width * currentImageProperties.height) / 1_000_000.0
                        }

                        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                        val lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
                        val lensMake = exif.getAttribute(ExifInterface.TAG_LENS_MAKE)
                        val meteringMode = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1)
                        val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                        val whiteBalance = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
                        val exposureProgram =
                            exif.getAttributeInt(ExifInterface.TAG_EXPOSURE_PROGRAM, -1)
                        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
                        val copyright = exif.getAttribute(ExifInterface.TAG_COPYRIGHT)
                        val imageDescription =
                            exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)

                        // GPS data
                        var latitude: Double? = null
                        var longitude: Double? = null
                        runCatching {
                            val latLong = exif.latLong
                            if (latLong != null && latLong.size == 2) {
                                latitude = latLong[0]
                                longitude = latLong[1]
                            }
                        }

                        currentImageProperties = currentImageProperties.copy(
                            exifDateTimeOriginal = dateTimeOriginal ?: dateTime
                            ?: dateTimeDigitized,
                            exifMake = make,
                            exifModel = model,
                            exifExposureTime = exposureTime,
                            exifFNumber = fNumber,
                            exifIsoSpeedRatings = isoSpeedRatings,
                            exifFocalLength = focalLength,
                            exifExposureCompensation = exposureCompensation,
                            exifMegapixels = megapixels,
                            exifGpsLatitude = latitude,
                            exifGpsLongitude = longitude,
                            exifOrientation = if (orientation != -1) orientation else null,
                            exifLensModel = lensModel,
                            exifMeteringMode = if (meteringMode != -1) meteringMode else null,
                            exifFlash = if (flash != -1) flash else null,
                            exifWhiteBalance = if (whiteBalance != -1) whiteBalance else null,
                            exifExposureProgram = if (exposureProgram != -1) exposureProgram else null,
                            exifSoftware = software,
                            exifCopyright = copyright,
                            exifImageDescription = imageDescription
                        )
                    }
                } catch (e: Exception) {
                    currentImageProperties = currentImageProperties.copy(
                        error = (currentImageProperties.error
                            ?: "") + "Error reading EXIF data: ${e.message}\n"
                    )
                }

                // Clean up error string
                if (currentImageProperties.error?.endsWith("\n") == true) {
                    currentImageProperties = currentImageProperties.copy(
                        error = currentImageProperties.error!!.trimEnd('\n')
                    )
                }

                // Check if we have any useful information
                val hasUsefulInfo = currentImageProperties.isValidDimensions ||
                        currentImageProperties.isPartialInfo ||
                        currentImageProperties.sizeBytes != null && currentImageProperties.sizeBytes!! > 0

                if (!hasUsefulInfo && currentImageProperties.error.isNullOrEmpty()) {
                    currentImageProperties = currentImageProperties.copy(
                        error = "Could not retrieve any information for this image file."
                    )
                }

                currentImageProperties
            } catch (e: SecurityException) {
                ImageProperties(error = "Security error processing file: ${e.message}")
            } catch (e: Exception) {
                ImageProperties(error = "An unexpected error occurred: ${e.message}")
            }
        }
        imageProperties = loadedInfo
        isLoading = false
    }

    // Display the loading state or the loaded information
    ModalBottomSheet(onClose) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Loading image info...")
            } else {
                // Display error if present
                imageProperties?.error?.let { error ->
                    if (error.isNotBlank()) {
                        Text("Error:", fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(error, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Display info if we have valid data
                imageProperties?.let { properties ->
                    if (properties.isValidDimensions) {
                        // Display date if available
                        properties.exifDateTimeOriginal?.let { dateTimeString ->
                            runCatching {
                                val parser =
                                    SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                                val date = parser.parse(dateTimeString)
                                val formatter =
                                    SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault())
                                Text(formatter.format(date), fontWeight = FontWeight.Bold)
                            }.onFailure {
                                Text(dateTimeString, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Display camera make and model
                        val cameraInfo = buildString {
                            properties.exifMake?.let { make ->
                                append(make.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                                })
                            }
                            properties.exifModel?.let { model ->
                                if (isNotEmpty()) append(" ")
                                append(model)
                            }
                        }

                        if (cameraInfo.isNotBlank()) {
                            Text(
                                text = cameraInfo,
                                fontWeight = FontWeight.Bold
                            )
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
                                    containerColor = if (showKey) CommonColors.prime else MaterialTheme.colorScheme.surface,
                                    labelColor = Color.LightGray,
                                ),
                                border = BorderStroke(1.dp, CommonColors.prime)
                            )
                        }
                    }
                } ?: run {
                    if (uri != null) {
                        Text("Could not load any information for the image file.")
                    } else {
                        Text("No file path provided.")
                    }
                }
            }
        }
    }
}