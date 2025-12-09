package com.example.cs567_3d_ui_project.argis.mlutils

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import androidx.core.graphics.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.FloatBuffer

//Code for this class was found here: https://github.com/shubham0204/Depth-Anything-Android
class DepthAnything(val context: Context,
                    val model: Model = MODEL_DEFAULT
) {

    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession = ortEnvironment.createSession(context.assets.open(model.fileName).readBytes())
    private val inputName = ortSession.inputNames.iterator().next()

    private val inputDim: Int
    private val outputDim: Int


    companion object{
        //val MODEL_DEFAULT = Model.FusedModelQ4_F16_256
        //val MODEL_DEFAULT = Model.DepthAnythingV2_VITS_Outdoor_Dynamic
        val MODEL_DEFAULT = Model.DepthAnythingV2_VITS_256_Outdoor_Dynamic_NOPOST
        //val MODEL_DEFAULT = Model.DepthAnythingV2_VITS_Outdoor_Dynamic_NOPOST
    }

    enum class Model(val fileName: String) {
        FusedModelQ4_F16_256("fused_model_q4f16_256.onnx"),
        //https://github.com/fabio-sim/Depth-Anything-ONNX/releases
        //Exported the outdoor model using onnx with preprocessing
        DepthAnythingV2_VITS_256_Outdoor_Dynamic("fused_model_vits_256_outdoor_dynamic.onnx"),
        //This No Post Processing version was derived by us through the use of these scripts
        //https://github.com/shubham0204/Google_Colab_Notebooks/blob/main/Depth_Anything_FusedOps_ONNX_Model.ipynb
        DepthAnythingV2_VITS_256_Outdoor_Dynamic_NOPOST("fused_model_vits_256_outdoor_dynamic_no_post.onnx"),
        DepthAnythingV2_VITS_Outdoor_Dynamic_NOPOST("fused_model_vits_outdoor_dynamic_nopost.onnx")
    }

    init {
        when {
            model.fileName.contains("fused_model_q4f16_256") -> {
                inputDim = 256
                outputDim = 252
            }
            model.fileName.contains("_512") -> {
                inputDim = 512
                outputDim = 504
            }
            model.fileName.contains("fused_model_vits_outdoor_dynamic") -> {
                inputDim = 518
                outputDim = 518
            }
            model.fileName.contains("fused_model_vits_256_outdoor_dynamic") -> {
                inputDim = 256
                outputDim = 252
            }

            else -> throw IllegalArgumentException("Unsupported model size")
        }
    }

    private val rotateTransform = Matrix().apply { postRotate(-90f) }

    suspend fun predict(inputImage: Bitmap): DepthAnythingPrediction =
        withContext(Dispatchers.IO) {

            val inputTensor: OnnxTensor

            when(MODEL_DEFAULT) {
//                Model.DepthAnythingV2_VITS_Outdoor_Dynamic ->
//                {
//                    val resizedImage = Bitmap.createScaledBitmap(
//                        inputImage,
//                        inputDim,
//                        inputDim,
//                        true)
//
//                    val imagePixels = convert(resizedImage)
//                    inputTensor = OnnxTensor.createTensor(
//                            ortEnvironment,
//                            imagePixels,
//                            longArrayOf(1, resizedImage.height.toLong(), resizedImage.width.toLong(),  3),
//                            OnnxJavaType.UINT8
//                        )
//                    val t1 = System.currentTimeMillis()
//                    val outputs = ortSession.run(mapOf(inputName to inputTensor))
//                    val inferenceTime = System.currentTimeMillis() - t1
//                    val outputTensor = outputs[0] as OnnxTensor
//
//                    var depthMap = Bitmap.createBitmap(resizedImage.height, resizedImage.width, Bitmap.Config.ALPHA_8)
//                    depthMap.copyPixelsFromBuffer(outputTensor.byteBuffer)
//                    depthMap = Bitmap.createBitmap(depthMap, 0, 0, resizedImage.height, resizedImage.width, rotateTransform, false)
//                    depthMap = Bitmap.createScaledBitmap(depthMap, resizedImage.width, resizedImage.height, true)
//
//                    //https://stackoverflow.com/questions/36493977/flip-a-bitmap-image-horizontally-or-vertically
//                    val cx = depthMap.width / 2f
//                    val cy = depthMap.height / 2f
//                    val matrix = Matrix().apply {
//                        postScale(1f, -1f, cx, cy)
//                    }
//                    depthMap = Bitmap.createBitmap(depthMap, 0, 0, depthMap.width, depthMap.height, matrix, true)
//                    return@withContext DepthAnythingPrediction(depthMap, inferenceTime, outputTensor)
//                }

                Model.DepthAnythingV2_VITS_Outdoor_Dynamic_NOPOST -> {

                    val resizedImage = Bitmap.createScaledBitmap(
                        inputImage,
                        inputDim,
                        inputDim,
                        true)

                    val imagePixels = convert(resizedImage)

                    inputTensor =
                        OnnxTensor.createTensor(
                            ortEnvironment,
                            imagePixels,
                            longArrayOf(1, inputDim.toLong(), inputDim.toLong(), 3),
                            OnnxJavaType.UINT8)

                    val t1 = System.currentTimeMillis()
                    val outputs = ortSession.run(mapOf(inputName to inputTensor))
                    val inferenceTime = System.currentTimeMillis() - t1
                    val outputTensor = outputs[0] as OnnxTensor
                    val buffer = outputTensor.floatBuffer

//                    if(context.generateDepthBitmap){
//                        var depthMap = Bitmap.createBitmap(outputDim, outputDim, Bitmap.Config.ALPHA_8)
//                        depthMap.copyPixelsFromBuffer(outputTensor.byteBuffer)
//                        depthMap = Bitmap.createBitmap(depthMap, 0, 0, outputDim, outputDim, rotateTransform, false)
//                        depthMap = Bitmap.createScaledBitmap(depthMap, inputImage.width, inputImage.height, true)
//
//                        //https://stackoverflow.com/questions/36493977/flip-a-bitmap-image-horizontally-or-vertically
//                        val cx = depthMap.width / 2f
//                        val cy = depthMap.height / 2f
//                        val matrix = Matrix().apply {
//                            postScale(1f, -1f, cx, cy)
//                        }
//
//                        depthMap = Bitmap.createBitmap(depthMap, 0, 0, depthMap.width, depthMap.height, matrix, true)
//                        return@withContext DepthAnythingPrediction(depthMap, inferenceTime, outputTensor)
//                    }
                    Log.i("Test", "$buffer")
                    return@withContext DepthAnythingPrediction(null, inferenceTime, outputTensor.floatBuffer)
                }

                Model.DepthAnythingV2_VITS_256_Outdoor_Dynamic_NOPOST -> {

                    val resizedImage = Bitmap.createScaledBitmap(
                        inputImage,
                        inputDim,
                        inputDim,
                        true)

                    val imagePixels = convert(resizedImage)

                    inputTensor =
                        OnnxTensor.createTensor(
                            ortEnvironment,
                            imagePixels,
                            longArrayOf(1, inputDim.toLong(), inputDim.toLong(), 3),
                            OnnxJavaType.UINT8)

                    val t1 = System.currentTimeMillis()
                    val outputs = ortSession.run(mapOf(inputName to inputTensor))
                    val inferenceTime = System.currentTimeMillis() - t1
                    val outputTensor = outputs[0] as OnnxTensor
                    val buffer = outputTensor.floatBuffer

//                    if(context.generateDepthBitmap){
//                        var depthMap = Bitmap.createBitmap(outputDim, outputDim, Bitmap.Config.ALPHA_8)
//                        depthMap.copyPixelsFromBuffer(outputTensor.byteBuffer)
//                        depthMap = Bitmap.createBitmap(depthMap, 0, 0, outputDim, outputDim, rotateTransform, false)
//                        depthMap = Bitmap.createScaledBitmap(depthMap, inputImage.width, inputImage.height, true)
//
//                        //https://stackoverflow.com/questions/36493977/flip-a-bitmap-image-horizontally-or-vertically
//                        val cx = depthMap.width / 2f
//                        val cy = depthMap.height / 2f
//                        val matrix = Matrix().apply {
//                            postScale(1f, -1f, cx, cy)
//                        }
//
//                        depthMap = Bitmap.createBitmap(depthMap, 0, 0, depthMap.width, depthMap.height, matrix, true)
//                        return@withContext DepthAnythingPrediction(depthMap, inferenceTime, outputTensor)
//                    }
                    Log.i("Test", "$buffer")
                    return@withContext DepthAnythingPrediction(null, inferenceTime, outputTensor.floatBuffer)
                }

                else -> {
                    val resizedImage = Bitmap.createScaledBitmap(
                        inputImage,
                        inputDim,
                        inputDim,
                        true)

                    val imagePixels = convert(resizedImage)

                    inputTensor =
                        OnnxTensor.createTensor(
                            ortEnvironment,
                            imagePixels,
                            longArrayOf(1, inputDim.toLong(), inputDim.toLong(), 3),
                            OnnxJavaType.UINT8)

                    val t1 = System.currentTimeMillis()
                    val outputs = ortSession.run(mapOf(inputName to inputTensor))
                    val inferenceTime = System.currentTimeMillis() - t1
                    val outputTensor = outputs[0] as OnnxTensor

                    var depthMap = Bitmap.createBitmap(outputDim, outputDim, Bitmap.Config.ALPHA_8)
                    depthMap.copyPixelsFromBuffer(outputTensor.byteBuffer)
                    depthMap = Bitmap.createBitmap(depthMap, 0, 0, outputDim, outputDim, rotateTransform, false)
                    depthMap = Bitmap.createScaledBitmap(depthMap, inputImage.width, inputImage.height, true)

                    //https://stackoverflow.com/questions/36493977/flip-a-bitmap-image-horizontally-or-vertically
                    val cx = depthMap.width / 2f
                    val cy = depthMap.height / 2f
                    val matrix = Matrix().apply {
                        postScale(1f, -1f, cx, cy)
                    }

                    depthMap = Bitmap.createBitmap(depthMap, 0, 0, depthMap.width, depthMap.height, matrix, true)
                    return@withContext DepthAnythingPrediction(depthMap, inferenceTime, outputTensor.floatBuffer)
                }
            }
        }

    private fun convert(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocate(1 * bitmap.width * bitmap.height * 3)
        imgData.rewind()
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                imgData.put(Color.red(bitmap[i, j]).toByte())
                imgData.put(Color.blue(bitmap[i, j]).toByte())
                imgData.put(Color.green(bitmap[i, j]).toByte())
            }
        }
        imgData.rewind()
        return imgData
    }

    private fun convert2(bitmap: Bitmap): FloatBuffer {
        val imgData = FloatBuffer.allocate(1 * bitmap.width * bitmap.height * 3)
        imgData.rewind()
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                imgData.put(Color.red(bitmap[i, j]).toFloat())
                imgData.put(Color.green(bitmap[i, j]).toFloat())
                imgData.put(Color.blue(bitmap[i, j]).toFloat())
            }
        }
        imgData.rewind()
        return imgData
    }

    data class DepthAnythingPrediction(
        val depthImage: Bitmap?,
        val inferenceTime: Long,
        val depthReadings: FloatBuffer?
    )
}