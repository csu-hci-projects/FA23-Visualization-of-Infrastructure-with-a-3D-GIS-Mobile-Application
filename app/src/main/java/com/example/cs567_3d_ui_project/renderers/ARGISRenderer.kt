package com.example.cs567_3d_ui_project.renderers

import android.opengl.GLSurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARGISRenderer(val activity: ARGISActivity): GLSurfaceView.Renderer, DefaultLifecycleObserver {
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun onDrawFrame(p0: GL10?) {
        TODO("Not yet implemented")
    }
}