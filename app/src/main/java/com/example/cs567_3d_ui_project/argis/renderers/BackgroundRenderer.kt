package com.example.cs567_3d_ui_project.argis.renderers

import com.example.cs567_3d_ui_project.argis.Texture

class BackgroundRenderer(renderer: ARRenderer) {

    private var cameraDepthTexture : Texture
    private var cameraColorTexture: Texture
    private lateinit var depthColorPaletteTexture: Texture

    init {
        cameraDepthTexture = Texture(
            renderer,
            Texture.Target.TEXTURE_2D,
            Texture.WrapMode.CLAMP_TO_EDGE,
            false)

        cameraColorTexture = Texture(
            renderer,
            Texture.Target.TEXTURE_EXTERNAL_OES,
            Texture.WrapMode.CLAMP_TO_EDGE,
            false
        )
    }

}