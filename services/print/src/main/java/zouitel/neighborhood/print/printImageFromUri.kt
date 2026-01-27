package zouitel.neighborhood.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    }.also { printHelper ->
        printHelper.printBitmap(jobName, bitmap)
    }
}


fun printImageFromUri(
    context: Context,
    imagePath: String,
    jobName: String = "Image Print",
    scaleMode: Int = PrintHelper.SCALE_MODE_FIT
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(imagePath))

    val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    } ?: throw FileNotFoundException("Unable to load image from URI: $imagePath")
    
    printImage(context, bitmap, jobName, scaleMode)
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
        // Display the image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image to print",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Print button
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

/**
 * Example usage with different scale modes
 */
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