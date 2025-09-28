package com.example.cs567_3d_ui_project.activities

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class DepthAPIActivity: AppCompatActivity() {
    var installRequested = false

    var beforeSessionResume: ((Session) -> Unit)? = null

    private val CAMERA_PERMISSION_CODE = 0

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA

    var exceptionCallback: ((Exception) -> Unit)? = null

    var mySession: Session? = null
        private set

    companion object{
        private const val TAG = "DepthAPIActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.exceptionCallback =
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
                Log.e(DepthAPIActivity.TAG, "ARCore threw an exception: ${message!!}", exception)
        }
    }

    fun onResume(owner: LifecycleOwner) {
        val session = mySession ?: tryCreateSession(this) ?: return

        try{
            session.configure(
                session.config.apply {
                    geospatialMode = Config.GeospatialMode.ENABLED
                    val isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                    if(isDepthSupported){
                        depthMode = Config.DepthMode.AUTOMATIC
                    }
                }
            )
            beforeSessionResume?.invoke(session)
            session.resume()
            mySession = session
        }
        catch(e: Exception){
            Log.e("Failure During AR Session ON Resume", e.message.toString())
        }
    }

    private fun tryCreateSession(activity: Activity): Session?
    {
        if(!hasCameraPermissions(activity)){
            requestCameraPermissions(activity)
        }

        return try{
            //Request install of AR Core just in case
            when(ArCoreApk.getInstance().requestInstall(activity, !installRequested)!!){
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return null
                }
                ArCoreApk.InstallStatus.INSTALLED -> {
                }
            }

            Session(activity)
        }
        catch (e: Exception){
            Log.e("Session Creation Failure", e.message.toString())
            null
        }
    }

    private fun hasCameraPermissions(activity: Activity): Boolean {
        return ActivityCompat.checkSelfPermission(activity.applicationContext, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermissions(activity: Activity){
        if(!hasCameraPermissions(activity))
        {
            ActivityCompat.requestPermissions(activity, arrayOf(CAMERA_PERMISSION),
                CAMERA_PERMISSION_CODE)
        }
    }
}