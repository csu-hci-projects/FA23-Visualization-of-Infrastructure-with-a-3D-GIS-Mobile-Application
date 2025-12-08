package com.example.cs567_3d_ui_project.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cs567_3d_ui_project.argis.helpers.ARGISSessionLifecycleHelper
import com.example.cs567_3d_ui_project.argis.helpers.DepthSettings
import com.example.cs567_3d_ui_project.argis.helpers.FullScreenHelper
import com.example.cs567_3d_ui_project.argis.mlutils.DepthAnything
import com.example.cs567_3d_ui_project.argis.mlutils.ObjectDetectionHelper
import com.example.cs567_3d_ui_project.argis.renderers.ARGISRenderer
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.GetFeatureResponse
import com.example.cs567_3d_ui_project.views.ARGISView
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class ARGISActivity: AppCompatActivity() {

    lateinit var arGISSurfaceView: ARGISView
    lateinit var arGISSessionHelper: ARGISSessionLifecycleHelper
    lateinit var argisRenderer: ARGISRenderer

    var latestGetFeatureResponse: GetFeatureResponse? = null
    val depthSettings = DepthSettings()

    lateinit var depthAnythingV2: DepthAnything
    lateinit var objectDetectionHelper: ObjectDetectionHelper

    companion object{
        private const val TAG = "ARGISActivity"
    }

    val selectFileToPlayBack =  registerForActivityResult(getFilePlaybackIntent()) { it ->
        if (it.resultCode != android.app.Activity.RESULT_OK) {
            Log.e(TAG, "onActivityResult select file failed");
        } else {
            val mp4FileUri: Uri? = it.data?.data
            Log.d(TAG, String.format("onActivityResult result is %s", mp4FileUri))

            // Begin playback.
            arGISSurfaceView.startPlayingback(mp4FileUri)
        }
    }

    fun getFilePlaybackIntent(): ActivityResultContracts.StartActivityForResult {
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

        val startActivityForResult = ActivityResultContracts.StartActivityForResult()
        startActivityForResult.createIntent(this, intent)
        return startActivityForResult
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(intent.hasExtra("getFeaturesResponse")){
            latestGetFeatureResponse = GetFeatureResponse(intent.extras!!.getString("getFeaturesResponse")!!, 200)
        }

        arGISSessionHelper = ARGISSessionLifecycleHelper(this)

        arGISSessionHelper.exceptionCallback =
            {
                exception ->
                val message =
                    when(exception){
                        is UnavailableUserDeclinedInstallationException ->
                            "Please install Google Play Services for AR"
                        is UnavailableApkTooOldException -> "Please update ARCore"
                        is UnavailableSdkTooOldException -> "Please update this app"
                        is UnavailableDeviceNotCompatibleException -> "This Device does not support AR"
                        is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                        else -> "Failed to create AR session: $exception"
                    }
                Log.e(TAG, "ARCore threw an exception: $message", exception)
            }

        arGISSessionHelper.beforeSessionResume = ::createSession
        lifecycle.addObserver(arGISSessionHelper)

        argisRenderer = ARGISRenderer(this)
        lifecycle.addObserver(argisRenderer)

        arGISSurfaceView = ARGISView(this)
        lifecycle.addObserver(arGISSurfaceView)
        setContentView(arGISSurfaceView.root)

        ARRenderer(arGISSurfaceView.surfaceView, argisRenderer, assets)

        depthSettings.onCreate(this)

        depthAnythingV2 = DepthAnything(this)
        Log.i(ARGISRenderer.TAG, "Model Load Success!")

        objectDetectionHelper = ObjectDetectionHelper(this)
    }

    private fun createSession(session: Session){
        session.configure(session.config.apply {
            //Set light estimation mode to Environmental HDR
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

            depthMode =
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    fun recreateSession(){
        val session = arGISSessionHelper.recreateSession()
        createSession(session!!)

        lifecycle.removeObserver(argisRenderer)
        argisRenderer = ARGISRenderer(this)
        lifecycle.addObserver(argisRenderer)

        arGISSurfaceView = ARGISView(this)

        setContentView(arGISSurfaceView.root)

        ARRenderer(arGISSurfaceView.surfaceView, argisRenderer, assets)

        depthSettings.onCreate(this)
    }

}