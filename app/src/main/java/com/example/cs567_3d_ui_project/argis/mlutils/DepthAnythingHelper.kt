package com.example.cs567_3d_ui_project.argis.mlutils

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.FloatBuffer

//Code for this class was found here: https://github.com/shubham0204/Depth-Anything-Android
class DepthAnything(context: Context,
                    model: Model = MODEL_DEFAULT
) {

    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession = ortEnvironment.createSession(context.assets.open(model.fileName).readBytes())
    private val inputName = ortSession.inputNames.iterator().next()

    private val inputDim: Int
    private val outputDim: Int


    companion object{
        val MODEL_DEFAULT = Model.FusedModelQ4_F16_256
        //val MODEL_DEFAULT = Model.DepthAnythingV2_VITS_Outdoor_Dynamic
    }

    enum class Model(val fileName: String) {
        FusedModelQ4_F16_256("fused_model_q4f16_256.onnx"),
        //https://github.com/fabio-sim/Depth-Anything-ONNX/releases
        DepthAnythingV2_VITS_Outdoor_Dynamic("depth_anything_v2_vits_outdoor_dynamic.onnx"),
        DepthAnythingV2_VITS("depth_anything_v2_vits.onnx")
    }

    init {
        when {
            model.fileName.contains("_256") -> {
                inputDim = 256
                outputDim = 252
            }
            model.fileName.contains("_512") -> {
                inputDim = 512
                outputDim = 504
            }
            model.fileName.contains("vit") -> {
                inputDim = 512
                outputDim = 512
            }

            else -> throw IllegalArgumentException("Unsupported model size")
        }
    }

    private val rotateTransform = Matrix().apply { postRotate(180f) }

    suspend fun predict(inputImage: Bitmap): Pair<Bitmap, Long> =
        withContext(Dispatchers.Default) {

            val inputTensor: OnnxTensor

            when(MODEL_DEFAULT) {
                Model.DepthAnythingV2_VITS_Outdoor_Dynamic ->
                {
                    val imagePixels = convert2(inputImage)
                    inputTensor =
                        OnnxTensor.createTensor(
                            ortEnvironment,
                            imagePixels,
                            longArrayOf(1, 3, inputImage.height.toLong(), inputImage.width.toLong()),
                        )
                    val t1 = System.currentTimeMillis()
                    val outputs = ortSession.run(mapOf(inputName to inputTensor))
                    val inferenceTime = System.currentTimeMillis() - t1
                    val outputTensor = outputs[0] as OnnxTensor

                    var depthMap = Bitmap.createBitmap(inputImage.height, inputImage.width, Bitmap.Config.ALPHA_8)
                    depthMap.copyPixelsFromBuffer(outputTensor.byteBuffer)
                    depthMap = Bitmap.createBitmap(depthMap, 0, 0, inputImage.height, inputImage.width, rotateTransform, false)
                    //depthMap = Bitmap.createScaledBitmap(depthMap, inputImage.width, inputImage.height, true)
                    return@withContext Pair(depthMap, inferenceTime)
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
                            //OnnxJavaType.FLOAT16
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
                    return@withContext Pair(depthMap, inferenceTime)
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
}