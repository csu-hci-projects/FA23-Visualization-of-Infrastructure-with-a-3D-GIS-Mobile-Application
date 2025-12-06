package com.example.cs567_3d_ui_project.argis.mlutils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class ObjectDetectionHelper(
    val context: Context,
    var threshold: Float = THRESHOLD_DEFAULT,
    var maxResults: Int = MAX_RESULTS_DEFAULT,
    var delegate: Delegate = Delegate.CPU,
    var model: Model = MODEL_DEFAULT
) {


    private var interpreter: Interpreter? = null

    private val _detectionResult = MutableSharedFlow<DetectionResult>()
    val detectionResult: SharedFlow<DetectionResult> = _detectionResult

    private val _error = MutableSharedFlow<Throwable>()
    val error: SharedFlow<Throwable> = _error

    companion object {
        val MODEL_DEFAULT = Model.Yolov1111725float16NMS
        const val MAX_RESULTS_DEFAULT = 10
        const val THRESHOLD_DEFAULT = 0.5F

        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.20F
        private const val IOU_THRESHOLD = 0.5F

        const val TAG = "ObjectDetectorHelper"
    }

    enum class Delegate(val value: Int) {
        CPU(0), NNAPI(1)
    }

    enum class Model(val fileName: String) {
        YoloV1111725float16("yolov11_11_7_25_float16.tflite"),
        YoloV1111725float32("yolov11_11_7_25_float32.tflite"),
        Yolov1111725float16NMS("yolov11_11_7_25_float16_nms.tflite"),
        Yolov1111725float32NMS("yolov11_11_7_25_float32_nms.tflite")
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    suspend fun setupObjectDetector() {
        try {
            val litertBuffer = FileUtil.loadMappedFile(context, model.fileName)
            /*labels = getModelMetadata(litertBuffer)*/
            interpreter = Interpreter(litertBuffer, Interpreter.Options().apply {
                numThreads = 5
                useNNAPI = delegate == Delegate.NNAPI
            })
            Log.i(TAG, "Successfully init Interpreter!")


         /*   val (_, tensorHeight, tensorWidth, _) = interpreter!!.getInputTensor(0).shape()
            Log.i(TAG, "Input tensor shape Height: $tensorHeight, Width: $tensorWidth")

            val outputShape = interpreter!!.getOutputTensor(0).shape()
            numChannel = outputShape[1]
            numElements = outputShape[2]

            Log.i(TAG, "Output tensor shape Channels: $numChannel, NumElements: $numElements")*/

        } catch (e: Exception) {
            _error.emit(e)
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    private fun getDetections(
        locations: FloatArray,
        categories: FloatArray,
        scores: FloatArray,
        width: Int,
        scaleRatio: Float
    ): List<Detection> {
        val boundingBoxList = getBoundingBoxList(locations, width, scaleRatio)

        val detections = mutableListOf<Detection>()
        for (i in 0 until maxResults) {
            val categoryIndex = categories[i].toInt()
            detections.add(
                Detection(
                    label = labels()[categoryIndex],
                    boundingBox = boundingBoxList[i],
                    score = scores[i]
                )
            )
        }

        return detections
            .filter { !it.boundingBox.isEmpty && it.score >= THRESHOLD_DEFAULT }
            .sortedByDescending { it.score }
    }

    suspend fun detectRaw(bitmap: Bitmap, rotationDegrees: Int): ByteArray
    {
        if (interpreter == null) return ByteArray(0)
        return withContext(Dispatchers.IO) {
            val (_, tensorHeight, tensorWidth, _) = interpreter!!.getInputTensor(0).shape()
            Log.i(TAG, "Input tensor shape Height: $tensorHeight, Width: $tensorWidth")

            if (tensorWidth == 0 || tensorHeight == 0) {
                return@withContext ByteArray(0)
            }

            val scaledBitmap = scaleBitmap(bitmap, tensorWidth, tensorHeight)

            // Preprocess the image and convert it into a TensorImage for classification.
            val tensorImage = createTensorImage(
                bitmap = scaledBitmap, rotationDegrees = rotationDegrees
            )

            return@withContext detectImageRaw(tensorImage).array()
        }

    }

    // Runs object detection on live streaming cameras frame-by-frame and returns the results
    // asynchronously to the caller.
    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int) {
        if (interpreter == null) return
        withContext(Dispatchers.IO) {
            val (_, tensorHeight, tensorWidth, _) = interpreter!!.getInputTensor(0).shape()
            Log.i(TAG, "Input tensor shape Height: $tensorHeight, Width: $tensorWidth")

            if(tensorWidth == 0 || tensorHeight == 0){
                return@withContext
            }

            val scaledBitmap = scaleBitmap(bitmap, tensorWidth, tensorHeight)
/*
            try {

                val photoDirectory = File("/storage/emulated/0/Download").apply { mkdirs() }
                val timestamp = System.currentTimeMillis()
                val photoFile = File(photoDirectory, "combined_image_$timestamp.jpg")

                FileOutputStream(photoFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                }

                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                Log.d("CameraX", "Image added to gallery: ${photoFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("CameraX", "Error adding image to gallery: ${e.message}", e)
            }
*/

            // Preprocess the image and convert it into a TensorImage for classification.
            val tensorImage = createTensorImage(
                bitmap = scaledBitmap, rotationDegrees = rotationDegrees
            )

            val detections = detectImage(tensorImage)

            Log.i(TAG, "Completed Detection! Count: ${detections.size}")

            val detectionResult = DetectionResult(
                    detections = detections,
                    inputImageWidth = tensorWidth,
                    inputImageHeight = tensorHeight,
                )
            _detectionResult.emit(detectionResult)
        }
    }

    fun detectImageRaw(tensorImage: TensorImage): ByteBuffer {
        val imageBuffer = tensorImage.buffer

        val outputShape = interpreter!!.getOutputTensor(0).shape()

        val numChannel = outputShape[1]
        val numElements = outputShape[2]

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), DataType.FLOAT32)
        interpreter!!.run(imageBuffer, output.buffer)

        return output.buffer
    }

    fun detectImage(tensorImage: TensorImage): List<BoundingBox> {

        val imageBuffer = tensorImage.buffer

        val outputShape = interpreter!!.getOutputTensor(0).shape()

        val numChannel = outputShape[1]
        val numElements = outputShape[2]

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), DataType.FLOAT32)
        interpreter!!.run(imageBuffer, output.buffer)

        return bestBox(output.floatArray)
    }


    /*
    *
    * https://www.reddit.com/r/Ultralytics/comments/1jwl6se/ultralytics_postprocessing_guide/
    * https://github.com/ultralytics/ultralytics/issues/19088
    * https://docs.ultralytics.com/datasets/obb/#yolo-obb-format
    *
    * Post processing with NMS has the shape of below
    *
    * x1, y1, x2, y2, confidence, label, and angle in radians
    *
    * With NMS included, the output shape of the tensor would be (BatchSize, NumberOfDetects, NumberOfFeatures(described above)
    * */
    fun postProcessNMS(array: FloatArray, originalImageWidth: Float, originalImageHeight: Float): List<OBBDetectionNMS> {
        val detections = ArrayList<OBBDetectionNMS>()

        val (_, tensorHeight, tensorWidth, _) = interpreter!!.getInputTensor(0).shape()
        Log.i(TAG, "Input tensor shape Height: $tensorHeight, Width: $tensorWidth")

        val outputShape = interpreter!!.getOutputTensor(0).shape()

        val batchSize = outputShape[0]
        val numDetections = outputShape[1]
        val numFeatures = outputShape[2]

        if(numDetections == 0){
            return detections
        }

        val numLabels = labels().size
        if(numLabels <= 0){
            return detections
        }

        for (c in 0 until numDetections step batchSize * numFeatures) {
            val confidence = array[c+4]
            if(confidence > CONFIDENCE_THRESHOLD){
                var x0 = array[c]
                var y0 = array[c+1]
                var x1 = array[c+2]
                var y1 = array[c+3]
                val classLabel = array[c+5]
                val angleInRadians = array[c+6]

                //https://github.com/android/camera-samples/blob/main/CameraXAdvanced/tflite/src/main/java/com/example/android/camerax/tflite/CameraActivity.kt
                //Following this example for scaling the results
                val obb = OrientedBoundingBoxNMS(x0, y0, x1, y1, classLabel.toInt(), angleInRadians)
                val mappedCoordinates = mapOutputCoordinates(obb, originalImageWidth, originalImageHeight)
                detections.add(OBBDetectionNMS(obb, confidence, labels()[classLabel.toInt()], mappedCoordinates))
            }
        }
        return detections
    }

    //https://docs.ultralytics.com/datasets/obb/#yolo-obb-format

    //https://github.com/ultralytics/ultralytics/blob/main/ultralytics/utils/ops.py#L361
    //https://github.com/ultralytics/ultralytics/blob/main/ultralytics/utils/metrics.py#L254
    /*
    * The Shape of the output is center x, center y, width, height, the next
    * n classes and lastly the angle.
    * https://github.com/ultralytics/ultralytics/issues/19088
    *
    * x, y, w, h, class0, class1, ..., class9, angle
    * */
    fun postProcess(array: FloatArray, originalImageWidth: Float, originalImageHeight: Float): List<OBBDetection> {
        val detections = ArrayList<OBBDetection>()
        val obbs = ArrayList<OrientedBoundingBox>()
        val scores = ArrayList<Float>()
        val labels = ArrayList<Int>()

        val (_, tensorHeight, tensorWidth, _) = interpreter!!.getInputTensor(0).shape()
        Log.i(TAG, "Input tensor shape Height: $tensorHeight, Width: $tensorWidth")

        val outputShape = interpreter!!.getOutputTensor(0).shape()

        val inputImageHeight = tensorHeight.toFloat()
        val inputImageWidth = tensorWidth.toFloat()

//        val originalImageWidth = originalImage.width.toFloat()
//        val originalImageHeight = originalImage.height.toFloat()

        val batchSize = outputShape[0]
        val numFeatures = outputShape[1]
        val numDetections = outputShape[2]

        if(numDetections == 0){
            return detections
        }

        val numLabels = numFeatures - 5
        if(numLabels <= 0){
            return detections
        }

        val scalingFactor = Math.min(inputImageHeight/originalImageHeight, inputImageWidth/originalImageWidth)
        val ratio = 1.0f/scalingFactor

        for (c in 0 until numDetections step batchSize * numFeatures) {
            val x = array[c]
            val y = array[c + 1]
            val w = array[c + 2]
            val h = array[c + 3]

            val scoresIdx = c + 4
            var maxScore = -Float.MAX_VALUE
            var classId = -1

            for(j in 0 until numLabels){
                val score = array[scoresIdx + j]
                if(score > maxScore){
                    maxScore = score
                    classId = j
                }
            }

            val angle = array[c + 4 + numLabels]

            if(maxScore > CONFIDENCE_THRESHOLD)
            {
                var cx = x * ratio
                var cy = y * ratio
                var bw = w * ratio
                var bh = h * ratio

                // Discard boxes that are too small.
                if (bw < 1.0f || bh < 1.0f)
                    continue;

                cx = Math.min(Math.max(cx, 0f), originalImageWidth)
                cy = Math.min(Math.max(cy, 0f), originalImageHeight)
                bw = Math.min(Math.max(bw, 0f), originalImageWidth)
                bh = Math.min(Math.max(bh, 0f), originalImageHeight)

                val angleDegrees = (angle / Math.PI * 180f).toFloat()

                val obb = OrientedBoundingBox(cx, cy, bw, bh, angleDegrees)
                obbs.add(obb)
                scores.add(maxScore)
                labels.add(classId)
            }
        }

        //apply non-maximum suppression (NMS)
        val indices = ArrayList<Int>()
        val numBoxes = obbs.size

        if(numBoxes == 0){
            return detections
        }

        //Filter again
        val sortedIndices = ArrayList<Int>()
        for(i in 0 until numBoxes){
            if(scores[i] >= CONFIDENCE_THRESHOLD) {
                sortedIndices.add(i)
            }
        }

        if(sortedIndices.isEmpty()){
            return detections
        }

        //Very important step to ensure we are focused on the high confidence predictions first
        //THis does not look to be sorting properly: TODO
        sortedIndices.sortedWith( compareByDescending {
            scores[it]
        })

        val areas = ArrayList<Float>()
        for(i in 0 until numBoxes)
        {
            areas[i] = obbs[i].width * obbs[i].height
        }

        //https://github.com/ultralytics/ultralytics/blob/main/ultralytics/utils/metrics.py#L254

        val suppressed = ArrayList<Boolean>()
        for(i in 0 until sortedIndices.size){
            val currentIndex = sortedIndices[i]

            if(suppressed[currentIndex]){
                continue
            }

            indices.add(currentIndex)
            val obb = obbs[currentIndex]

            val x0 = obb.x
            val y1 = obb.y

            var a0 = obb.width.pow(2) / 12
            var b0 = obb.height.pow(2) / 12
            var c0 = obb.angle

            val cos0 = cos(c0.toDouble())
            val sin0 = sin(c0.toDouble())

            val cos0Squared = cos0.pow(2)
            val sin0Squared = sin0.pow(2)

            var a0f = a0 * cos0Squared + b0 * sin0Squared
            var b0f = a0 * sin0Squared + b0 * cos0Squared
            var c0f = (a0 - b0) * cos0 * sin0

            for(j in i + 1 until sortedIndices.size){
                val compareIdx = sortedIndices[j]

                val compareObb = obbs[compareIdx]

                val x1 = compareObb.x
                val y1 = compareObb.y

                val a1 = compareObb.width.pow(2) / 12
                val b1 = compareObb.height.pow(2) / 12
                val c1 = compareObb.angle

                val cos1 = cos(c1.toDouble())
                val sin1 = sin(c1.toDouble())
                val cos1Squared = cos1.pow(2)
                val sin1Squared = sin1.pow(2)

                var a1f = a1 * cos1Squared + b1 * sin1Squared
                var b1f = a1 * sin1Squared + b1 * cos1Squared
                var c1f = (a1 - b1) * cos1 * sin1

            }


        }

        return detections
    }

    /**
     * Helper function used to map the coordinates for objects coming out of
     * the model into the coordinates that the user sees on the screen.
     */
    private fun mapOutputCoordinates(location: OrientedBoundingBoxNMS, originalImageWidth: Float, originalImageHeight: Float): RectF {

       // Step 1: map location to the preview coordinates
        val previewLocation = RectF(
            location.x0 * originalImageWidth,
            location.y0 * originalImageHeight,
            location.x1 * originalImageWidth,
            location.y1 * originalImageHeight
        )

        // Step 2: compensate for camera sensor orientation and mirroring
        //Note: We skipped step two because we only support back camera views

        // Step 3: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
        val margin = 0.1f
        val requestedRatio = 4f / 3f

        val midX = (previewLocation.left + previewLocation.right) / 2f
        val midY = (previewLocation.top + previewLocation.bottom) / 2f

        return if (originalImageWidth < originalImageHeight) {
            RectF(
                midX - (1f + margin) * requestedRatio * previewLocation.width() / 2f,
                midY - (1f - margin) * previewLocation.height() / 2f,
                midX + (1f + margin) * requestedRatio * previewLocation.width() / 2f,
                midY + (1f - margin) * previewLocation.height() / 2f
            )
        }
        else{
            RectF(
                midX - (1f - margin) * previewLocation.width() / 2f,
                midY - (1f + margin) * requestedRatio * previewLocation.height() / 2f,
                midX + (1f - margin) * previewLocation.width() / 2f,
                midY + (1f + margin) * requestedRatio * previewLocation.height() / 2f
            )
        }

        return previewLocation
    }



    fun bestBox(array: FloatArray) : List<BoundingBox> {

        val (_, tensorHeight, tensorWidth, _) = interpreter!!.getInputTensor(0).shape()
        Log.i(TAG, "Input tensor shape Height: $tensorHeight, Width: $tensorWidth")

        val outputShape = interpreter!!.getOutputTensor(0).shape()

        val batchSize = outputShape[0]
        val numChannel = outputShape[1]
        val numElements = outputShape[2]

        val numClasses = numChannel - 4
        val extra = numChannel - numClasses - 4
        val maskIndex = 4 + numClasses

        val xIndices = arrayOf(0 until numChannel)

        Log.i(TAG, "Output tensor shape, Index0: ${outputShape[0]} Channels: $numChannel, NumElements: $numElements")

        val boundingBoxes = mutableListOf<BoundingBox>()

        val x0 = array[0]
        val y0 = array[1]
        val w0 = array[2]
        val h0 = array[3]

        val con0 = array[4]
        val classId = array[5]

        val t = array[6]

        val angle = t % (Math.PI/2)
        //https://github.com/RNoahG/YoloXPythonAndroid/blob/main/Test3/app/src/main/java/de/yolox/anonymizationtest3/model/Model.kt
        for (c in 0 until numElements step batchSize * numChannel) {

            Log.i(TAG, "C: $c")
            var maxConf = CONFIDENCE_THRESHOLD
            var maxIdx = 0
            val objectness = array[c+4]

            for (id in 0 until labels().size){
                val conf = array[c+5+id] * objectness
                if (conf > maxConf){
                    maxIdx = id
                    maxConf = conf
                }
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels()[maxIdx]

                val cx = array[c] / tensorWidth
                val cy = array[c+1] / tensorWidth
                val w = array[c+2] / tensorWidth
                val h = array[c+3] / tensorWidth

                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)

                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName))
            }
        }

        if(boundingBoxes.isEmpty()) return boundingBoxes

//        for (c in 0 until numElements) {
//            var maxConf = CONFIDENCE_THRESHOLD
//            var maxIdx = -1
//            var j = 4
//            var arrayIdx = c + numElements * j
//            while (j < numChannel){
//                if (array[arrayIdx] > maxConf) {
//                    maxConf = array[arrayIdx]
//                    maxIdx = j - 4
//                }
//                j++
//                arrayIdx += numElements
//            }
//
//            if (maxConf > CONFIDENCE_THRESHOLD) {
//                //TODO somehow Array out of bounds except
//                //java.lang.ArrayIndexOutOfBoundsException: length=3; index=3
//                //at java.util.Arrays$ArrayList.get(Arrays.java:4599)
//                //at com.example.cs567_3d_ui_project.argis.mlutils.ObjectDetectionHelper.bestBox(ObjectDetectionHelper.kt:235)
//
//                var clsName : String = "Unknown"
//                if(maxIdx < labels().size){
//                     clsName = labels()[maxIdx]
//                }
//
//                val cx = array[c] // 0
//                val cy = array[c + numElements] // 1
//                val w = array[c + numElements * 2]
//                val h = array[c + numElements * 3]
//                val x1 = cx - (w/2F)
//                val y1 = cy - (h/2F)
//                val x2 = cx + (w/2F)
//                val y2 = cy + (h/2F)
//                if (x1 < 0F || x1 > 1F) continue
//                if (y1 < 0F || y1 > 1F) continue
//                if (x2 < 0F || x2 > 1F) continue
//                if (y2 < 0F || y2 > 1F) continue
//
//                boundingBoxes.add(
//                    BoundingBox(
//                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
//                        cx = cx, cy = cy, w = w, h = h,
//                        cnf = maxConf, cls = maxIdx, clsName = clsName
//                    )
//                )
//            }
//        }
//
//        if (boundingBoxes.isEmpty()) return boundingBoxes

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    private fun fitCenterBitmap(
        originalBitmap: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmapWithBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapWithBackground)
        canvas.drawColor(Color.TRANSPARENT)

        val scale: Float = height.toFloat() / originalBitmap.height
        val dstWidth = width * scale
        val processBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val scaledBitmap = Bitmap.createScaledBitmap(
            processBitmap, dstWidth.toInt(), height, true
        )

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val left = (width - dstWidth) / 2
        canvas.drawBitmap(scaledBitmap, left, 0f, paint)
        return bitmapWithBackground
    }

    private fun createTensorImage(
        bitmap: Bitmap, rotationDegrees: Int
    ): TensorImage {
        val rotation = -rotationDegrees / 90
        val imageProcessor = ImageProcessor
            .Builder()
            .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(CastOp(INPUT_IMAGE_TYPE))
            .add(Rot90Op(rotation)).build()

        // Preprocess the image and convert it into a TensorImage for classification.
        return imageProcessor.process(TensorImage.fromBitmap(bitmap))
    }

    private fun scaleBitmap(
        bitmap: Bitmap, width: Int, height: Int
    ): Bitmap {
        return fitCenterBitmap(bitmap, width, height)
    }

    /**
     * A tf.float32 tensor of shape [N, 4] containing bounding box coordinates
     * in the following order: [ymin, xmin, ymax, xmax]
     */
    private fun getBoundingBoxList(
        locations: FloatArray, width: Int, scaleRatio: Float
    ): Array<RectF> {
        val boundingBoxList = Array(locations.size / 4) { RectF() }
        val actualWidth = width * scaleRatio
        val padding = (width - width * scaleRatio) / 2

        for (i in boundingBoxList.indices) {
            val topRatio = locations[i * 4]
            val leftRatio = locations[i * 4 + 1]
            val bottomRatio = locations[i * 4 + 2]
            val rightRatio = locations[i * 4 + 3]

            val top = topRatio.coerceAtLeast(0f).coerceAtMost(1f)
            val left =
                ((leftRatio * width - padding) / actualWidth).coerceAtLeast(0f).coerceAtMost(1f)
            val bottom = bottomRatio.coerceAtLeast(top).coerceAtMost(1f)
            val right =
                ((rightRatio * width - padding) / actualWidth).coerceAtLeast(left).coerceAtMost(1f)

            val rectF = RectF(left, top, right, bottom)
            boundingBoxList[i] = rectF
        }

        return boundingBoxList;
    }

    // Wraps results from inference, the time it takes for inference to be performed, and
    // the input image and height for properly scaling UI to return back to callers
    data class DetectionResult(
        val detections: List<BoundingBox>,
        //val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    data class Detection(
        val label: String, val boundingBox: RectF, val score: Float
    )

    fun labels(): List<String>{
        return listOf(
            "Insulator",
            "Pole",
            "Wire")
        }
    }
