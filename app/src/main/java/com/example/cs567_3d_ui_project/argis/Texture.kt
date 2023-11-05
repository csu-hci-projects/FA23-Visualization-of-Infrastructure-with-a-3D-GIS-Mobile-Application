package com.example.cs567_3d_ui_project.argis

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import com.otaliastudios.opengl.types.ByteBuffer
import java.io.Closeable

class Texture(renderer: ARRenderer, target: Target, wrapMode: WrapMode, useMipMaps: Boolean=true) : Closeable {

    private var target: Target
    private var textureId: IntArray = arrayOf(0).toIntArray()

    companion object{
        val TAG = Texture.javaClass.simpleName

        fun createFromAsset(renderer: ARRenderer, assetFileName: String, wrapMode: WrapMode, colorFormat: ColorFormat): Texture{
            val texture = Texture(renderer, Target.TEXTURE_2D, wrapMode)
            var bitmap : Bitmap? = null
            try{
                bitmap = convertBitMapToConfig(BitmapFactory.decodeStream(renderer.getAssets().open(assetFileName)),
                    Bitmap.Config.ARGB_8888)

                val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
                bitmap.copyPixelsToBuffer(buffer)
                buffer.rewind()

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.getTextureId())
                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    colorFormat.ordinal,
                    bitmap.width,
                    bitmap.height,
                    0,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    buffer
                )

                GLError.maybeThrowGLException("Failed to populate texture data", "glTexImage2D")
                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
                GLError.maybeThrowGLException("Failed to generate mipmaps", "glGenerateMipmap")
            }catch(e: Exception){
                texture.close()
                Log.e("Error While Creating From Asset", e.message.toString())
            }
            finally {
                bitmap?.recycle()
            }
            return texture
        }

        private fun convertBitMapToConfig(bitmap: Bitmap, config: Config) : Bitmap{
            if(bitmap.config == config){
                return bitmap
            }

            val result = bitmap.copy(config, false)
            bitmap.recycle()
            return result
        }
    }


    init {
        this.target = target

        GLES30.glGenTextures(1, textureId, 0)
        GLError.maybeThrowGLException("Texture creation failed", "glGenTextures")

        val minFilter = when(useMipMaps){
            true -> GLES30.GL_LINEAR_MIPMAP_LINEAR
            false -> GLES30.GL_LINEAR
        }

        try{
            GLES30.glBindTexture(this.target.ordinal, textureId[0])
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_MIN_FILTER, minFilter)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_WRAP_S, wrapMode.ordinal)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_WRAP_T, wrapMode.ordinal)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
        }
        catch (e: Exception){
            Log.e("Error in Initializing Texture", e.message.toString())
        }
    }

    enum class Target(glTexture2d: Int) {
        TEXTURE_2D(GLES30.GL_TEXTURE_2D),
        TEXTURE_EXTERNAL_OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),
        TEXTURE_CUBE_MAP(GLES30.GL_TEXTURE_CUBE_MAP)
    }

    enum class WrapMode(glClampToEdge: Int) {
        CLAMP_TO_EDGE(GLES30.GL_CLAMP_TO_EDGE),
        MIRRORED_REPEAT(GLES30.GL_MIRRORED_REPEAT),
        REPEAT(GLES30.GL_REPEAT)
    }

    enum class ColorFormat(glesEnum: Int){
        LINEAR(GLES30.GL_RGBA8),
        SRGB(GLES30.GL_SRGB8_ALPHA8)
    }

    fun getTextureId(): Int{
        return textureId[0]
    }

    override fun close() {
        if(textureId[0] != 0){
            GLES30.glDeleteTextures(1, textureId, 0)
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free texture", "glDeleteTextures")
            textureId[0] = 0
        }
    }

    fun getTarget(): Target{
        return target
    }


}