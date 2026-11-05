package zouitel.neighborhood.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import java.io.File
import java.io.FileNotFoundException

fun printImage(
    context: Context,
    bitmap: Bitmap,
    jobName: String = "Image Print",
    scaleMode: Int = PrintHelper.SCALE_MODE_FIT
) {
    PrintHelper(context).apply {
        this.scaleMode = scaleMode
    }.printBitmap(jobName, bitmap)
}

/**
 * Prints an image from a filesystem path. This overload is retained for
 * existing callers; content:// URIs should use the Uri overload below.
 */
fun printImageFromUri(
    context: Context,
    imagePath: String,
    jobName: String = "Image Print",
    scaleMode: Int = PrintHelper.SCALE_MODE_FIT
) {
    val file = File(imagePath)
    require(file.isFile) { "Image file does not exist: $imagePath" }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    printImageFromUri(context, uri, jobName, scaleMode)
}

/**
 * Prints an image supplied by any ContentResolver-readable URI, including
 * content:// URIs from the photo picker or another provider.
 */
fun printImageFromUri(
    context: Context,
    imageUri: Uri,
    jobName: String = "Image Print",
    scaleMode: Int = PrintHelper.SCALE_MODE_FIT
) {
    val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    } ?: throw FileNotFoundException("Unable to decode image from URI: $imageUri")

    printImage(context, bitmap, jobName, scaleMode)
    // Do not recycle here: PrintHelper's adapter may still be reading the
    // bitmap after this function returns. The adapter retains its reference.
}

@Composable
fun ImageWithPrintButton(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    jobName: String = "Photo Print"
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image to print",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        )
        Button(
            onClick = {
                printImage(
                    context = context,
                    bitmap = bitmap,
                    jobName = jobName,
                    scaleMode = PrintHelper.SCALE_MODE_FIT
                )
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Print Image")
        }
    }
}

@Composable
fun PrintImageExample(bitmap: Bitmap) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image preview",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        Button(onClick = {
            printImage(
                context = context,
                bitmap = bitmap,
                jobName = "My Photo - Fit",
                scaleMode = PrintHelper.SCALE_MODE_FIT
            )
        }) {
            Text("Print (Fit to Page)")
        }
        Button(onClick = {
            printImage(
                context = context,
                bitmap = bitmap,
                jobName = "My Photo - Fill",
                scaleMode = PrintHelper.SCALE_MODE_FILL
            )
        }) {
            Text("Print (Fill Page)")
        }
    }
}
