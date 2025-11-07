package com.example.cs567_3d_ui_project.rawdepth.rendering

import android.opengl.GLES20
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.activities.DepthAPIActivity
import com.example.cs567_3d_ui_project.argis.helpers.DisplayRotationHelper
import com.example.cs567_3d_ui_project.argis.renderers.ARGISRenderer
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.io.IOException


class DepthAPIActivityRenderer(val activity: DepthAPIActivity):
    ARRenderer.Renderer,
    DefaultLifecycleObserver {


    private val displayRotationHelper: DisplayRotationHelper = DisplayRotationHelper(activity)

    companion object {
        val TAG: String = ARGISRenderer::class.java.simpleName
        val depthRenderer: DepthRenderer = DepthRenderer()
        val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    }

    private val mySession get() = activity.depthApiLifecycleHelper.mySession

    override fun onSurfaceCreated(render: ARRenderer?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer?.createOnGlThread( /*context=*/activity)
            //depthRenderer?.createOnGlThread( /*context=*/activity)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }

    }

    override fun onSurfaceChanged(render: ARRenderer?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height);
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(render: ARRenderer?) {
        try {
            val frame = mySession?.update()
            val camera = frame?.camera

            // If the frame is ready, render the camera preview image to the GL surface.
            backgroundRenderer?.draw(frame)

            // Retrieve the depth data for this frame.
           // val points = DepthData.create(frame, mySession?.createAnchor(camera?.pose));

            // Clear screen to notify driver it should not load any pixels from previous frame.
            //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            //GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

            // Visualize depth points.
            //depthRenderer?.update(points);
            //depthRenderer?.draw(camera!!);

        }catch (e: Exception){
            Log.e(TAG, "Exception drawing frame in DepthAPIActivity")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }
}