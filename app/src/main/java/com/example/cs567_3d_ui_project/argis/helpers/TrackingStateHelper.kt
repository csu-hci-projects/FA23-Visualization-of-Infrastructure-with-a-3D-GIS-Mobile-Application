package com.example.cs567_3d_ui_project.argis.helpers

import android.app.Activity
import android.view.WindowManager
import com.google.ar.core.Camera
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

class TrackingStateHelper(activity: Activity) {


    private val activity: Activity
    private var previousTrackingState: TrackingState = TrackingState.STOPPED



    companion object{
        private const val INSUFFICIENT_FEATURES_MESSAGE =
            "Can't find anything. Aim device at a surface with more texture or color."
        private const val EXCESSIVE_MOTION_MESSAGE = "Moving too fast. Slow down."
        private const val INSUFFICIENT_LIGHT_MESSAGE = "Too dark. Try moving to a well-lit area."
        private const val INSUFFICIENT_LIGHT_ANDROID_S_MESSAGE =
            ("Too dark. Try moving to a well-lit area."
                    + " Also, make sure the Block Camera is set to off in system settings.")
        private const val BAD_STATE_MESSAGE =
            "Tracking lost due to bad internal state. Please try restarting the AR experience."
        private const val CAMERA_UNAVAILABLE_MESSAGE =
            "Another app is using the camera. Tap on this app or try closing the other one."

        private const val ANDROID_S_SDK_VERSION = 31

        fun getTrackingFailureReasonString(camera: Camera): String{
            when(val reason = camera.trackingFailureReason){
                TrackingFailureReason.NONE -> return ""
                TrackingFailureReason.BAD_STATE -> return BAD_STATE_MESSAGE
                TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                    if(android.os.Build.VERSION.SDK_INT < ANDROID_S_SDK_VERSION){
                        return INSUFFICIENT_LIGHT_MESSAGE
                    }
                    return INSUFFICIENT_LIGHT_ANDROID_S_MESSAGE
                }
                TrackingFailureReason.EXCESSIVE_MOTION -> return EXCESSIVE_MOTION_MESSAGE
                TrackingFailureReason.INSUFFICIENT_FEATURES -> return INSUFFICIENT_FEATURES_MESSAGE
                TrackingFailureReason.CAMERA_UNAVAILABLE -> return CAMERA_UNAVAILABLE_MESSAGE
                else -> return "Unknown Tracking Failure Reason: $reason"
            }
        }
    }

    init{
        this.activity = activity
    }

    fun updateKeepScreenOnFlag(trackingState: TrackingState){
        if(trackingState == previousTrackingState){
            return
        }

        previousTrackingState = trackingState
        when(trackingState){
            TrackingState.PAUSED -> {
                activity.runOnUiThread {activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
            }

            TrackingState.STOPPED -> {
                activity.runOnUiThread{activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
            }
            TrackingState.TRACKING -> {
                activity.runOnUiThread{activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
            }
        }
    }



}