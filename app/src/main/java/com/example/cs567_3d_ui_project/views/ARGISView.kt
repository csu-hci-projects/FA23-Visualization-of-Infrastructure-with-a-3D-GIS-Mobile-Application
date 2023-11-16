package com.example.cs567_3d_ui_project.views

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.activities.ARGISActivity

class ARGISView(val activity: ARGISActivity): DefaultLifecycleObserver {

    val root = View.inflate(activity, R.layout.argis_view, null)
    val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)

    val optionsMenuButton = root.findViewById<ImageButton>(R.id.options).apply {
        setOnClickListener {
            v -> PopupMenu(activity, v).apply {
                setOnMenuItemClickListener {
                    item ->
                    when(item.itemId){
                        R.id.edit -> Toast.makeText(activity.applicationContext, "Navigate/Display Edit Buttons Next", Toast.LENGTH_LONG)
                        else -> null
                    } != null
                }
            inflate(R.menu.ar_options_menu)
            show()
            }
        }
    }

    val session
        get() = activity.arGISSessionHelper.mySession

    init {
//        surfaceView.setRenderer(activity.argisRenderer)
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
}