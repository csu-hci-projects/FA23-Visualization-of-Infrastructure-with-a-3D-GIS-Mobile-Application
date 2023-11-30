package com.example.cs567_3d_ui_project.argis.renderers

import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import com.example.cs567_3d_ui_project.argis.Axis
import com.example.cs567_3d_ui_project.argis.GLError
import com.example.cs567_3d_ui_project.argis.Mesh
import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.Framebuffer
import com.example.cs567_3d_ui_project.argis.helpers.AnchorHelper
import com.example.cs567_3d_ui_project.argis.helpers.DisplayRotationHelper
import com.example.cs567_3d_ui_project.argis.helpers.TrackingStateHelper
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.LineGeometry
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.PointGeometry
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.atan
import kotlin.math.pow

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

    lateinit var pipeObjectMesh: Mesh
    lateinit var pipeObjectShader: Shader
    lateinit var pipeObjectTexture: Texture

    lateinit var pipeObjectAlbedoTexture: Texture
    lateinit var pipeObjectRoughnessTexture: Texture

    private val displayRotationHelper: DisplayRotationHelper = DisplayRotationHelper(activity)

    private var hasSetTextureNames = false

    private val Z_Near = 0.1f
    private val Z_Far = 100f

    var earthAnchor: Anchor? = null

    val modelMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val modelViewMatrix = FloatArray(16)
    var rotationMatrix = FloatArray(16)
    var scaleMatrix = FloatArray(16)

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
    val viewInverseMatrix = FloatArray(16)
    val worldLightDirection = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    val viewLightDirection = FloatArray(4) // view x world light direction

    override fun onSurfaceCreated(render: ARRenderer?) {
        try{
            this.render = render!!
            //planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFrameBuffer = Framebuffer(render, 1, 1)

            cubeMapFilter = SpecularCubemapFilter(render,
                CUBEMAP_RESOLUTION,
                CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES)

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


            //"models/PipeAttempt.obj"
            //"models/PipeAttempt2.obj"
            //"models/Cylinder.obj"
            //"models/PipeAttempt_horizontal.obj"
            //"models/Pipe_Cylinder.obj"
            pipeObjectMesh = Mesh.createFromAsset(
                render,
                "models/HorizontalPipeSection_Aligned.obj"
            )
            //"models/WhitePipe.png"
            //"models/Cylinder_Text.png"


//            pipeObjectTexture = Texture.createFromAsset(
//                render,
//                "models/Test.png",
//                Texture.WrapMode.CLAMP_TO_EDGE,
//                Texture.ColorFormat.SRGB
//            )

            pipeObjectAlbedoTexture = Texture.createFromAsset(
                render,
                "models/MetalPipe_Albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            pipeObjectRoughnessTexture = Texture.createFromAsset(
                render,
                "models/MetalPipe_Roughness.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

//            pipeObjectShader = Shader.createFromAssets(
//                render,
//                "shaders/ar_unlit_object.vert",
//                "shaders/ar_unlit_object.frag",
//                null).setTexture("u_Texture", pipeObjectTexture)

            pipeObjectShader = Shader.createFromAssets(
                render,
                "shaders/environmental_hdr.vert",
                "shaders/environmental_hdr.frag",
                mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubeMapFilter.numberOfMipmapLevels.toString()))
                .setTexture("u_AlbedoTexture", pipeObjectAlbedoTexture)
                .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", pipeObjectRoughnessTexture)
                .setTexture("u_Cubemap", cubeMapFilter.filteredCubemapTexture)
                .setTexture("u_DfgTexture", dfgTexture)


            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)
        }
        catch (e:Exception){
            Log.e(TAG, "Failed to read a required asset file: ${e.message}")
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

        // Update lighting parameters in the shader
        updateLightEstimation(frame.lightEstimate, viewMatrix)

        render.clear(virtualSceneFrameBuffer, 0f,0f,0f,0f)

        //Get the user's Geospatial info
        earth = session.earth!!

        if(earth.trackingState == TrackingState.TRACKING){
            val cameraGeospatialPose = earth.cameraGeospatialPose
            Log.i("Camera Location", "${cameraGeospatialPose.latitude},${cameraGeospatialPose.longitude},${cameraGeospatialPose.altitude}")

            updateLocationAccuracy(cameraGeospatialPose)

            //Attempt to place an anchor at the first point feature
            if(activity.latestGetFeatureResponse != null){
                val features = activity.latestGetFeatureResponse!!.getFeatureResponseContent.features

                val lineFeatures = features.filter{ it.geometry.type == "LineString"}
                val pointFeatures = features.filter { it.geometry.type == "Point" }

                //Log.i("Point Feature Count", pointFeatures.size.toString())
                anchorHelper.detachAnchors()

                if(pointFeatures.any()){
                    val pointFeature = pointFeatures.first()
                    anchorHelper.createEarthAnchorFromPointFeature(earth, pointFeature, cameraGeospatialPose)

                    anchorHelper.wrappedAnchors.forEach {
                        it ->
                        if(it.anchor == null) return@forEach
//                        it.let {
//                            render.renderCompassAtAnchor(it.anchor!!, it.selected)
//                        }
                    }
                }

                if(lineFeatures.any()){

                    Log.i("Count", lineFeatures.size.toString())

                    lineFeatures.forEach {
                        lineFeature ->
                        //val lineFeature = lineFeatures.first()
                        val wrappedLineEarthAnchor = anchorHelper.createEarthAnchorsFromLineGeometry(earth, lineFeature, cameraGeospatialPose)

                        val lineGeometry = lineFeature.geometry.toLineGeometry()!!

                        val thetaArray = calculateAngleForLineSegments(lineGeometry)
                        val scaleFactorArray = calculateScaleFactorsForLineSegments(lineGeometry)

                        wrappedLineEarthAnchor.anchors.forEachIndexed{
                            i,a ->
                            if(a == null) return@forEach
                            //TODO: Make use of the angles found in the theta array
                            //Possibly adjust the wrappedLineEarthAnchor to store the theta array
                            val theta = thetaArray[i]
                            val scaleFactor = scaleFactorArray[i]

                            if(activity.arGISSurfaceView.alignAssets){
                                render.renderAssetAtAnchor(a, wrappedLineEarthAnchor.selected, theta, 1.0f)
                            }else{
                                render.renderAssetAtAnchor(a, wrappedLineEarthAnchor.selected, wrappedLineEarthAnchor.angle, 1.0f)
                            }


                        }

                        if(activity.arGISSurfaceView.allModelsRotate){
                            //Set Next Angle for asset to rotate at
                            if(wrappedLineEarthAnchor.angle + 0.01f >= 360.0f){
                                anchorHelper.updateWrappedLineEarthAnchorAngle(wrappedLineEarthAnchor, 0.0f)
                            }
                            else{
                                anchorHelper.updateWrappedLineEarthAnchorAngle(wrappedLineEarthAnchor,
                                    wrappedLineEarthAnchor.angle + 0.01f)
                            }
                        }

//                        anchorHelper.wrappedLineEarthAnchors.forEach {
//                            it.anchors.forEachIndexed{
//                                    i, a ->
//                                if(a == null) return@forEach
//                                //TODO: Make use of the angles found in the theta array
//                                //Possibly adjust the wrappedLineEarthAnchor to store the theta array
//                                val theta = thetaArray[i]
//                                val scaleFactor = scaleFactorArray[i]
//                                render.renderAssetAtAnchor(a, it.selected, theta, 1.75f)
//                                //render.renderAssetAtAnchor(a, it.selected, it.angle, 2.0f)
//                                //render.renderAssetAtAnchor(a, it.selected, it.angle)
//                            }
//
//
//                        }
                    }
                }
            }

            //handleTap(frame, camera, cameraGeospatialPose)

        }
        else{
            val earthState = earth.earthState
            Log.i("Earth State", earthState.toString())
        }

        backgroundRenderer.drawVirtualScene(renderer, virtualSceneFrameBuffer, Z_Near, Z_Far)

    }


    private fun Session.hasTrackingPlane() =
        getAllTrackables(Plane::class.java).any{it.trackingState == TrackingState.TRACKING}

    private fun calculateAngleForLineSegments(lineGeometry: LineGeometry): FloatArray {
        //Find the theta between each line segment.
        val angleArray = ArrayList<Float>()

        //Since the last point geometry has no other direction, use the last
        //angle as its theta.
        var theta: Float

        lineGeometry.lineRoute.forEachIndexed {
                i, pointGeometry ->

            if(i + 1 >= lineGeometry.lineRoute.size){
                //Copy the last computed angle before exiting
                angleArray.add(angleArray[i-1])
                return@forEachIndexed
            }

            val nextIndex = i + 1
            val nextGeometry = lineGeometry.lineRoute[nextIndex]

            theta = calculateAngleForSegment(pointGeometry, nextGeometry)
            angleArray.add(theta)
        }

        return angleArray.toFloatArray()
    }

    private fun calculateScaleFactorsForLineSegments(lineGeometry: LineGeometry): FloatArray{
        val scaleFactorArray = ArrayList<Float>()

        var scaleFactor: Float

        lineGeometry.lineRoute.forEachIndexed{
            i, pointGeometry ->

            if(i + 1 >= lineGeometry.lineRoute.size){
                scaleFactorArray.add(scaleFactorArray[i - 1])
                return@forEachIndexed
            }

            val nextIndex = i + 1
            val nextGeometry = lineGeometry.lineRoute[nextIndex]

            scaleFactor = calculateScaleFactorForSegment(pointGeometry, nextGeometry)
            scaleFactorArray.add(scaleFactor)
        }

        return scaleFactorArray.toFloatArray()
    }

    private fun calculateAngleForSegment(
        pointGeometry1: PointGeometry,
        pointGeometry2: PointGeometry
    ): Float {
        //TODO: If this doesn't work might have to research into how to handle things in Z as well

        //Using Pythagoras Theorem, determine the angle that the line segment should be in.
        //https://www.dummies.com/article/academics-the-arts/science/physics/how-to-find-the-angle-and-magnitude-of-a-vector-173966/
        //theta = tan^-1(y/x)
        //x = x2 - x1
        //y = y2 - y1

        val x = pointGeometry2.x - pointGeometry1.x
        val y = pointGeometry2.y - pointGeometry1.y

        return atan((y / x)).toFloat()
    }


    private fun rotateAsset(modelMatrix: FloatArray, rotationMatrix: FloatArray, theta: Float, axis: Axis = Axis.Y): FloatArray {
        //Rotate the model matrix
        //hardcoded just for testing
        //applied the transformation matrix along Y according to this guide:
        //https://learnopengl.com/Getting-started/Transformations

        when(axis){
            Axis.X -> {
                //Transform with X
                Log.i("Rotate Asset On X", "Theta = $theta")
                rotationMatrix[0] = 1.0f
                rotationMatrix[5] = kotlin.math.cos(theta)
                rotationMatrix[6] = -kotlin.math.sin(theta)
                rotationMatrix[9] = kotlin.math.sin(theta)
                rotationMatrix[10] = kotlin.math.cos(theta)
            }
            Axis.Y -> {
                //Transform with Y
                Log.i("Rotate Asset On Y", "Theta = $theta")
                rotationMatrix[0] = kotlin.math.cos(theta)
                rotationMatrix[2] = kotlin.math.sin(theta)
                rotationMatrix[5] = 1.0f
                rotationMatrix[8] = -kotlin.math.sin(theta)
                rotationMatrix[10] = kotlin.math.cos(theta)
            }
            Axis.Z -> {
                //Transform with Z
                Log.i("Rotate Asset On Z", "Theta = $theta")
                rotationMatrix[0] = kotlin.math.cos(theta)
                rotationMatrix[1] = -kotlin.math.sin(theta)
                rotationMatrix[4] = kotlin.math.sin(theta)
                rotationMatrix[5] = kotlin.math.cos(theta)
                rotationMatrix[10] = 1.0f
            }
        }

        //Leave w as 1
        rotationMatrix[15] = 1f

        val rotatedModelMatrix = FloatArray(16)

        //Multiply model matrix and the rotation matrix before doing any other transforms
        Matrix.multiplyMM(rotatedModelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
        return rotatedModelMatrix
    }

    private fun calculateScaleFactorForSegment(pointGeometry1: PointGeometry,
                                     pointGeometry2: PointGeometry) : Float{

        val x = pointGeometry2.x - pointGeometry1.x
        val y = pointGeometry2.y - pointGeometry1.y

        val xSquared = x.pow(2)
        val ySquared = y.pow(2)

        return kotlin.math.sqrt(xSquared + ySquared).toFloat()

    }

    private fun scaleAsset(modelMatrix: FloatArray, transformationMatrix: FloatArray, scaleFactor: Float, axis: Axis): FloatArray{
        val scaledModelMatrix = FloatArray(16)

        when(axis){
            Axis.X -> {
                transformationMatrix[0] = scaleFactor
                transformationMatrix[5] = 1.0f
                transformationMatrix[10] = 1.0f
            }
            Axis.Y -> {
                transformationMatrix[0] = 1.0f
                transformationMatrix[5] = scaleFactor
                transformationMatrix[10] = 1.0f
            }
            Axis.Z -> {
                transformationMatrix[0] = 1.0f
                transformationMatrix[5] = 1.0f
                transformationMatrix[10] = scaleFactor
            }

        }

        //Always set w to 1.0 for now
        transformationMatrix[15] = 1.0f

        Matrix.multiplyMM(scaledModelMatrix, 0, modelMatrix, 0, transformationMatrix, 0)

        return scaledModelMatrix
    }

//TODO: Create a method just for rendering line strings as these changes will likely break for other feature types
    private fun ARRenderer.renderAssetAtAnchor(anchor: Anchor, selected: Boolean=false, theta: Float = 0.0f, scaleFactor: Float = 1.0f){

        //Get the current pose of the anchor in world space.
        //The Anchor pose is updated during calls to session.update()
        anchor.pose.toMatrix(modelMatrix, 0)

        Log.i("renderAssetAtAnchor", "Anchor Before Rotation")
        prettyPrintMatrix(modelMatrix)


        rotationMatrix = FloatArray(16)
        val rotatedModelMatrix = rotateAsset(modelMatrix, rotationMatrix, theta, activity.arGISSurfaceView.modelRotationAxis)

        Log.i("renderAssetAtAnchor", "Anchor After Rotation")
        prettyPrintMatrix(rotatedModelMatrix)

        //Scale Models (Must be last)
        scaleMatrix = FloatArray(16)
        val scaledRotatedModelMatrix = scaleAsset(rotatedModelMatrix, scaleMatrix, scaleFactor, Axis.Z)

        Log.i("renderAssetAtAnchor", "Anchor After Scaling")
        prettyPrintMatrix(scaledRotatedModelMatrix)

        //Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, scaledRotatedModelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)


        //Update shader properties and draw
        if(selected){
            selectedMapMarkerShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            Log.i("Drawing Selected Feature Texture", "Drew Object Selected")
            draw(mapMarkerObjectMesh, selectedMapMarkerShader, virtualSceneFrameBuffer)
        }else{
            Log.i("Draw", "Draw Normal Object")
            //mapMarkerObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            pipeObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            draw(pipeObjectMesh, pipeObjectShader, virtualSceneFrameBuffer)
            //draw(mapMarkerObjectMesh, mapMarkerObjectShader, virtualSceneFrameBuffer)
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

                //The geospatial plane seems very difficult to get consistent behavior
                //with taps. Need to see what other way there is.
                if(closestEarthAnchor != null && !closestEarthAnchor.selected){
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
        val scaleMatrix = FloatArray(16)
        val modelXScale = FloatArray(16)
        val viewXmodelXscale = FloatArray(16)
        val world2ScreenMatrix = FloatArray(16)

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

    private fun updateSphericalHarmonicsCoefficients(coefficients: FloatArray) {
        // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
        // constants in sphericalHarmonicFactors were derived from three terms:
        //
        // 1. The normalized spherical harmonics basis functions (y_lm)
        //
        // 2. The lambertian diffuse BRDF factor (1/pi)
        //
        // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
        // of all incoming light over a hemisphere for a given surface normal, which is what the shader
        // (environmental_hdr.frag) expects.
        //
        // You can read more details about the math here:
        // https://google.github.io/filament/Filament.html#annex/sphericalharmonics
        require(coefficients.size == 9 * 3) {
            "The given coefficients array must be of length 27 (3 components per 9 coefficients"
        }

        // Apply each factor to every component of each coefficient
        for (i in 0 until 9 * 3) {
            sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3]
        }
        pipeObjectShader.setVec3Array(
            "u_SphericalHarmonicsCoefficients",
            sphericalHarmonicsCoefficients
        )
    }

    private fun updateMainLight(
        direction: FloatArray,
        intensity: FloatArray,
        viewMatrix: FloatArray
    ) {
        // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
        worldLightDirection[0] = direction[0]
        worldLightDirection[1] = direction[1]
        worldLightDirection[2] = direction[2]
        Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0)
        pipeObjectShader.setVec4("u_ViewLightDirection", viewLightDirection)
        pipeObjectShader.setVec3("u_LightIntensity", intensity)
    }

    /** Update state based on the current frame's light estimation. */
    private fun updateLightEstimation(lightEstimate: LightEstimate, viewMatrix: FloatArray) {
        if (lightEstimate.state != LightEstimate.State.VALID) {
            pipeObjectShader.setBool("u_LightEstimateIsValid", false)
            return
        }
        pipeObjectShader.setBool("u_LightEstimateIsValid", true)

        Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0)

        pipeObjectShader.setMat4("u_ViewInverse", viewInverseMatrix)


        updateMainLight(
            lightEstimate.environmentalHdrMainLightDirection,
            lightEstimate.environmentalHdrMainLightIntensity,
            viewMatrix
        )
        updateSphericalHarmonicsCoefficients(lightEstimate.environmentalHdrAmbientSphericalHarmonics)
        cubeMapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap())
    }

    private fun prettyPrintMatrix(matrix: FloatArray){
        //Always assuming it is a 4x4 matrix since we can only work with those in open GL
        if(matrix.size != 16){
            Log.i("prettyPrintMatrix", "Only 4x4 float arrays allowed")
        }

        val stringBuilder = StringBuilder()

        matrix.toList().forEachIndexed {
                index, fl ->

            if((index + 1).mod(4) == 0){
                stringBuilder.appendLine("$fl")
            }
            else{
                stringBuilder.append("$fl ")
            }
        }

       Log.i("Pretty Print Matrix", stringBuilder.toString())
    }

    private fun updateLocationAccuracy(geospatialPose: GeospatialPose){
        val horizontalAccuracy = geospatialPose.horizontalAccuracy
        val verticalAccuracy = geospatialPose.verticalAccuracy
        val orientationYawAccuracy = geospatialPose.orientationYawAccuracy

        val accuracyArray = listOf(horizontalAccuracy, verticalAccuracy, orientationYawAccuracy)

        Log.i("HorizontalAccuracy", horizontalAccuracy.toString())
        Log.i("VerticalAccuracy", verticalAccuracy.toString())
        Log.i("orientationYawAccuracy", orientationYawAccuracy.toString())

        val locationAccuracyUpdate: String =
            if(accuracyArray.any { it >= 50.0 }){
                activity.baseContext.getString(R.string.low_accuracy)
            }
            else if(accuracyArray.any{it > 10.0 && it < 50.0}){
                activity.baseContext.getString(R.string.medium_accuracy)
            }
            else if(accuracyArray.all{it <= 10.0}){
                activity.baseContext.getString(R.string.high_accuracy)
            } else{
                activity.baseContext.getString(R.string.unknown_accuracy)
            }

        activity.arGISSurfaceView.updateLocationAccuracy(locationAccuracyUpdate)

    }

}





