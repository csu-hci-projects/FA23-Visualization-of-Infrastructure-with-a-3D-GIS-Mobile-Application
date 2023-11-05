package com.example.cs567_3d_ui_project.argis

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer

class Texture(renderer: ARRenderer, target: Target, wrapMode: WrapMode, useMipMaps: Boolean=true) {

    private var target: Target
    private var textureId: IntArray = arrayOf(0).toIntArray()

    init {
        this.target = target

        GLES30.glGenTextures(1, textureId, 0)

        val minFilter = when(useMipMaps){
            true -> GLES30.GL_LINEAR_MIPMAP_LINEAR
            false -> GLES30.GL_LINEAR
        }

        try{

            GLES30.glBindTexture(this.target.ordinal, textureId[0])

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_MIN_FILTER, minFilter)

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_WRAP_S, wrapMode.ordinal)

            GLES30.glTexParameteri(this.target.ordinal, GLES30.GL_TEXTURE_WRAP_T, wrapMode.ordinal)

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
}