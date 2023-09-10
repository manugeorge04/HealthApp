package com.example.manu_mc_proj1

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


import com.example.manu_mc_proj1.databinding.ActivityMainBinding


//BREATHE RATE
import android.widget.Button
import android.widget.TextView
import kotlin.math.sqrt
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null

    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    // BREATHE RATE
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null

    private val accelValuesX = mutableListOf<Float>()
    private val accelValuesY = mutableListOf<Float>()
    private val accelValuesZ = mutableListOf<Float>()

    private val dataCollectionDuration = 45000L // 45 seconds in milliseconds
    private var isRecording = false
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        dbHelper = (applicationContext as HealthApp).dbHelper

        // Set up the listeners for take photo and video capture buttons
//        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        // Set up the listeners for the video capture button
        viewBinding.measureHeartRate.setOnClickListener { captureVideo() }

        viewBinding.uploadSigns.setOnClickListener { uploadSign() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val selector = QualitySelector
            .from(
                Quality.HD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )

        val recorder = Recorder.Builder()
            .setQualitySelector(selector)
            .build()

        this.videoCapture = VideoCapture.withOutput(recorder)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Check if the accelerometer sensor is available
        if (accelerometerSensor == null) {
            // Handle the case where accelerometer sensor is not available
            val textView = findViewById<TextView>(R.id.textView)
            textView.text = "No Acceloerometer"
        }

        val button = findViewById<Button>(R.id.symptoms) // Replace with your button's ID
        button.setOnClickListener {
            // Create an Intent to open the new Activity
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }

        val startButton = findViewById<Button>(R.id.measure_respiratory_rate)
        startButton.setOnClickListener {
            if (!isRecording) {
                val textView = findViewById<TextView>(R.id.textView)
                textView.text = "Reading Respiratory Rate ...."
                startRecording()
            } else {
                stopRecording()
            }
        }

    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    var bindToLifecycle: Camera? = null


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.HIGHEST,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                        )
                    )
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                bindToLifecycle =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))



    }

    // Implements VideoCapture use case, including start and stop capturing.
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.measureHeartRate.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        bindToLifecycle?.cameraControl?.enableTorch(true)

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.measureHeartRate.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG, "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }
                        viewBinding.measureHeartRate.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                        bindToLifecycle?.cameraControl?.enableTorch(false)
                        viewBinding.textView2.apply {
                            text = "Calculating..."
                        }
                        var path = "/storage/emulated/0/Movies/CameraX-Video/" + name + ".mp4";
                        updateHeartRate(path)
                    }
                }
            }


    }

    fun updateHeartRate(path: String): Deferred<Unit> = GlobalScope.async {
        val slowTask = SlowTask()
        val rate = slowTask.execute(path).get()
        val textView = findViewById<TextView>(R.id.textView2)
        textView.text = rate
    }

    fun convertMediaUriToPath(uri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, proj, null, null, null)
        val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        var path = cursor.getString(column_index)
        cursor.close()
        return path
    }

    open class SlowTask
        : AsyncTask<String, String, String?>() {
        override fun doInBackground(vararg params: String?): String? {
            var m_bitmap: Bitmap? = null
            var retriever = MediaMetadataRetriever()
            var frameList = ArrayList<Bitmap>()
            try {

                retriever.setDataSource(params[0])
                var duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                var aduration = duration!!.toInt()
                var i = 10
                while (i < aduration) {
                    val bitmap = retriever.getFrameAtIndex(i)
                    frameList.add(bitmap!!)
                    i += 5
                }
            } catch (m_e: Exception) {
            } finally {
                retriever?.release()
                var redBucket: Long = 0
                var pixelCount: Long = 0
                val a = mutableListOf<Long>()
                for (i in frameList) {
                    redBucket = 0
                    for (y in 550 until 650) {
                        for (x in 550 until 650) {
                            val c: Int = i.getPixel(x, y)
                            pixelCount++
                            redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                        }
                    }
                    a.add(redBucket)
                }
                val b = mutableListOf<Long>()
                for (i in 0 until a.lastIndex - 5) {
                    var temp =
                        (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2) + a.elementAt(
                            i + 3
                        ) + a.elementAt(
                            i + 4
                        )) / 4
                    b.add(temp)
                }
                var x = b.elementAt(0)
                var count = 0
                for (i in 1 until b.lastIndex) {
                    var p = b.elementAt(i.toInt())
                    if ((p - x) > 3500) {
                        count = count + 1
                    }
                    x = b.elementAt(i.toInt())
                }
                var rate = ((count.toFloat() / 45) * 60).toInt()
                return (rate / 2).toString()
            }
        }


    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                accelValuesX.add(x)
                accelValuesY.add(y)
                accelValuesZ.add(z)

                if (isRecording && System.currentTimeMillis() - startTime >= dataCollectionDuration) {
                    val textView = findViewById<TextView>(R.id.textView)
                    textView.text = "Finished Reading Heart Rate"
                    stopRecording()
                }
            }
        }
    }

    private fun startRecording() {
        accelValuesX.clear()
        accelValuesY.clear()
        accelValuesZ.clear()

        isRecording = true
        startTime = System.currentTimeMillis()

        accelerometerSensor?.let { sensor ->
            sensorManager.registerListener(
                accelerometerListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun stopRecording() {
        isRecording = false
        sensorManager.unregisterListener(accelerometerListener)

        // Call your function with the recorded data
        if (accelValuesX.size >= 150) {
            val result = callRespiratoryCalculator(accelValuesX, accelValuesY, accelValuesZ)
            val textView = findViewById<TextView>(R.id.textView)
            textView.text = "Your Respiratory Rate is: $result"
        }
        else{
            var x = accelValuesX.size
            val textView = findViewById<TextView>(R.id.textView)
            textView.text = "Insufficient Data collected, try again $x"
        }
    }

    private fun callRespiratoryCalculator(
        accelValuesX: List<Float>,
        accelValuesY: List<Float>,
        accelValuesZ: List<Float>
    ): Int {
        var previousValue = 0f
        var currentValue = 0f
        previousValue = 10f
        var k=0
        var len = accelValuesX.size
        for (i in 11 until len) {
            currentValue = sqrt(
                Math.pow(accelValuesZ[i].toDouble(), 2.0) + Math.pow(
                    accelValuesX[i].toDouble(),
                    2.0
                ) + Math.pow(accelValuesY[i].toDouble(), 2.0)
            ).toFloat()
            var x = previousValue - currentValue
            if (Math.abs(x) > 0.15) {
                k++
            }
            previousValue=currentValue
        }
//        val ret= (k/12.5)
        return k+12
    }

    private fun uploadSign() {
        // Gets the data repository in write mode
        val breath = findViewById<TextView>(R.id.textView).text.toString()
        val heart = findViewById<TextView>(R.id.textView2).text.toString()
        val db = dbHelper?.writableDatabase
        val values = ContentValues().apply {
            put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_HEART_RATE, heart)
            put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_BREATH_RATE, breath)
        }
        db?.insert(FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME, null, values)
        if (db != null) {
            db.close()
        }
    }




}