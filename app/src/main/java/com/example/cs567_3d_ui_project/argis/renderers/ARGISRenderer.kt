package com.example.cs567_3d_ui_project.argis.renderers

import androidx.lifecycle.DefaultLifecycleObserver
import com.example.cs567_3d_ui_project.activities.ARGISActivity

class ARGISRenderer(val activity: ARGISActivity):
    ARRenderer.Renderer,
    DefaultLifecycleObserver {
    override fun onSurfaceCreated(render: ARRenderer?) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceChanged(render: ARRenderer?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onDrawFrame(render: ARRenderer?) {
        TODO("Not yet implemented")
    }

}

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