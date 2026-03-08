package zouitel.neighborhood.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAverageColorFromImage(context: Context, uri: String): Color {
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(50, 50) // Scale down for performance
                .scale(Scale.FIT)
                .allowHardware(false) // Required to access pixels
                .build()
            
            val result = loader.execute(request)
            
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                
                bitmap?.let {
                    var redSum = 0L
                    var greenSum = 0L
                    var blueSum = 0L
                    var pixelCount = 0
                    
                    for (x in 0 until it.width) {
                        for (y in 0 until it.height) {
                            val pixel = it.getPixel(x, y)
                            redSum += android.graphics.Color.red(pixel)
                            greenSum += android.graphics.Color.green(pixel)
                            blueSum += android.graphics.Color.blue(pixel)
                            pixelCount++
                        }
                    }
                    
                    return@withContext Color(
                        red = (redSum / pixelCount).toInt(),
                        green = (greenSum / pixelCount).toInt(),
                        blue = (blueSum / pixelCount).toInt()
                    )
                }
            }
            
            Color.Gray
        } catch (e: Exception) {
            e.printStackTrace()
            Color.Gray
        }
    }
}