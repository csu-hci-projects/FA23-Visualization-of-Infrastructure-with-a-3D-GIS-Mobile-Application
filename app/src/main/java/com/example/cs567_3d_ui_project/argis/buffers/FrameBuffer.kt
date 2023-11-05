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

    companion object{
        val TAG: String = FrameBuffer.javaClass.simpleName
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
        }
        catch (e: Exception){
            close()
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
}