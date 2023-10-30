package com.example.cs567_3d_ui_project.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cs567_3d_ui_project.helpers.ARGISSessionLifecycleHelper
import com.example.cs567_3d_ui_project.renderers.ARGISRenderer
import com.example.cs567_3d_ui_project.views.ARGISView
import com.google.ar.core.Config
import com.google.ar.core.Session

class ARGISActivity: AppCompatActivity() {

    lateinit var arGISSurfaceView: ARGISView
    lateinit var arGISSessionHelper: ARGISSessionLifecycleHelper
    lateinit var argisRenderer: ARGISRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arGISSessionHelper = ARGISSessionLifecycleHelper(this)
        arGISSessionHelper.beforeSessionResume = ::createSession
        lifecycle.addObserver(arGISSessionHelper)

        argisRenderer = ARGISRenderer(this)
        lifecycle.addObserver(argisRenderer)

        arGISSurfaceView = ARGISView(this)
        lifecycle.addObserver(arGISSurfaceView)
        setContentView(arGISSurfaceView.root)
    }

    private fun createSession(session: Session){
        session.configure(session.config.apply {
            //Set light estimation mode to Environmental HDR
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

        })
    }


}