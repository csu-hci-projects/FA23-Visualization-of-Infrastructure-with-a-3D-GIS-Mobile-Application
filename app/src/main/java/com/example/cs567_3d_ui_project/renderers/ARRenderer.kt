package com.example.cs567_3d_ui_project.renderers

import android.content.res.AssetManager
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer

class ARRenderer(glSurfaceView: GLSurfaceView, renderer: Renderer, assetManager: AssetManager) {

    val TAG : String? = ARRenderer::class.simpleName

    init {
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
    }

    /** Interface to be implemented for rendering callbacks.  */
    interface Renderer {
        /**
         * Called by [SampleRender] when the GL render surface is created.
         *
         *
         * See [GLSurfaceView.Renderer.onSurfaceCreated].
         */
        fun onSurfaceCreated(render: ARRenderer?)

        /**
         * Called by [SampleRender] when the GL render surface dimensions are changed.
         *
         *
         * See [GLSurfaceView.Renderer.onSurfaceChanged].
         */
        fun onSurfaceChanged(
            render: ARRenderer?,
            width: Int,
            height: Int
        )

        /**
         * Called by [SampleRender] when a GL frame is to be rendered.
         *
         *
         * See [GLSurfaceView.Renderer.onDrawFrame].
         */
        fun onDrawFrame(render: ARRenderer?)
    }

}