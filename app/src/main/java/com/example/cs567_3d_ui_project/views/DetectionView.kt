package com.example.cs567_3d_ui_project.views

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.activities.DetectionActivity
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService

class DetectionView(val activity: DetectionActivity, val executor: ExecutorService): DefaultLifecycleObserver {

    val root = View.inflate(activity, R.layout.fragment_camera, null)
    val cameraView = root.findViewById<PreviewView>(R.id.camera_view)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing get() = lensFacing == CameraSelector.LENS_FACING_FRONT

    private lateinit var bitmapBuffer: Bitmap

    companion object{
        const val TAG = "DetectionView"
        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "yolov11_11_7_25_float16.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        // Release TFLite resources.
        tflite.close()
        nnApiDelegate.close()
    }


    private var pauseAnalysis = false
    private var imageRotationDegrees: Int = 0
    private val tfImageBuffer = TensorImage(DataType.UINT8)

    private val tfImageProcessor by lazy {
        val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
        ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)
            )
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(0f, 1f))
            .build()
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(activity, MODEL_PATH),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }

    //    private val detector by lazy {
//        ObjectDetectionHelper(
//            tflite,
//            FileUtil.loadLabels(this, LABELS_PATH)
//        )
//    }

    val cameraCaptureButton: Button = root.findViewById<Button>(R.id.camera_capture_button)
        .apply {
            setOnClickListener{
                val matrix = Matrix().apply {
                    postRotate(imageRotationDegrees.toFloat())
                    if (isFrontFacing) postScale(-1f, 1f)
                }
                val uprightImage = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)


                try {

                    val photoDirectory = File("/storage/emulated/0/Download").apply { mkdirs() }
                    val timestamp = System.currentTimeMillis()
                    val photoFile = File(photoDirectory, "combined_image_$timestamp.jpg")

                    FileOutputStream(photoFile).use { out ->
                        uprightImage.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.flush()
                        out.close()
                    }

                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                    }
                    activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    Log.d("CameraX", "Image added to gallery: ${photoFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e("CameraX", "Error adding image to gallery: ${e.message}", e)
                }
//                activityCameraBinding.imagePredicted.setImageBitmap(uprightImage)
//                activityCameraBinding.imagePredicted.visibility = View.VISIBLE
            }
        }


    //        activityCameraBinding.cameraCaptureButton.setOnClickListener {
//
//            // Disable all camera controls
//            it.isEnabled = false
//
//            if (pauseAnalysis) {
//                // If image analysis is in paused state, resume it
//                pauseAnalysis = false
//                activityCameraBinding.imagePredicted.visibility = View.GONE
//
//            } else {
//                // Otherwise, pause image analysis and freeze image
//                pauseAnalysis = true
//                val matrix = Matrix().apply {
//                    postRotate(imageRotationDegrees.toFloat())
//                    if (isFrontFacing) postScale(-1f, 1f)
//                }
//                val uprightImage = Bitmap.createBitmap(
//                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
//                activityCameraBinding.imagePredicted.setImageBitmap(uprightImage)
//                activityCameraBinding.imagePredicted.visibility = View.VISIBLE
//            }
//
//            // Re-enable camera controls
//            it.isEnabled = true
//        }


    /**
     * Helper function used to map the coordinates for objects coming out of
     * the model into the coordinates that the user sees on the screen.
     */
//    private fun mapOutputCoordinates(location: RectF): RectF {
//
//        // Step 1: map location to the preview coordinates
//        val previewLocation = RectF(
//            location.left * activityCameraBinding.viewFinder.width,
//            location.top * activityCameraBinding.viewFinder.height,
//            location.right * activityCameraBinding.viewFinder.width,
//            location.bottom * activityCameraBinding.viewFinder.height
//        )
//
//        // Step 2: compensate for camera sensor orientation and mirroring
//        val isFrontFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
//        val correctedLocation = if (isFrontFacing) {
//            RectF(
//                activityCameraBinding.viewFinder.width - previewLocation.right,
//                previewLocation.top,
//                activityCameraBinding.viewFinder.width - previewLocation.left,
//                previewLocation.bottom)
//        } else {
//            previewLocation
//        }
//
//        // Step 3: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
//        val margin = 0.1f
//        val requestedRatio = 4f / 3f
//        val midX = (correctedLocation.left + correctedLocation.right) / 2f
//        val midY = (correctedLocation.top + correctedLocation.bottom) / 2f
//        return if (activityCameraBinding.viewFinder.width < activityCameraBinding.viewFinder.height) {
//            RectF(
//                midX - (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
//                midY - (1f - margin) * correctedLocation.height() / 2f,
//                midX + (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
//                midY + (1f - margin) * correctedLocation.height() / 2f
//            )
//        } else {
//            RectF(
//                midX - (1f - margin) * correctedLocation.width() / 2f,
//                midY - (1f + margin) * requestedRatio * correctedLocation.height() / 2f,
//                midX + (1f - margin) * correctedLocation.width() / 2f,
//                midY + (1f + margin) * requestedRatio * correctedLocation.height() / 2f
//            )
//        }
//    }
//


    //    private fun reportPrediction(
//        prediction: ObjectDetectionHelper.ObjectPrediction?
//    ) = activityCameraBinding.viewFinder.post {
//
//        // Early exit: if prediction is not good enough, don't report it
//        if (prediction == null || prediction.score < ACCURACY_THRESHOLD) {
//            activityCameraBinding.boxPrediction.visibility = View.GONE
//            activityCameraBinding.textPrediction.visibility = View.GONE
//            return@post
//        }
//
//        // Location has to be mapped to our local coordinates
//        val location = mapOutputCoordinates(prediction.location)
//
//        // Update the text and UI
//        activityCameraBinding.textPrediction.text = "${"%.2f".format(prediction.score)} ${prediction.label}"
//        (activityCameraBinding.boxPrediction.layoutParams as ViewGroup.MarginLayoutParams).apply {
//            topMargin = location.top.toInt()
//            leftMargin = location.left.toInt()
//            width = min(activityCameraBinding.viewFinder.width, location.right.toInt() - location.left.toInt())
//            height = min(activityCameraBinding.viewFinder.height, location.bottom.toInt() - location.top.toInt())
//        }
//
//        // Make sure all UI elements are visible
//        activityCameraBinding.boxPrediction.visibility = View.VISIBLE
//        activityCameraBinding.textPrediction.visibility = View.VISIBLE
//    }

    //    /** Declare and bind preview and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError")
    public fun bindCameraUseCases() = cameraView.post {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(root.context)
        cameraProviderFuture.addListener ({

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(cameraView.display.rotation)
                .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(cameraView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            var frameCounter = 0
            var lastFpsTimestamp = System.currentTimeMillis()

            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer {
                image ->
                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    imageRotationDegrees = image.imageInfo.rotationDegrees
                    bitmapBuffer = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888)
                }

                // Copy out RGB bits to our shared buffer
                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer)  }
            })


//            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
//                if (!::bitmapBuffer.isInitialized) {
//                    // The image rotation and RGB image buffer are initialized only once
//                    // the analyzer has started running
//                    imageRotationDegrees = image.imageInfo.rotationDegrees
//                    bitmapBuffer = Bitmap.createBitmap(
//                        image.width, image.height, Bitmap.Config.ARGB_8888)
//                }
//
//                // Early exit: image analysis is in paused state
//                if (pauseAnalysis) {
//                    image.close()
//                    return@Analyzer
//                }
//
//                // Copy out RGB bits to our shared buffer
//                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer)  }
//
//                // Process the image in Tensorflow
//                val tfImage =  tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })
//
//                // Perform the object detection for the current frame
////                val predictions = detector.predict(tfImage)
////
////                // Report only the top prediction
////                reportPrediction(predictions.maxByOrNull { it.score })
//
//                // Compute the FPS of the entire pipeline
//                val frameCount = 10
//                if (++frameCounter % frameCount == 0) {
//                    frameCounter = 0
//                    val now = System.currentTimeMillis()
//                    val delta = now - lastFpsTimestamp
//                    val fps = 1000 * frameCount.toFloat() / delta
//                    Log.d(TAG, "FPS: ${"%.02f".format(fps)} with tensorSize: ${tfImage.width} x ${tfImage.height}")
//                    lastFpsTimestamp = now
//                }
//            })

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this.activity as LifecycleOwner, cameraSelector, preview, imageAnalysis)

            // Use the camera object to link our preview use case with the view
            preview.surfaceProvider = cameraView.surfaceProvider

        }, ContextCompat.getMainExecutor(root.context))
    }
}