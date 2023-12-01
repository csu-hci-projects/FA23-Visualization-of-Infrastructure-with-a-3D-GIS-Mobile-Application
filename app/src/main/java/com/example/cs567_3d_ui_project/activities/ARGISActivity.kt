package com.example.cs567_3d_ui_project.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.cs567_3d_ui_project.argis.helpers.ARGISSessionLifecycleHelper
import com.example.cs567_3d_ui_project.argis.helpers.DepthSettings
import com.example.cs567_3d_ui_project.argis.helpers.FullScreenHelper
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

    companion object{
        private const val TAG = "ARGISActivity"
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
                Log.e(TAG, "ARCore threw an exception: ${message!!}", exception)
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


}