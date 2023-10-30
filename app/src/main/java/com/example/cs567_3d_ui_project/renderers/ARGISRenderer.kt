package com.example.cs567_3d_ui_project.renderers

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARGISRenderer(val activity: ARGISActivity): GLSurfaceView.Renderer, DefaultLifecycleObserver {
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(1.0f, 1.0f, 0.4f, 0.4f)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
       GLES20.glViewport(0,0,width,height)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }
}