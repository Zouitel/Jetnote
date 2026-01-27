package zouitel.neighborhood.feedback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import kotlin.math.abs

@Composable
fun Modifier.reportable(isShakable: Boolean?, background: Int? = null): Modifier {
    val context = LocalContext.current
    val bottomNav = LocalBottomSheetNavigator.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val lifecycleOwner = LocalLifecycleOwner.current

    val sensorEventListener = remember {
        object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastShake: Long = 0
            private var shakeCount = 0

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val currentTime = System.currentTimeMillis()

                    if ((currentTime - lastUpdate) > 100) { // Adjust frequency of readings
                        val diffTime = currentTime - lastUpdate
                        lastUpdate = currentTime

                        val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
                        if (speed > SHAKE_THRESHOLD) { // Adjust shake sensitivity threshold
                            if ((currentTime - lastShake) < SHAKE_SLOP_TIME_MS) {
                                return  // Avoid multiple shakes too quickly
                            }
                            lastShake = currentTime
                            shakeCount++

                            if (shakeCount >= SHAKE_COUNT_RESET_TIME_MS) { // Optionally reset shake count after a delay
                                shakeCount = 0
                            }
                            // Invoke the action when a shake is detected
                            if (isShakable == true) {
                                bottomNav.show(FeedbackSheetScreen(background))
                                keyboardController?.hide()
                            }
                        }
                        lastX = x
                        lastY = y
                        lastZ = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for shake detection, but required to implement SensorEventListener
            }

            private var lastX: Float = 0f
            private var lastY: Float = 0f
            private var lastZ: Float = 0f
        }
    }

    // Register and unregister sensor listener based on lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    accelerometerSensor?.let {
                        sensorManager.registerListener(
                            sensorEventListener,
                            it,
                            SensorManager.SENSOR_DELAY_GAME // Adjust sensor delay for responsiveness
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(sensorEventListener)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(sensorEventListener) // Ensure unregister on dispose
        }
    }
    return this
}

// Constants for shake detection tuning (adjust as needed)
private const val SHAKE_THRESHOLD = 3000 // Sensitivity threshold (higher value = less sensitive)
private const val SHAKE_SLOP_TIME_MS = 100 // Minimum time between shakes (milliseconds)
private const val SHAKE_COUNT_RESET_TIME_MS = 3 // Reset shake count after this many shakes (optional)