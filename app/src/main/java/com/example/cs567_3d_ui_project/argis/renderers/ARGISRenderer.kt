package com.example.cs567_3d_ui_project.argis.renderers

import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import com.example.cs567_3d_ui_project.argis.GLError
import com.example.cs567_3d_ui_project.argis.Mesh
import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.Framebuffer
import com.example.cs567_3d_ui_project.argis.helpers.AnchorHelper
import com.example.cs567_3d_ui_project.argis.helpers.DisplayRotationHelper
import com.example.cs567_3d_ui_project.argis.helpers.TrackingStateHelper
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
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

    lateinit var mapMarkerObjectMesh: Mesh
    lateinit var mapMarkerObjectShader: Shader
    lateinit var mapMarkerObjectTexture: Texture

    lateinit var selectedMapMarkerObjectMesh: Mesh
    lateinit var selectedMapMarkerShader: Shader
    lateinit var selectedMapMarkerTexture: Texture

    private val displayRotationHelper: DisplayRotationHelper = DisplayRotationHelper(activity)

    private var hasSetTextureNames = false

    private val Z_Near = 0.1f
    private val Z_Far = 100f

    var earthAnchor: Anchor? = null

    val modelMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val modelViewMatrix = FloatArray(16)

    val modelViewProjectionMatrix = FloatArray(16)

    val anchorHelper = AnchorHelper()

    private lateinit var earth: Earth

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


            //models/spatial_marker_baked.png

            mapMarkerObjectTexture = Texture.createFromAsset(
                render,
                "models/BakedBox2.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            //models/geospatial_marker.obj
            //models/Cube.obj
            //"models/Pipe_Blenderkt.obj

            mapMarkerObjectMesh = Mesh.createFromAsset(
                render,
                "models/Cube.obj")


            mapMarkerObjectShader = Shader.createFromAssets(render,
                "shaders/ar_unlit_object.vert",
                "shaders/ar_unlit_object.frag",
                null)
                .setTexture("u_Texture", mapMarkerObjectTexture)

            selectedMapMarkerTexture = Texture.createFromAsset(
                render,
                "models/SelectedCube.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            selectedMapMarkerObjectMesh = Mesh.createFromAsset(
                render,
                "models/selectedcube.obj")

            selectedMapMarkerShader = Shader.createFromAssets(
                render,
                "shaders/ar_unlit_object.vert",
                "shaders/ar_unlit_object.frag",
                null).setTexture("u_Texture", selectedMapMarkerTexture)


            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)
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
                session.hasTrackingPlane() && anchorHelper.isEmpty() ->
                    "No Spatial Features Nearby"
                session.hasTrackingPlane() && !anchorHelper.isEmpty() -> "Tracking and Anchors"
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

        //Get Projection Matrix
        camera.getProjectionMatrix(projectionMatrix, 0, Z_Near, Z_Far)

        //Get Camera Matrix
        camera.getViewMatrix(viewMatrix, 0)


        render.clear(virtualSceneFrameBuffer, 0f,0f,0f,0f)


        //Get the user's Geospatial info
        earth = session.earth!!

        if(earth?.trackingState == TrackingState.TRACKING){
            val cameraGeospatialPose = earth.cameraGeospatialPose
            Log.i("Camera Location", "${cameraGeospatialPose.latitude},${cameraGeospatialPose.longitude},${cameraGeospatialPose.altitude}")

            //Attempt to place an anchor at the first point feature
            if(activity.latestGetFeatureResponse != null){
                val features = activity.latestGetFeatureResponse!!.getFeatureResponseContent.features

                val pointFeatures = features.filter { it.geometry.type == "Point" }

                Log.i("Point Feature Count", pointFeatures.size.toString())

                if(pointFeatures.any()){
                    val pointFeature = pointFeatures.first()

//                    val pointFeatureGeometry = pointFeature.geometry.toPointGeometry()
//                    Log.i("Point Feature Geometry", "${pointFeatureGeometry!!.y},${pointFeatureGeometry.x}")

                    //earthAnchor?.detach()
                    anchorHelper.detachAnchorsAndClear()

                    anchorHelper.createEarthAnchorFromPointFeature(earth, pointFeature, cameraGeospatialPose)

//                    earthAnchor = earth.createAnchor(
//                        pointFeatureGeometry.y,
//                        pointFeatureGeometry.x,
//                        cameraGeospatialPose.altitude,
//                        0f,
//                        0f,
//                        0f,
//                        1f
//                    )



//                    earthAnchor?.let {
//                        render.renderCompassAtAnchor(it)
//                    }

                    anchorHelper.wrappedAnchors.forEach {
                        it ->
                        if(it.anchor == null) return@forEach
                        it.let {
                            render.renderCompassAtAnchor(it.anchor!!, it.selected)
                        }
                    }

                }
            }

            handleTap(frame, camera, cameraGeospatialPose)

        }
        else{
            val earthState = earth!!.earthState
            Log.i("Earth State", earthState.toString())
        }

        backgroundRenderer.drawVirtualScene(renderer, virtualSceneFrameBuffer, Z_Near, Z_Far)

    }

    private fun Session.hasTrackingPlane() =
        getAllTrackables(Plane::class.java).any{it.trackingState == TrackingState.TRACKING}

    private fun ARRenderer.renderCompassAtAnchor(anchor: Anchor, selected: Boolean=false){

        //Get the current pose of the anchor in world space.
        //The Anchor pose is updated during calls to session.update()
        anchor.pose.toMatrix(modelMatrix, 0)

        //Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        //Update shader properties and draw
        mapMarkerObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

        if(selected){
            Log.i("Drawing Selected Feature Texture", "Drew Object Selected")
            draw(mapMarkerObjectMesh, selectedMapMarkerShader, virtualSceneFrameBuffer)
        }else{
            Log.i("Draw", "Draw Normal Object")
            draw(mapMarkerObjectMesh, mapMarkerObjectShader, virtualSceneFrameBuffer)
        }

    }

    private fun handleTap(frame: Frame, camera: Camera, geospatialPose: GeospatialPose){
        if (camera.trackingState != TrackingState.TRACKING) return
        val tap = activity.arGISSurfaceView.tapHelper.poll() ?: return

        try{
            val hitResultList = frame.hitTest(tap)
            Log.i("HitResultList Size", hitResultList.size.toString())

            if(hitResultList.any()) {
                val hitResult = hitResultList.first()

                when(hitResult.trackable!!){
                    is Point -> Log.i("Trackable is Point", "${(hitResult.trackable as Point).pose.tx()}, ${(hitResult.trackable as Point).pose.ty()}, ${(hitResult.trackable as Point).pose.tz()}")
                    is Plane -> Log.i("Trackable is Plane", hitResult.trackable.anchors.size.toString())
                    is DepthPoint -> Log.i("Trackable is Depth Point", hitResult.trackable.anchors.size.toString())
                    else -> Log.i("Something Else", hitResult.trackable.anchors.size.toString())
                }

                val geospatialHitPose = earth.getGeospatialPose(hitResult.hitPose)
                Log.i(
                    "Hit Result",
                    "${geospatialHitPose.latitude}, ${geospatialHitPose.longitude}, ${geospatialHitPose.altitude}"
                )

                val closestEarthAnchor = anchorHelper.getClosestAnchorToTap(geospatialHitPose)

                if(closestEarthAnchor != null){
                    Log.i("Tap Point", "${tap.x}" + ":${tap.y}")
                    val geospatialAnchorPoint = earth.getGeospatialPose(closestEarthAnchor.anchor!!.pose)

                    //Log.i("Geospatial Tap Point", "${geospatialHitPose.latitude}, ${geospatialHitPose.longitude}, ${geospatialHitPose.altitude}")
                    Log.i("Closest Geospatial Anchor Pose", "${geospatialPose.latitude}, ${geospatialPose.longitude}, ${geospatialPose.altitude}")

                    convertAnchorPositionToScreenCoordinates(closestEarthAnchor.anchor!!)
                    Log.i("GFeature", "${geospatialAnchorPoint.latitude},${geospatialAnchorPoint.longitude}, ${geospatialAnchorPoint.altitude}")
//                    Log.i("GFeature", "${geospatialAnchorPoint.latitude},${geospatialAnchorPoint.longitude}, ${geospatialAnchorPoint.altitude}")
//                    val eastUpSouthQuaternion = geospatialPose.eastUpSouthQuaternion.toList().map {
//                        it.toString()
//                    }.reduce { acc, s -> "$acc,$s" }
//                    Log.i("EastUpSouthQuarternion", eastUpSouthQuaternion)
//                    Log.i("CFeature", "${earthAnchor!!.pose.ty() },${earthAnchor!!.pose.tx()}, ${earthAnchor!!.pose.tz()}")
//                    Log.i("CFeature", "${earthAnchor!!.pose.ty() },${earthAnchor!!.pose.tx()}, ${earthAnchor!!.pose.tz()}")
                    anchorHelper.setSelectedEarthAnchors(listOf(closestEarthAnchor))
                }
                else{
                    anchorHelper.setSelectedEarthAnchors()
                }
            }
        }
        catch (e: Exception){
            Log.e("Error an Hit Result Processing", e.message.toString())
        }








    }

    //Implemented this based off this thread
    //https://stackoverflow.com/questions/49026297/convert-3d-world-arcore-anchor-pose-to-its-corresponding-2d-screen-coordinates/49066308#49066308
    fun calculateWorld2CameraMatrix(anchorMatrix: FloatArray): FloatArray{
        val scaleFactor = 1.0f
        var scaleMatrix = FloatArray(16)
        var modelXScale = FloatArray(16)
        var viewXmodelXscale = FloatArray(16)
        var world2ScreenMatrix = FloatArray(16)

        //Set scale factor into diagonal parts of matrix (I think?)
        Matrix.setIdentityM(scaleMatrix, 0)
        scaleMatrix[0] = scaleFactor
        scaleMatrix[5] = scaleFactor
        scaleMatrix[10] = scaleFactor

        Matrix.multiplyMM(modelXScale, 0, anchorMatrix, 0, scaleMatrix, 0)
        Matrix.multiplyMM(viewXmodelXscale, 0, viewMatrix, 0, modelXScale, 0)
        Matrix.multiplyMM(world2ScreenMatrix, 0, projectionMatrix, 0, viewXmodelXscale, 0)

        return world2ScreenMatrix
    }

    fun convertAnchorPositionToScreenCoordinates(anchor: Anchor){
        val bounds = getWindowBounds()

        val width = bounds.first
        val height = bounds.second

        val anchorMatrix = FloatArray(16)
        anchor.pose.toMatrix(anchorMatrix, 0)

        val world2ScreenMatrix = calculateWorld2CameraMatrix(anchorMatrix)
        val anchor2d = world2Screen(width, height, world2ScreenMatrix)

        Log.i("Anchor 2D Screen Coord", "${anchor2d[0]},${anchor2d[1]}")

    }

    private fun world2Screen(screenWidth: Int, screenHeight: Int, world2ScreenMatrix: FloatArray): DoubleArray {
        val origin = floatArrayOf(0f, 0f, 0f, 1f)
        val ndcCoord = FloatArray(4)
        Matrix.multiplyMV(ndcCoord, 0, world2ScreenMatrix, 0, origin, 0)

        ndcCoord[0] = ndcCoord[0]/ndcCoord[3]
        ndcCoord[1] = ndcCoord[1]/ndcCoord[3]

        val pos2D = doubleArrayOf(0.0, 0.0)
        pos2D[0] = screenWidth * ((ndcCoord[0] + 1.0)/2.0)
        pos2D[1] = screenHeight * ((1.0 - ndcCoord[1])/2.0)

        return pos2D
    }

    //Returns widthPixels and heightPixels in a Tuple
    private fun getWindowBounds(): Pair<Int,Int> {
        return if(Build.VERSION.SDK_INT >= 30){
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else{
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            //Suppressed as this is the way to get the display before SDK 30
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

}





