package com.example.cs567_3d_ui_project.views

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.activities.DepthAPIActivity

class DepthAPIView(val activity: DepthAPIActivity): DefaultLifecycleObserver {

    val root = View.inflate(activity, R.layout.depthapiview, null)
    val surfaceView = root.findViewById<GLSurfaceView>(R.id.depthapisurfaceview)

    val session
        get() = activity.depthApiLifecycleHelper.mySession

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
}