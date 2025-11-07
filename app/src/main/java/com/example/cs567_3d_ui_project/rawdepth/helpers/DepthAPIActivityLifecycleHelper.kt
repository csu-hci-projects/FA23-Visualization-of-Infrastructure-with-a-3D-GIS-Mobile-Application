package com.example.cs567_3d_ui_project.rawdepth.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session

class DepthAPIActivityLifecycleHelper(val activity: Activity): DefaultLifecycleObserver {

    var installRequested = false

    var beforeSessionResume: ((Session) -> Unit)? = null

    private val CAMERA_PERMISSION_CODE = 0

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA

    var exceptionCallback: ((Exception) -> Unit)? = null

    var mySession: Session? = null
        private set

    override fun onResume(owner: LifecycleOwner) {
        val session = mySession ?: tryCreateSession() ?: return

        try{
            session.configure(
                session.config.apply {
                    geospatialMode = Config.GeospatialMode.ENABLED
                    val isDepthSupported = session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)
                    if(isDepthSupported){
                        depthMode = Config.DepthMode.RAW_DEPTH_ONLY
                        focusMode = Config.FocusMode.AUTO
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

    private fun tryCreateSession(): Session?
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

    override fun onPause(owner: LifecycleOwner) {
        mySession?.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mySession?.close()
        mySession = null
    }
}