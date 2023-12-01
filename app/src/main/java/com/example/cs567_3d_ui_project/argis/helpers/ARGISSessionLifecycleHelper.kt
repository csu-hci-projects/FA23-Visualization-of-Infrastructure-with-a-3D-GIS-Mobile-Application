package com.example.cs567_3d_ui_project.argis.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session

class ARGISSessionLifecycleHelper(val activity: Activity,
    val features: Set<Session.Feature> = setOf()
): DefaultLifecycleObserver {

    private val CAMERA_PERMISSION_CODE = 0

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA

    var installRequested = false

    var beforeSessionResume: ((Session) -> Unit)? = null

    var mySession: Session? = null
        private set

    var exceptionCallback: ((Exception) -> Unit)? = null

    override fun onResume(owner: LifecycleOwner) {
        val session = mySession ?: tryCreateSession() ?: return
        try{
            session.configure(
                session.config.apply {
                    geospatialMode = Config.GeospatialMode.ENABLED
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

    override fun onPause(owner: LifecycleOwner) {
        mySession?.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mySession?.close()
        mySession = null
    }

    private fun tryCreateSession(): Session?
    {
        if(!hasCameraPermissions()){
            requestCameraPermissions()
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

            Session(activity, features)
        }
        catch (e: Exception){
            Log.e("Session Creation Failure", e.message.toString())
            null
        }


    }

    private fun hasCameraPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(activity.applicationContext, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermissions(){
        if(!hasCameraPermissions())
        {
            ActivityCompat.requestPermissions(activity, arrayOf(CAMERA_PERMISSION),
                CAMERA_PERMISSION_CODE)
        }
    }

    fun onRequestPermissionResult(requestCode: Int,
                                  permissions: Array<out String>,
                                  grantResults: IntArray)
    {
        when(requestCode){
            1 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if((ContextCompat.checkSelfPermission(activity.applicationContext, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED))
                    {
                        Toast.makeText(activity, "Permission Granted!", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(activity, "Permission Denied", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return
            }
        }

    }



}