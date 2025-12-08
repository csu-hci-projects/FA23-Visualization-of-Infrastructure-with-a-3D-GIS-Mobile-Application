package com.example.cs567_3d_ui_project.views

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.activities.ARGISActivity
import com.example.cs567_3d_ui_project.argis.Axis
import com.example.cs567_3d_ui_project.argis.helpers.TapHelper
import com.example.cs567_3d_ui_project.argis.mlutils.OBBDetectionNMS
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.PlaybackStatus
import com.google.ar.core.exceptions.CameraNotAvailableException

class ARGISView(val activity: ARGISActivity): DefaultLifecycleObserver {

    val root = View.inflate(activity, R.layout.argis_view, null)
    val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)

    var editingEnabled = false
    var allModelsRotate = false
    var alignAssets = true

    var detectingObjects = false

    var modelRotationAxis = Axis.Y
    var modelScaleAxis = Axis.Y

    var modelZAxis = Axis.Z;

    var scaleFactor = 1.0f

    public final enum class AppState {
        Idle,
        Playingback
    }

    companion object{
        const val TAG = "ARGISView"
    }

    private val scaleFactorTextView: TextView = root.findViewById(R.id.scaleFactor)

//    private val locationAccuracyTextView: TextView = root.findViewById(R.id.location_accuracy)

    val session
        get() = activity.arGISSessionHelper.mySession

    val tapHelper = TapHelper(activity).also { surfaceView.setOnTouchListener(it) }

    var appState = AppState.Idle

    val saveButton:ImageButton = root.findViewById<ImageButton>(R.id.save).apply {
        setOnClickListener{
            v ->
            editingEnabled = false
            editButton.visibility = View.VISIBLE
            cancelButton.visibility = View.INVISIBLE
            undoButton.visibility = View.INVISIBLE
            v.visibility = View.INVISIBLE
        }
    }

    val editButton:ImageButton = root.findViewById<ImageButton>(R.id.edit).apply {
        setOnClickListener {
                v ->
            editingEnabled = true
            v.visibility = View.INVISIBLE
            saveButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            undoButton.visibility = View.VISIBLE
        }
    }

    val cancelButton: ImageButton = root.findViewById<ImageButton>(R.id.cancel).apply {
        setOnClickListener{
            v ->
            editingEnabled = false
            editButton.visibility = View.VISIBLE
            saveButton.visibility = View.INVISIBLE
            undoButton.visibility = View.INVISIBLE
            v.visibility = View.INVISIBLE
        }
    }

    val undoButton: ImageButton = root.findViewById<ImageButton>(R.id.undo).apply {
        setOnClickListener{
            v ->
            editingEnabled = false
            editButton.visibility = View.VISIBLE
            saveButton.visibility = View.INVISIBLE
            cancelButton.visibility = View.INVISIBLE
            v.visibility = View.INVISIBLE
        }
    }

    val rotateButton: ImageButton = root.findViewById<ImageButton>(R.id.rotateAll).apply{
        setOnClickListener{
            v ->
            allModelsRotate = true
            alignAssets = false
            xAxis.visibility = View.VISIBLE
            yAxis.visibility = View.VISIBLE
            zAxis.visibility = View.VISIBLE
            pauseModelRotation.visibility = View.VISIBLE
            stopModelRotation.visibility = View.VISIBLE

            v.visibility = View.GONE
            eraseTransformationsButton.visibility = View.GONE
            scaleAssetsButton.visibility = View.GONE
        }
    }

    val xAxis: ImageButton = root.findViewById<ImageButton>(R.id.rotateModelXAxis).apply {
        setOnClickListener{
            v ->
            if(stopModelRotation.visibility == View.VISIBLE){
                modelRotationAxis = Axis.X
            }
            else if(stopScalingButton.visibility == View.VISIBLE){
                modelScaleAxis = Axis.X
            }
        }
    }

    val yAxis: ImageButton = root.findViewById<ImageButton>(R.id.rotateModelYAxis).apply {
        setOnClickListener{
                v ->
            if(stopModelRotation.visibility == View.VISIBLE){
                modelRotationAxis = Axis.Y
            }
            else if(stopScalingButton.visibility == View.VISIBLE){
                modelScaleAxis = Axis.Y
            }
        }
    }

    val zAxis: ImageButton = root.findViewById<ImageButton>(R.id.rotateModelZAxis).apply {
        setOnClickListener{
                v ->
            if(stopModelRotation.visibility == View.VISIBLE){
                modelRotationAxis = Axis.Z
            }
            else if(stopScalingButton.visibility == View.VISIBLE){
                modelScaleAxis = Axis.Z
            }
        }
    }

    val pauseModelRotation: ImageButton = root.findViewById<ImageButton>(R.id.pauseModelRotation).apply {
        setOnClickListener{
                v ->
            allModelsRotate = !allModelsRotate
        }
    }

    val stopModelRotation: ImageButton = root.findViewById<ImageButton>(R.id.stopModelRotation).apply {
        setOnClickListener{
                v ->
            allModelsRotate = false

            v.visibility = View.GONE
            xAxis.visibility = View.GONE
            yAxis.visibility = View.GONE
            zAxis.visibility = View.GONE
            pauseModelRotation.visibility = View.GONE

            rotateButton.visibility = View.VISIBLE
            eraseTransformationsButton.visibility = View.VISIBLE
            scaleAssetsButton.visibility = View.VISIBLE
        }
    }

    val eraseTransformationsButton: ImageButton = root.findViewById<ImageButton>(R.id.align).apply {
        setOnClickListener {
            alignAssets = true
            modelRotationAxis = Axis.Y
            modelScaleAxis = Axis.Y
            scaleFactor = 1.0f
        }
    }

    val scaleAssetsButton: ImageButton = root.findViewById<ImageButton>(R.id.scaleAll).apply {
        setOnClickListener {
            v ->
            xAxis.visibility = View.GONE
            yAxis.visibility = View.VISIBLE
            zAxis.visibility = View.VISIBLE
            scaleUpButton.visibility = View.VISIBLE
            scaleDownButton.visibility = View.VISIBLE
            stopScalingButton.visibility = View.VISIBLE
            scaleFactorTextView.visibility = View.VISIBLE

            v.visibility = View.GONE
            eraseTransformationsButton.visibility = View.GONE
            rotateButton.visibility = View.GONE
        }
    }

    val detectionButton: ImageButton = root.findViewById<ImageButton>(R.id.objectDetection). apply {
        setOnClickListener{
            detectingObjects = !detectingObjects
        }
    }

    @SuppressLint("SetTextI18n")
    val scaleUpButton: ImageButton = root.findViewById<ImageButton>(R.id.scaleUp).apply {
        setOnClickListener {
            v ->

            if( scaleFactor < 10.0f) {
                scaleFactor += 0.5f
            }
            scaleFactorTextView.text = "ScaleFactor: ${scaleFactor}x"
        }
    }

    @SuppressLint("SetTextI18n")
    val scaleDownButton: ImageButton = root.findViewById<ImageButton>(R.id.scaleDown).apply {
        setOnClickListener {
                v ->

            if(scaleFactor > 0.5f){
                scaleFactor -= 0.5f
            }
            scaleFactorTextView.text = "ScaleFactor: ${scaleFactor}x"
        }
    }

    val stopScalingButton: ImageButton = root.findViewById<ImageButton>(R.id.stopScaling).apply {
        setOnClickListener {
            v ->
            v.visibility = View.GONE
            xAxis.visibility = View.GONE
            yAxis.visibility = View.GONE
            zAxis.visibility = View.GONE
            scaleUpButton.visibility = View.GONE
            scaleDownButton.visibility = View.GONE
            scaleFactorTextView.visibility = View.GONE

            scaleAssetsButton.visibility = View.GONE
            eraseTransformationsButton.visibility = View.GONE
            rotateButton.visibility = View.GONE
        }
    }

    val playbackRecording: ImageButton = root.findViewById<ImageButton>(R.id.playRecording).apply {
        setOnClickListener{
            v ->
            v.visibility = View.GONE
            pausePlayback.visibility = View.VISIBLE
            stopPlayback.visibility = View.VISIBLE

            when(session!!.playbackStatus){
                PlaybackStatus.NONE -> {
                    onClickPlayback()
                }
                PlaybackStatus.OK -> {
                    resumeARCoreSession()
                }

                PlaybackStatus.IO_ERROR -> TODO()
                PlaybackStatus.FINISHED -> TODO()
            }
        }
    }

    val pausePlayback: ImageButton = root.findViewById<ImageButton>(R.id.pause).apply {
        setOnClickListener{
            v ->
            v.visibility = View.GONE
            playbackRecording.visibility = View.VISIBLE
            pauseARCoreSession()
        }
    }

    val stopPlayback: ImageButton = root.findViewById<ImageButton>(R.id.stopPlayback).apply {
        setOnClickListener{
            v->
            v.visibility = View.GONE
            playbackRecording.visibility = View.VISIBLE
            pausePlayback.visibility = View.GONE
            stopPlayback()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        try{
            Log.i("SurfaceView", surfaceView!!.toString())
            surfaceView.onResume()
        }
        catch (e:Exception){
            Log.e("Surface View On Resume Failure", e.message.toString())
            super.onResume(owner)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        try{
            Log.i("SurfaceView", surfaceView!!.toString())
            surfaceView.onPause()
        }
        catch(e:Exception){
            Log.e("Surface View On Pause Failure", e.message.toString())
            super.onPause(owner)
        }
    }

//    fun updateLocationAccuracy(locationAccuracyStatus: String){
//        if(locationAccuracyTextView.text == locationAccuracyStatus){
//            return
//        }
//        activity.runOnUiThread {
//            locationAccuracyTextView.text = locationAccuracyStatus
//        }
//    }

    fun onClickPlayback(){
        Log.d(TAG, "onClickPlayback")

        when(appState){
            AppState.Idle -> {
                pauseARCoreSession()

                val videoCollection: Uri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    MediaStore.Video.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                }
                else{
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val MP4_VIDEO_MIME_TYPE = "video/mp4"
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.setType(MP4_VIDEO_MIME_TYPE)
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, videoCollection)
                intent.addCategory(Intent.CATEGORY_OPENABLE)

                val hasStarted = activity.selectFileToPlayBack.launch(intent)

                Log.d(TAG, String.format("onClickPlayback start: selectFileToPlayback $hasStarted"))
            }

            AppState.Playingback -> {
                val hasStopped = stopPlayback()
                Log.d(TAG, String.format("onClickPlayback stop: hasStopped $hasStopped"))
            }
        }
    }

    fun startPlayingback(mp4FileUri: Uri?): Boolean {
        if(mp4FileUri == null){
            return false
        }

        Log.d(TAG, "startPlayingback at: $mp4FileUri")

        try{
            pauseARCoreSession()
            session!!.setPlaybackDatasetUri(mp4FileUri)
        }
        catch (e: Exception){
            Log.e(TAG, "startPlayingback - setPlaybackDataset failed", e)
        }

        val canResume = resumeARCoreSession()
        if(!canResume)
            return false

        val playbackStatus = session!!.playbackStatus
        Log.d(TAG, String.format("startPlayingback - playbackStatus $playbackStatus"))

        if(playbackStatus != PlaybackStatus.OK){
            return false
        }
        appState = AppState.Playingback

        return true
    }

    fun stopPlayback(): Boolean {
        if(appState != AppState.Playingback)
            return false

        try{
            pauseARCoreSession()
            activity.recreateSession()
            val canResume = resumeARCoreSession()
            if(!canResume)
                return false

            appState = AppState.Idle

            return true

        }
        catch(e: Exception) {
            Log.e(TAG, "Error in return to Idle state. Cannot create new ARCore session", e);
            return false;
        }
    }

    private fun pauseARCoreSession() {
        // Pause the GLSurfaceView so that it doesn't update the ARCore session.
        // Pause the ARCore session so that we can update its configuration.
        // If the GLSurfaceView is not paused,
        //   onDrawFrame() will try to update the ARCore session
        //   while it's paused, resulting in a crash.
        surfaceView.onPause()
        session!!.pause()
    }

    private fun resumeARCoreSession(): Boolean {
        // We must resume the ARCore session before the GLSurfaceView.
        // Otherwise, the GLSurfaceView will try to update the ARCore session.
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "CameraNotAvailableException in resumeARCoreSession", e)
            return false
        }
        surfaceView.onResume()
        return true
    }

    val earthStatusText = root.findViewById<TextView>(R.id.earthStatusText)

    fun updateEarthStatusText(earth: Earth, cameraGeospatialPose: GeospatialPose?) {
        activity.runOnUiThread {
            val poseText = if (cameraGeospatialPose == null) "" else
                activity.getString(
                    R.string.geospatial_pose,
                    cameraGeospatialPose.latitude,
                    cameraGeospatialPose.longitude,
                    cameraGeospatialPose.horizontalAccuracy,
                    cameraGeospatialPose.altitude,
                    cameraGeospatialPose.verticalAccuracy,
                    cameraGeospatialPose.heading,
                    cameraGeospatialPose.headingAccuracy
                )
            earthStatusText.text = activity.resources.getString(
                R.string.earth_state,
                earth.earthState.toString(),
                earth.trackingState.toString(),
                poseText
            )
        }
    }

    val inferenceResults = root.findViewById<TextView>(R.id.inferenceResults)

    fun updateObjectDetectionResults(obbDetectionNMSResults: List<OBBDetectionNMS>){
        var insulators = 0
        var poles = 0
        var wires = 0

        for(obbNMS in obbDetectionNMSResults){
            when(obbNMS.box.classIndex) {
                0 ->
                    insulators+=1
                1 ->
                    poles+=1
                2 ->
                    wires+=1
            }
        }

        activity.runOnUiThread{
            inferenceResults.text = activity.getString(
                R.string.inferenceResults,
                insulators,
                poles,
                wires
            )
        }

    }

}