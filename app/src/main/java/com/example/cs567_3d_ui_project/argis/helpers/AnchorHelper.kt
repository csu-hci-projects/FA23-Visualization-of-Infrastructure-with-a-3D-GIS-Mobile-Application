package com.example.cs567_3d_ui_project.argis.helpers

import android.util.Log
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.Feature
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose

class AnchorHelper {
    val wrappedAnchors = mutableListOf<WrappedEarthAnchor>()

    //This is a magic number and i am not happy about its use but it will
    //have to do for now. More precise selection would require more research.
    val tolerance = 0.0001

    fun detachAnchorsAndClear(){
        wrappedAnchors.forEach {
            a ->
            if(a.anchor != null ){ a.anchor?.detach() }
        }
       // wrappedAnchors.clear()
    }

    fun isEmpty(): Boolean{
        return !wrappedAnchors.any()
    }

    fun createEarthAnchorFromPointFeature(earth: Earth, pointFeature: Feature, geospatialPose: GeospatialPose){
        val pointGeometry = pointFeature.geometry.toPointGeometry()

        Log.i("Point Feature Geometry", "${pointGeometry!!.y},${pointGeometry.x}")

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

        if(wrappedAnchors.any{it.featureId == pointFeature.id}){
            val wrappedAnchor = wrappedAnchors.first{it.featureId == pointFeature.id}
            wrappedAnchor.anchor = earthAnchor
            wrappedAnchor.earth = earth
        }else{
            val wrappedAnchor = WrappedEarthAnchor(earthAnchor, earth, pointFeature.id)
            wrappedAnchors.add(wrappedAnchor)
        }


    }

    fun getClosestAnchorToTap(geospatialHitPose: GeospatialPose): WrappedEarthAnchor? {
        val geospatialAnchorPoints = wrappedAnchors.map {
            Pair(it.earth.getGeospatialPose(it.anchor!!.pose), it)
        }

        geospatialAnchorPoints.forEach {
            val distanceLat = kotlin.math.abs(it.first.latitude - geospatialHitPose.latitude)
            val distanceLong = kotlin.math.abs(it.first.longitude - geospatialHitPose.longitude)
            val distanceAlt = kotlin.math.abs(it.first.altitude - geospatialHitPose.altitude)

            Log.i("Hit Result - anchor helper", "${geospatialHitPose.latitude},${geospatialHitPose.longitude},${geospatialHitPose.altitude}")
            Log.i("Wrapped Anchor Loc", "${it.first.latitude},${it.first.longitude},${it.first.altitude}")
            Log.i("Distances", "${distanceLat},${distanceLong},${distanceAlt}")

            if(distanceLat <= tolerance && distanceLong <= tolerance && distanceAlt <= 0.5){
                return it.second
            }

        }
        //We did not find an intersecting anchor
        return null
    }

    fun setSelectedEarthAnchors(selectedWrappedEarthAnchors: List<WrappedEarthAnchor>? = null){
        if(selectedWrappedEarthAnchors == null){
            wrappedAnchors.forEach {
                it.selected = false
            }
        }
        else{
            val selectedEarthAnchorIds = selectedWrappedEarthAnchors.map {
                it.featureId
            }

            wrappedAnchors.filter {
                selectedEarthAnchorIds.contains(it.featureId)
            }.forEach {
                it.selected = true
            }
            //wrappedAnchors.find { selectedEarthAnchorIds.any{a -> it.featureId == a} }
        }

    }





}

data class WrappedEarthAnchor(
    var anchor: Anchor?,
    var earth: Earth,
    var featureId: String,
    var selected: Boolean = false
)

