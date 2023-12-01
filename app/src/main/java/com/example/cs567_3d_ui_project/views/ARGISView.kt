package com.example.cs567_3d_ui_project.views

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
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

class ARGISView(val activity: ARGISActivity): DefaultLifecycleObserver {

    val root = View.inflate(activity, R.layout.argis_view, null)
    val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)

    var editingEnabled = false
    var allModelsRotate = false
    var alignAssets = true

    var modelRotationAxis = Axis.Y
    var modelScaleAxis = Axis.Y

    var scaleFactor = 1.0f

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
            xAxis.visibility = View.VISIBLE
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

            scaleAssetsButton.visibility = View.VISIBLE
            eraseTransformationsButton.visibility = View.VISIBLE
            rotateButton.visibility = View.VISIBLE
        }
    }

    private val scaleFactorTextView: TextView = root.findViewById(R.id.scaleFactor)

    private val locationAccuracyTextView: TextView = root.findViewById(R.id.location_accuracy)



    val session
        get() = activity.arGISSessionHelper.mySession

    val tapHelper = TapHelper(activity).also { surfaceView.setOnTouchListener(it) }

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

    fun updateLocationAccuracy(locationAccuracyStatus: String){

        if(locationAccuracyTextView.text == locationAccuracyStatus){
            return
        }
        activity.runOnUiThread {
            locationAccuracyTextView.text = locationAccuracyStatus
        }
    }
}