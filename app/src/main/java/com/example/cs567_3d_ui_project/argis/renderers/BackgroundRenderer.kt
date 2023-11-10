package com.example.cs567_3d_ui_project.argis.renderers

import android.media.Image
import android.opengl.GLES30
import android.util.Log
import com.example.cs567_3d_ui_project.argis.Mesh
import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.FrameBuffer
import com.example.cs567_3d_ui_project.argis.buffers.VertexBuffer
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BackgroundRenderer(renderer: ARRenderer) {

    private var cameraDepthTexture : Texture
    var cameraColorTexture: Texture
    private lateinit var depthColorPaletteTexture: Texture

    private var cameraTexCoordsVertexBuffer: VertexBuffer

    private var backgroundShader: Shader? = null
    private var occlusionShader: Shader? = null

    private var useDepthVisualization: Boolean = false

    private var useOcclusion: Boolean = false
    private var aspectRatio: Float = 0.0F

    private val cameraTexCoords = ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var mesh: Mesh

    companion object{
        val TAG: String = BackgroundRenderer::class.java.simpleName

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

        var vertexBuffers: Array<VertexBuffer?> = arrayOf(screenCoordsVertexBuffer, cameraTexCoordsVertexBuffer ,virtualSceneTexCoordsVertexBuffer)

        mesh = Mesh(renderer, Mesh.PrimitiveMode.TRIANGLE_STRIP, null, vertexBuffers)

    }

    fun setUseDepthVisualization(renderer: ARRenderer, useDepthVisualization: Boolean){
        if(backgroundShader != null){

            if(this.useDepthVisualization == useDepthVisualization){
                return
            }

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

            backgroundShader =
                Shader.createFromAssets(renderer,
                    "shaders/background_show_depth_color_visualization.vert",
                    "shaders/background_show_depth_color_visualization.frag",
                    null)
                    .setTexture("u_CameraDepthTexture", cameraDepthTexture)
                    .setTexture("u_ColorMap", depthColorPaletteTexture)
                    .setDepthTest(false)
                    .setDepthWrite(false)

        }
        else{
            backgroundShader =
                Shader.createFromAssets(renderer,
                    "shaders/background_show_camera.vert",
                    "shaders/background_show_camera.frag",
                    null)
                    .setTexture("u_CameraColorTexture", cameraColorTexture)
                    .setDepthTest(false)
                    .setDepthWrite(false)
        }
    }

    fun setUseOcclusion(renderer: ARRenderer, useOcclusion: Boolean){
        if(occlusionShader != null){
            if(this.useOcclusion == useOcclusion){
                return
            }

            occlusionShader!!.close()
            occlusionShader = null
            this.useOcclusion = useOcclusion
        }

        val defines = HashMap<String, String>()
        val useOccVal = when(useOcclusion){
            true -> "1"
            false -> "1"
        }
        defines["USE_OCCLUSION"] = useOccVal

        occlusionShader =
            Shader.createFromAssets(renderer,
                "shaders/occlusion.vert",
                "shaders/occlusion.frag",
                defines)
                .setDepthTest(false)
                .setDepthWrite(false)
                .setBlend(Shader.BlendFactor.SRC_ALPHA, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA)

        if(useOcclusion){
            occlusionShader!!.setTexture("u_CameraDepthTexture", cameraDepthTexture)
                .setFloat("u_DepthAspectRatio", aspectRatio)
        }
    }

    fun updateDisplayGeometry(frame: Frame){
        if(frame.hasDisplayGeometryChanged()){

            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                NDC_QUAD_COORDS_BUFFER,
                Coordinates2d.TEXTURE_NORMALIZED,
                cameraTexCoords)

            cameraTexCoordsVertexBuffer.set(cameraTexCoords)
        }
    }

    fun updateCameraDepthTexture(image: Image){
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraDepthTexture.getTextureId())
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RG8,
            image.width,
            image.height,
            0,
            GLES30.GL_RG,
            GLES30.GL_UNSIGNED_BYTE,
            image.planes[0].buffer)

        if(useOcclusion){
            aspectRatio = image.width.toFloat() / image.height.toFloat()
            occlusionShader!!.setFloat("u_DepthAspectRatio", aspectRatio)
        }
    }

    fun drawBackground(renderer: ARRenderer){
        renderer.draw(mesh, backgroundShader)
    }

    fun drawVirtualScene(renderer: ARRenderer, virtualSceneFrameBuffer: FrameBuffer, zNear: Float, zFar: Float){
        try{
            occlusionShader!!.setTexture("u_VirtualSceneDepthTexture", virtualSceneFrameBuffer.getColorTexture())

            if(useOcclusion){
                occlusionShader!!.setTexture("use_VirtualSceneDepthTexture", virtualSceneFrameBuffer.getDepthTexture())
                    .setFloat("u_ZNear", zNear)
                    .setFloat("u_ZFar", zFar)
            }
            renderer.draw(mesh, occlusionShader)
        }catch (e: Exception){
            Log.e("Error on Drawing Virtual Scene", e.message.toString())
        }

    }

}