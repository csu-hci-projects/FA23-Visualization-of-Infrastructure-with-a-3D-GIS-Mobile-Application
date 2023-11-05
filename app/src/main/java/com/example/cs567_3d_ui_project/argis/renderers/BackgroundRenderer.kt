package com.example.cs567_3d_ui_project.argis.renderers

import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BackgroundRenderer(renderer: ARRenderer) {

    private var cameraDepthTexture : Texture
    private var cameraColorTexture: Texture
    private lateinit var depthColorPaletteTexture: Texture

    private var cameraTexCoordsVertexBuffer: VertexBuffer

    companion object{
        private val COORDS_BUFFER_SIZE : Int = 2 * 4 * 4

        private val NDC_QUAD_COORDS_BUFFER = ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        private val VIRTUAL_SCENE_TEX_COORDS_BUFFER = ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        init{
            NDC_QUAD_COORDS_BUFFER.put(floatArrayOf(-1f, -1f, +1f, -1f, -1f, +1f, +1f, +1f))
            VIRTUAL_SCENE_TEX_COORDS_BUFFER.put(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
        }

    }


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

        var screenCoordsVertexBuffer = VertexBuffer(renderer, 2, NDC_QUAD_COORDS_BUFFER)

        cameraTexCoordsVertexBuffer = VertexBuffer(renderer, 2, null)

        var virtualSceneTexCoordsVertexBuffer = VertexBuffer(renderer, 2, VIRTUAL_SCENE_TEX_COORDS_BUFFER)

        var vertexBuffers = arrayOf(screenCoordsVertexBuffer, cameraTexCoordsVertexBuffer ,virtualSceneTexCoordsVertexBuffer)

    }

}