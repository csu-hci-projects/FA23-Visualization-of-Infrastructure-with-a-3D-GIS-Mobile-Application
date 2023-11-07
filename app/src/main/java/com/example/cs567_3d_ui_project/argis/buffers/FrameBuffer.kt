package com.example.cs567_3d_ui_project.argis.buffers

import android.opengl.GLES30
import android.util.Log
import com.example.cs567_3d_ui_project.argis.GLError
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.io.Closeable

class FrameBuffer(render: ARRenderer, width: Int, height: Int): Closeable{

    private lateinit var colorTexture: Texture
    private lateinit var depthTexture: Texture

    private val frameBufferId: IntArray = arrayOf(0).toIntArray()
    private var width: Int = -1
    private var height: Int = -1

    companion object{
        val TAG: String = FrameBuffer::class.java.simpleName
    }

    init{
        try{
            colorTexture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE, false)
            depthTexture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE, false)

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.getTextureId())
            GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_NONE)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            resize(width, height)

            GLES30.glGenFramebuffers(1, frameBufferId, 0)
            GLError.maybeThrowGLException("Framebuffer creation failed", "glGenFramebuffers")
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId[0])
            GLError.maybeThrowGLException("Failed to bind framebuffer", "glGenFramebuffers")
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                colorTexture.getTextureId(),
                0)
            GLError.maybeThrowGLException("Failed to bind color texture to framebuffer", "glFrameBufferTexture2D")
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_TEXTURE_2D,
                depthTexture.getTextureId(),
                0)
            GLError.maybeThrowGLException("Failed to bind depth texture to framebuffer", "glFramebufferTexture2D")

            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)

            if(status != GLES30.GL_FRAMEBUFFER_COMPLETE){
                throw IllegalStateException("FrameBuffer construction no complete: code $status")
            }


        }
        catch (e: Exception){
            close()
            Log.e("Failed to Initialize FrameBuffer", e.message.toString())
        }

    }

    override fun close() {
        if(frameBufferId[0] != 0){
            GLES30.glDeleteFramebuffers(1, frameBufferId, 0)
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free framebuffer", "glDeleteFrameBuffers")
            frameBufferId[0] = 0
        }
        colorTexture.close()
        depthTexture.close()
    }

    fun resize(width: Int, height: Int){
        if(this.width == width && this.height == height){
            return
        }

        this.width = width
        this.height = height

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture.getTextureId())
        GLError.maybeThrowGLException("Failed to bind color texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            width,
            height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            null)
        GLError.maybeThrowGLException("Failed to specify color texture format", "glTexImage2D")

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.getTextureId())
        GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_DEPTH_COMPONENT32F,
            width,
            height,
            0,
            GLES30.GL_DEPTH_COMPONENT,
            GLES30.GL_FLOAT,
            null)
        GLError.maybeThrowGLException("Failed to specify depth texture format", "glTexImage2D")
    }

    fun getFrameBufferId(): Int{
        return frameBufferId[0]
    }

    fun getWidth(): Int{
        return width
    }

    fun getHeight(): Int{
        return height
    }

    fun getColorTexture(): Texture{
        return colorTexture
    }

    fun getDepthTexture(): Texture{
        return depthTexture
    }
}