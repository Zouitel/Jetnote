package zouitel.neighborhood.feedback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.feedback.ui.FeedbackSheetScreen
import kotlin.math.sqrt

@Composable
fun Modifier.reportable(isShakable: Boolean?, background: Int? = null): Modifier {
    val context = LocalContext.current
    val bottomNav = LocalBottomSheetNavigator.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isShakableState by rememberUpdatedState(isShakable)
    val backgroundState by rememberUpdatedState(background)

    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    val listener = remember(sensorManager) {
        object : SensorEventListener {
            private var lastX = 0f
            private var lastY = 0f
            private var lastZ = 0f
            private var lastSampleTime = 0L
            private var lastShakeTime = 0L
            private var shakeWindowStart = 0L
            private var shakesInWindow = 0

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val now = System.currentTimeMillis()
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                if (lastSampleTime != 0L && now - lastSampleTime >= SAMPLE_INTERVAL_MS) {
                    val delta = sqrt(
                        (x - lastX) * (x - lastX) +
                            (y - lastY) * (y - lastY) +
                            (z - lastZ) * (z - lastZ)
                    )
                    val acceleration = delta / (now - lastSampleTime) * 1_000
                    if (acceleration >= SHAKE_THRESHOLD && now - lastShakeTime >= SHAKE_DEBOUNCE_MS) {
                        if (now - shakeWindowStart > SHAKE_WINDOW_MS) {
                            shakeWindowStart = now
                            shakesInWindow = 0
                        }
                        shakesInWindow++
                        lastShakeTime = now

                        if (shakesInWindow >= REQUIRED_SHAKES) {
                            shakesInWindow = 0
                            shakeWindowStart = now
                            if (isShakableState == true) {
                                bottomNav.show(FeedbackSheetScreen(backgroundState))
                                keyboardController?.hide()
                            }
                        }
                    }
                }

                lastX = x
                lastY = y
                lastZ = z
                lastSampleTime = now
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
    }

    DisposableEffect(lifecycleOwner, sensorManager, accelerometer, listener) {
        var registered = false

        fun registerIfNeeded() {
            if (!registered && accelerometer != null) {
                registered = sensorManager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registerIfNeeded()
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(listener)
                    registered = false
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registerIfNeeded()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(listener)
        }
    }

    return this
}

private const val SAMPLE_INTERVAL_MS = 50L
private const val SHAKE_DEBOUNCE_MS = 250L
private const val SHAKE_WINDOW_MS = 1_500L
private const val REQUIRED_SHAKES = 3
private const val SHAKE_THRESHOLD = 2_500f
