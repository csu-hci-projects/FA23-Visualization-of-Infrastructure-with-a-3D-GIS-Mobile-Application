package com.example.cs567_3d_ui_project.argis.helpers

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.PointGeometry
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Trackable

class AnchorHelper {
    private val wrappedAnchors = mutableListOf<WrappedAnchor>()


    fun detachAnchors(){
        wrappedAnchors.forEach {
            a -> a.anchor.detach()
        }
    }

    fun isEmpty(): Boolean{
        return !wrappedAnchors.any()
    }

    fun createEarthAnchor(earth: Earth, pointGeometry: PointGeometry, geospatialPose: GeospatialPose){
        val earthAnchor = earth.createAnchor(
            pointGeometry.y,
            pointGeometry.x,
            pointGeometry.z ?: geospatialPose.altitude,
            0f,
            0f,
            0f,
            1f
        )
    }





}

data class WrappedAnchor(
    val anchor: Anchor,
    val trackable: Trackable
)
