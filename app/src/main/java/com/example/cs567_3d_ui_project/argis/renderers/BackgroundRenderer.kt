package com.example.cs567_3d_ui_project.argis.renderers

import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BackgroundRenderer(renderer: ARRenderer) {

    private var cameraDepthTexture : Texture
    var cameraColorTexture: Texture
    private lateinit var depthColorPaletteTexture: Texture

    private var cameraTexCoordsVertexBuffer: VertexBuffer

    private var backgroundShader: Shader? = null

    private var useDepthVisualization: Boolean = false


    companion object{
        val TAG: String = BackgroundRenderer.javaClass.simpleName

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

    fun setUseDepthVisualization(renderer: ARRenderer, useDepthVisualization: Boolean){
        if(backgroundShader != null){

            backgroundShader!!.close()
            backgroundShader = null
            this.useDepthVisualization = useDepthVisualization
        }

        if(useDepthVisualization){
            depthColorPaletteTexture = Texture.createFromAsset(
                renderer,
                "models/depth_color_palette.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.LINEAR)
        }
        else{

        }
    }

}