package com.example.cs567_3d_ui_project.argis.renderers

import android.opengl.GLES30
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import com.example.cs567_3d_ui_project.argis.GLError
import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.Framebuffer
import com.example.cs567_3d_ui_project.argis.helpers.DisplayRotationHelper
import com.example.cs567_3d_ui_project.argis.helpers.TrackingStateHelper
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.IOException
import java.nio.ByteBuffer

class ARGISRenderer(val activity: ARGISActivity):
    ARRenderer.Renderer,
    DefaultLifecycleObserver {

    private lateinit var render: ARRenderer
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var planeRenderer: PlaneRenderer

    private lateinit var dfgTexture: Texture
    private lateinit var cubeMapFilter: SpecularCubemapFilter

    private lateinit var virtualSceneFrameBuffer: Framebuffer
    private lateinit var virtualObjectShader: Shader
    private lateinit var virtualObjectAlbedoTexture: Texture

    private val displayRotationHelper: DisplayRotationHelper = DisplayRotationHelper(activity)

    private var hasSetTextureNames = false

    private val Z_Near = 0.1f
    private val Z_Far = 100f

    private val projectionMatrix = FloatArray(16)

    companion object{
        val TAG: String = ARGISRenderer::class.java.simpleName

        val APPROXIMATE_DISTANCE_METERS = 2.0f

        const val CUBEMAP_RESOLUTION = 16
        const val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32

        private val sphericalHarmonicFactors =
            floatArrayOf(
                0.282095f,
                -0.325735f,
                0.325735f,
                -0.325735f,
                0.273137f,
                -0.273137f,
                0.078848f,
                -0.273137f,
                0.136569f
            )
    }

    val session
        get() = activity.arGISSessionHelper.mySession

    private val trackingStateHelper = TrackingStateHelper(activity)
    private val wrappedAnchors = mutableListOf<WrappedAnchor>()
    val sphericalHarmonicsCoefficients = FloatArray(9 * 3)

    override fun onSurfaceCreated(render: ARRenderer?) {
        try{
            this.render = render!!
            //planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFrameBuffer = Framebuffer(render, 1, 1)

//            cubeMapFilter = SpecularCubemapFilter(render,
//                CUBEMAP_RESOLUTION,
//                CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES)

            dfgTexture = Texture(
                render,
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,
                false)

            val dfgResolution = 64
            val dfgChannels = 2
            val halfFloatSize = 2

            val buffer: ByteBuffer = ByteBuffer
                .allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize)
            activity.assets.open("models/dfg.raw").use { it.read(buffer.array()) }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId())
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture")
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RG16F,
                dfgResolution,
                dfgResolution,
                0,
                GLES30.GL_RG,
                GLES30.GL_HALF_FLOAT,
                buffer)
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D")

//            virtualObjectAlbedoTexture = Texture.createFromAsset(render, "models/pawn_albedo.png", Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.SRGB)
//            val virtualObjectPbrTexture = Texture.createFromAsset(
//                render,
//                "models/pawn_roughness_metallic_ao.png",
//                Texture.WrapMode.CLAMP_TO_EDGE,
//                Texture.ColorFormat.LINEAR
//            )
//
//            virtualObjectShader = Shader.createFromAssets(render,
//                "shaders/environmental_hdr.vert",
//                "shaders/environmental_hdr.frag",
//                mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubeMapFilter.numberOfMipmapLevels.toString()))
//                .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
//                .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
//                .setTexture("u_DfgTexture", dfgTexture)
        }
        catch (e:Exception){
            Log.e(TAG, "Failed to read a required asset file")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        hasSetTextureNames = false
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    override fun onSurfaceChanged(render: ARRenderer?, width: Int, height: Int) {
        Log.i("OnSurfaceChanged", "Changed")
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFrameBuffer.resize(width, height)
    }

    override fun onDrawFrame(renderer: ARRenderer?) {
        val session = session ?: return

        //Get the user's Geospatial info
        val earth = session.earth
        if(earth?.trackingState == TrackingState.TRACKING){
            val cameraGeospatialPose = earth.cameraGeospatialPose
            Log.i("Camera Location", "${cameraGeospatialPose.latitude},${cameraGeospatialPose.longitude},${cameraGeospatialPose.altitude}")
        }
        else{
            val earthState = earth!!.earthState
            Log.i("Earth State", earthState.toString())
        }

        if(!hasSetTextureNames){
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.getTextureId()))
            hasSetTextureNames = true
        }

        displayRotationHelper.updateSessionIfNeeded(session)

        val frame =
            try{
                session.update()
            }
            catch (e: CameraNotAvailableException){
                Log.e(TAG, "Camera not available during onDrawFrame", e)
                return
            }

        val camera = frame.camera

        try{
            backgroundRenderer.setUseDepthVisualization(renderer!!,
                activity.depthSettings.depthColorVisualizationEnabled())

            backgroundRenderer.setUseOcclusion(renderer, activity.depthSettings.useDepthForOcclusion())
        }
        catch (e: IOException){
            Log.e(TAG, "Failed to read a required asset file", e)
            return
        }

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)

        val shouldGetDepthImage = activity.depthSettings.useDepthForOcclusion() ||
        activity.depthSettings.depthColorVisualizationEnabled()

        if(camera.trackingState == TrackingState.TRACKING && shouldGetDepthImage){
            try{
                val depthImage = frame.acquireDepthImage16Bits()
                backgroundRenderer.updateCameraDepthTexture(depthImage)
                depthImage.close()
            }
            catch (e: NotYetAvailableException){
                Log.w("Update Camera Depth Texture", e.message.toString())
            }
        }

        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        val message: String? =
            when{
                camera.trackingState == TrackingState.PAUSED
                        && camera.trackingFailureReason == TrackingFailureReason.NONE ->
                    "Searching for Surfaces"
                camera.trackingState == TrackingState.PAUSED ->
                    TrackingStateHelper.getTrackingFailureReasonString(camera)
                session.hasTrackingPlane() && wrappedAnchors.isEmpty() ->
                    "Tap on a surface to place an object"
                session.hasTrackingPlane() && wrappedAnchors.isNotEmpty() -> null
                else -> "Searching for surfaces"
            }

        Log.i(TAG, message!!)


        //Draw background
        if(frame.timestamp != 0L){
            //Suppress rendering if the camera did not produce the first frame yet.
            backgroundRenderer.drawBackground(renderer)
        }

        if(camera.trackingState == TrackingState.PAUSED){
            return
        }

//        camera.getProjectionMatrix(projectionMatrix, 0, Z_Near, Z_Far)
//
//        planeRenderer.drawPlanes(
//            renderer,
//            session.getAllTrackables(Plane::class.java),
//            camera.displayOrientedPose,
//            projectionMatrix
//        )

        //The rest of the code in Hello AR Kotlin is setting up shaders for the GL stuff
        //that it renders. There are good things to potentially crib from in there but we will skip for now.
//        render.clear(virtualSceneFrameBuffer, 0f,0f,0f,0f)
//        backgroundRenderer.drawVirtualScene(renderer, virtualSceneFrameBuffer, Z_Near, Z_Far)

    }

    private fun Session.hasTrackingPlane() =
        getAllTrackables(Plane::class.java).any{it.trackingState == TrackingState.TRACKING}

}

data class WrappedAnchor(
    val anchor: Anchor,
    val trackable: Trackable
)

//fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?){
//    GLES20.glClearColor(1.0f, 1.0f, 0.4f, 0.4f)
//}
//
//fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
//    GLES20.glViewport(0,0,width,height)
//}
//
//override fun onDrawFrame(p0: GL10?) {
//    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//}