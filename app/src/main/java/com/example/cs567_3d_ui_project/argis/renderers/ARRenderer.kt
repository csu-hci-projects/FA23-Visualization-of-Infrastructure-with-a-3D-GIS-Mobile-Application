package com.example.cs567_3d_ui_project.argis.renderers

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.example.cs567_3d_ui_project.argis.buffers.FrameBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(glSurfaceView: GLSurfaceView, renderer: Renderer, assetManager: AssetManager) {

    private val TAG : String? = ARRenderer::class.simpleName
    private var viewPortWidth: Int = 1
    private var viewPortHeight: Int = 1

    init {
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        glSurfaceView.setRenderer(object: GLSurfaceView.Renderer{
            override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
                GLES30.glClearColor(1.0f, 1.0f, 0.4f, 0.4f)
                //GLES30.glEnable(GLES30.GL_BLEND)
                //renderer.onSurfaceCreated(this@ARRenderer)
            }

            override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
                GLES30.glViewport(0,0,w,h)
//                viewPortHeight = h
//                viewPortWidth = w
//                renderer.onSurfaceChanged(this@ARRenderer, w, h)
            }

            override fun onDrawFrame(p0: GL10?) {
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            }

        })
    }

    fun clear(frameBuffer: FrameBuffer, r: Float, g: Float, b: Float, a: Float){

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