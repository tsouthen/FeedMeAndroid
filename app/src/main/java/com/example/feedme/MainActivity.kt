package com.example.feedme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.feedme.ui.theme.FeedMeTheme

class MainActivity : ComponentActivity(), OpenCloseEventListener {
    private lateinit var openCloseDetector: OpenCloseDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openCloseDetector = OpenCloseDetector(this, this)

        setContent {
            FeedMeTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    MainContent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        openCloseDetector.start()
    }

    override fun onPause() {
        super.onPause()
        openCloseDetector.stop()
    }

    override fun onOpen() {
        println("Opened")
    }

    override fun onClose() {
        println("Closed")
    }
}

interface OpenCloseEventListener {
    fun onOpen()
    fun onClose()
}

class OpenCloseDetector(private val context: Context, private val openCloseListener: OpenCloseEventListener) {
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    companion object listener: SensorEventListener {
        private val mRotationMatrix = FloatArray(16)
        private val mRotationMatrixFromVector = FloatArray(16)
        private val orientations = FloatArray(3)
        private var isOpen = false
        private var openCloseListener: OpenCloseEventListener? = null

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
                return
            }
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values)
            SensorManager.remapCoordinateSystem(mRotationMatrixFromVector, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix)
            SensorManager.getOrientation(mRotationMatrix, orientations)

            val pitch = Math.toDegrees(orientations[1].toDouble())
            if (pitch > 80) {
                if (!isOpen) {
                    isOpen = true
                    openCloseListener?.onOpen()
                }
            } else if (pitch < 10) {
                if (isOpen) {
                    isOpen = false
                    openCloseListener?.onClose()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Do something here if sensor accuracy changes.
        }
    }

    public fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let {
            listener.openCloseListener = openCloseListener
            sensorManager.registerListener(listener, it, 500000) //SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    public fun stop() {
        listener.openCloseListener = null
        sensorManager.unregisterListener(listener)
    }
}

@Composable
fun MainContent() {
    var capturing by remember { mutableStateOf(false) }
    Column(modifier = Modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Last Fed:")
        Text(text = "Yesterday!")
        Button(onClick = { capturing = !capturing }) {
            Box {
                if (!capturing)
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Start sensors")
                else
                    Icon(painterResource(id = R.drawable.ic_round_stop_24), contentDescription = "Stop sensors")

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FeedMeTheme {
        MainContent()
    }
}