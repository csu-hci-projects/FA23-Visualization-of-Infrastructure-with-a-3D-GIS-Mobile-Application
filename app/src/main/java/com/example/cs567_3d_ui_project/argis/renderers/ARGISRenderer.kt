package com.example.cs567_3d_ui_project.argis.renderers

import android.opengl.GLES30
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import com.example.cs567_3d_ui_project.argis.GLError
import com.example.cs567_3d_ui_project.argis.Texture
import com.google.ar.core.Anchor
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.IOException
import java.nio.ByteBuffer

class ARGISRenderer(val activity: ARGISActivity):
    ARRenderer.Renderer,
    DefaultLifecycleObserver {

    lateinit var render: ARRenderer
    lateinit var backgroundRenderer: BackgroundRenderer

    lateinit var dfgTexture: Texture

    var hasSetTextureNames = false

    companion object{
        val TAG: String = ARGISRenderer::class.java.simpleName
    }

    val session
        get() = activity.arGISSessionHelper.mySession

    override fun onSurfaceCreated(render: ARRenderer?) {
        try{
            this.render = render!!
            backgroundRenderer = BackgroundRenderer(render)

            dfgTexture = Texture(
                render,
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,
                false)

            val dfgResolution = 64
            val dfgChannels = 2
            val halfFloatSize = 2

            val buffer: ByteBuffer = ByteBuffer
                .allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize)
            activity.assets.open("models/dfg.raw").use { it.read(buffer.array()) }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId())
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture")
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RG16F,
                dfgResolution,
                dfgResolution,
                0,
                GLES30.GL_RG,
                GLES30.GL_HALF_FLOAT,
                buffer)
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D")
        }
        catch (e:Exception){
            Log.e(TAG, "Failed to read a required asset file")
        }
    }

    override fun onSurfaceChanged(render: ARRenderer?, width: Int, height: Int) {
        Log.i("OnSurfaceChanged", "Changed")
    }

    override fun onDrawFrame(renderer: ARRenderer?) {
        val session = session ?: return

        if(!hasSetTextureNames){
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.getTextureId()))
            hasSetTextureNames = true
        }

        val frame =
            try{
                session.update()
            }
            catch (e: CameraNotAvailableException){
                Log.e(TAG, "Camera not available during onDrawFrame", e)
                return
            }

        val camera = frame.camera

        try{
            backgroundRenderer.setUseDepthVisualization(renderer!!,
                activity.depthSettings.depthColorVisualizationEnabled)

            backgroundRenderer.setUseOcclusion(renderer, activity.depthSettings.useDepthForOcclusion())
        }
        catch (e: IOException){
            Log.e(TAG, "Failed to read a required asset file", e)
            return
        }

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)

        val shouldGetDepthImage = activity.depthSettings.useDepthForOcclusion() ||
        activity.depthSettings.depthColorVisualizationEnabled()

        if(camera.trackingState == TrackingState.TRACKING && shouldGetDepthImage){
            try{
                val depthImage = frame.acquireDepthImage16Bits()
                backgroundRenderer.updateCameraDepthTexture(depthImage)
                depthImage.close()
            }
            catch (e: NotYetAvailableException){
                Log.w("Update Camera Depth Texture", e.message.toString())
            }
        }


        //Draw background
        if(frame.timestamp != 0L){
            //Suppress renderering if the camera did not produce the first frame yet.
            backgroundRenderer.drawBackground(renderer)
        }

        if(camera.trackingState == TrackingState.PAUSED){
            return
        }

        //The rest of the code in Hello AR Kotlin is setting up shaders for the GL stuff
        //that it renders. There are good things to potentially crib from in there but we will skip for now.
    }

}

data class WrappedAnchor(
    val anchor: Anchor,
    val trackable: Trackable
)

//fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?){
//    GLES20.glClearColor(1.0f, 1.0f, 0.4f, 0.4f)
//}
//
//fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
//    GLES20.glViewport(0,0,width,height)
//}
//
//override fun onDrawFrame(p0: GL10?) {
//    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//}