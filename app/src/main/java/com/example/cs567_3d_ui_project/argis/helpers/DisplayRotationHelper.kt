package com.example.cs567_3d_ui_project.argis.helpers

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.view.Display
import android.view.WindowManager
import com.google.ar.core.Session

class DisplayRotationHelper(context: Context): DisplayListener {
    private var viewportChanged: Boolean = false

    var viewPortWidth: Int = 0
    var viewPortHeight: Int = 0

    private val display: Display?

    private val displayManager: DisplayManager
    private val cameraManager: CameraManager

    init{
        displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        display = if(android.os.Build.VERSION.SDK_INT < 30){
            val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } else{
            context.display
        }
    }

    fun onResume(){
        displayManager.registerDisplayListener(this, null)
    }

    fun onPause(){
        displayManager.unregisterDisplayListener(this)
    }

    fun onSurfaceChanged(width: Int, height: Int){
        viewPortWidth = width
        viewPortHeight = height
        viewportChanged = true
    }

    fun updateSessionIfNeeded(session: Session){
        if(viewportChanged){
            val displayRotation = display!!.rotation
            session.setDisplayGeometry(displayRotation, viewPortWidth, viewPortHeight)
            viewportChanged = false
        }
    }


    override fun onDisplayAdded(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onDisplayRemoved(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onDisplayChanged(p0: Int) {
        viewportChanged = true
    }
}