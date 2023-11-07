package com.example.cs567_3d_ui_project.argis.helpers

import android.content.Context
import android.content.SharedPreferences

class DepthSettings {

    var depthColorVisualizationEnabled: Boolean = false
    var useDepthForOcclusion: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences

    companion object{
        val SHARED_PREFERENCES_ID = "SHARED_PREFERENCES_OCCLUSION_OPTIONS"
        val SHARED_PREFERENCES_SHOW_DEPTH_ENABLE_DIALOG_OOBE = "show_depth_enable_dialog_oobe"
        val SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION = "use_depth_for_occlusion"
    }

    fun onCreate(context: Context){
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE)
        useDepthForOcclusion = sharedPreferences.getBoolean(
            SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION, false)
    }

    fun useDepthForOcclusion(): Boolean{
        return this.useDepthForOcclusion
    }

    fun setUseDepthForOcclusionDS(enable: Boolean){
        if(enable == useDepthForOcclusion){
            return
        }

        useDepthForOcclusion = enable
        val editor = sharedPreferences.edit()
        editor.putBoolean(SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION, useDepthForOcclusion)
        editor.apply()
    }

    fun depthColorVisualizationEnabled(): Boolean{
        return depthColorVisualizationEnabled
    }
}