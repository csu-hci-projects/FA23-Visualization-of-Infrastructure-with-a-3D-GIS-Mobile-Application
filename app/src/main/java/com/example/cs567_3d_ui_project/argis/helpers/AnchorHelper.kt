package com.example.cs567_3d_ui_project.argis.helpers

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.PointGeometry
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose

class AnchorHelper {
    val wrappedAnchors = mutableListOf<WrappedEarthAnchor>()

    val tolerance = 0.0001

    fun detachAnchorsAndClear(){
        wrappedAnchors.forEach {
            a -> a.anchor?.detach()
        }
        wrappedAnchors.clear()
    }

    fun isEmpty(): Boolean{
        return !wrappedAnchors.any()
    }

    fun createEarthAnchor(earth: Earth, pointGeometry: PointGeometry, geospatialPose: GeospatialPose){
        //pointGeometry.z ?: geospatialPose.altitude,
        val earthAnchor = earth.createAnchor(
            pointGeometry.y,
            pointGeometry.x,
            geospatialPose.altitude,
            0f,
            0f,
            0f,
            1f
        )

        val wrappedAnchor = WrappedEarthAnchor(earthAnchor, earth)
        wrappedAnchors.add(wrappedAnchor)
    }

    fun getClosestAnchorToTap(geospatialHitPose: GeospatialPose): Anchor? {
        val geospatialAnchorPoints = wrappedAnchors.map {
            Pair(it.earth.getGeospatialPose(it.anchor!!.pose), it.anchor)
        }

        geospatialAnchorPoints.forEach {
            val distanceLat = kotlin.math.abs(it.first.latitude - geospatialHitPose.latitude)
            val distanceLong = kotlin.math.abs(it.first.longitude - geospatialHitPose.longitude)

            if(distanceLat <= tolerance && distanceLong <= tolerance){
                return it.second
            }

        }
        //We did not find an intersecting anchor
        return null
    }





}

data class WrappedEarthAnchor(
    val anchor: Anchor?,
    val earth: Earth
)

