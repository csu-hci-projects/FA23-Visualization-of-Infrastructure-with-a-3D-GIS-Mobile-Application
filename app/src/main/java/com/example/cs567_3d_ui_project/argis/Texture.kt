package com.example.cs567_3d_ui_project.argis

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer

class Texture(renderer: ARRenderer, target: Target, wrapMode: WrapMode, useMipMaps: Boolean=true) {

    init {
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
}