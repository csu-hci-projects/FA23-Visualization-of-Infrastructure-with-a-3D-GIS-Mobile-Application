package com.example.cs567_3d_ui_project.argis.renderers

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.cs567_3d_ui_project.argis.GLError
import com.example.cs567_3d_ui_project.argis.Mesh
import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.buffers.Framebuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(glSurfaceView: GLSurfaceView, renderer: Renderer, assetManager: AssetManager) {

    private var viewPortWidth: Int = 1
    private var viewPortHeight: Int = 1

    private val assetManager: AssetManager

    companion object{
        private val TAG : String? = ARRenderer::class.simpleName
    }

    init {
        this.assetManager = assetManager
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        glSurfaceView.setRenderer(object: GLSurfaceView.Renderer {
            override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLError.maybeThrowGLException("Failed to enable blending", "glEnable")
                renderer.onSurfaceCreated(this@ARRenderer)
            }

            override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
                viewPortHeight = h
                viewPortWidth = w
                renderer.onSurfaceChanged(this@ARRenderer, w, h)
            }

            override fun onDrawFrame(p0: GL10?) {
                clear(null, 0f, 0f, 0f, 0f)
                renderer.onDrawFrame(this@ARRenderer)
            }

        })

        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setWillNotDraw(false)
    }

    fun draw(mesh: Mesh?, shader: Shader?, frameBuffer: Framebuffer? = null){
        useFrameBuffer(frameBuffer)
        shader!!.lowLevelUse()
        mesh!!.lowLevelDraw()
        Log.i("Draw-LowLevel", "Drew Object")

    }

    fun clear(frameBuffer: Framebuffer?, r: Float, g: Float, b: Float, a: Float){
        useFrameBuffer(frameBuffer)
        GLES30.glClearColor(r, g, b, a)
        GLError.maybeThrowGLException("Failed to set clear color", "glClearColor")
        GLES30.glDepthMask(true)
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear")
    }

    fun getAssets(): AssetManager{
        return assetManager
    }

    private fun useFrameBuffer(frameBuffer: Framebuffer?){
        val frameBufferId: Int
        val viewPortWidth: Int
        val viewPortHeight: Int

        if(frameBuffer == null){
            frameBufferId = 0
            viewPortHeight = this.viewPortHeight
            viewPortWidth = this.viewPortWidth
        }
        else{
            frameBufferId = frameBuffer.framebufferId
            viewPortWidth = frameBuffer.width
            viewPortHeight = frameBuffer.height
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
        GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFrameBuffer")
        GLES30.glViewport(0, 0, viewPortWidth, viewPortHeight)
        GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport")
    }

    /** Interface to be implemented for rendering callbacks.  */
     interface Renderer {
        /**
         * Called by [ARRenderer] when the GL render surface is created.
         *
         *
         * See [GLSurfaceView.Renderer.onSurfaceCreated].
         */
        fun onSurfaceCreated(render: ARRenderer?)

        /**
         * Called by [ARRenderer] when the GL render surface dimensions are changed.
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
         * Called by [ARRenderer] when a GL frame is to be rendered.
         *
         *
         * See [GLSurfaceView.Renderer.onDrawFrame].
         */
        fun onDrawFrame(render: ARRenderer?)
    }

}