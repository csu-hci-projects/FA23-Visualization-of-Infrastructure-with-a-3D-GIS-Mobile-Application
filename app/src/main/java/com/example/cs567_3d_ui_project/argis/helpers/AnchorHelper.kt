package com.example.cs567_3d_ui_project.argis.helpers

import android.util.Log
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.Feature
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.LineGeometry
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose

class AnchorHelper {
    val wrappedAnchors = mutableListOf<WrappedEarthAnchor>()
    val wrappedLineEarthAnchors = mutableListOf<WrappedLineEarthAnchor>()

    //This is a magic number and i am not happy about its use but it will
    //have to do for now. More precise selection would require more research.
    val tolerance = 0.0001

    fun detachAnchors(){
        wrappedAnchors.forEach {
            a ->
            if(a.anchor != null ){ a.anchor?.detach() }
        }

        wrappedLineEarthAnchors.forEach {
            l ->
            l.anchors.forEach { a ->
                if(a != null) { a.detach() }
            }
        }

       // wrappedAnchors.clear()
    }

    fun isEmpty(): Boolean{
        return !wrappedAnchors.any() && !wrappedLineEarthAnchors.any()
    }

    fun createEarthAnchorsFromLineGeometry(earth: Earth, lineFeature: Feature, geospatialPose: GeospatialPose){
        val lineGeometry = lineFeature.geometry.toLineGeometry()

        val lineAnchors = ArrayList<Anchor?>()
        lineGeometry!!.lineRoute.forEach{
            pointGeometry ->
            val earthAnchor = earth.createAnchor(
                pointGeometry.y,
                pointGeometry.x,
                geospatialPose.altitude - 3,
                0f,
                0f,
                0f,
                1f
            )
            lineAnchors.add(earthAnchor)
        }

        if(wrappedLineEarthAnchors.any{it.featureId == lineFeature.id}){
            val wrappedLineEarthAnchor = wrappedLineEarthAnchors.first{it.featureId == lineFeature.id}
            wrappedLineEarthAnchor.anchors = lineAnchors
            wrappedLineEarthAnchor.earth = earth
        }else{
            val wrappedLineEarthAnchor = WrappedLineEarthAnchor(lineAnchors, earth, lineFeature.id)
            wrappedLineEarthAnchors.add(wrappedLineEarthAnchor)
        }





//        val centerPoint = getCenterVertexOfLineGeometry(lineGeometry!!)
//        Log.i("CenterPoint", centerPoint.toString())
//        val directionVectors = calculateDirectionalVectorsFromLineFeature(lineGeometry)
//        Log.i("DirectionVectors Size", directionVectors.size.toString())
//        directionVectors.forEach {
//            Log.i("DirectionVector", it.toString())
//        }
//        Log.i("DirectionVectors", directionVectors.size.toString())



    }

    fun calculateDirectionalVectorsFromLineFeature(lineGeometry: LineGeometry): FloatArray{
        if(lineGeometry.lineRoute.isEmpty()){
            //return
        }
        val vectorArray = ArrayList<Float>(lineGeometry.lineRoute.size * 3)
        lineGeometry.lineRoute.forEachIndexed {
                i, pointGeometry ->

            if(i + 1 >= lineGeometry.lineRoute.size){
                return@forEachIndexed
            }

            val nextIndex = i + 1
            val nextGeometry = lineGeometry.lineRoute[nextIndex]

            val vX = nextGeometry.x - pointGeometry.x
            val vY = nextGeometry.y - pointGeometry.y
            val vZ = nextGeometry.z?.minus(pointGeometry.z!!) ?: 0

            vectorArray.add(vX.toFloat())
            vectorArray.add(vY.toFloat())
            vectorArray.add(vZ.toFloat())
        }

        return vectorArray.toFloatArray()
    }

    fun interpolateLineGeometry(lineGeometry: LineGeometry, stepsPerSegment: Int = 1){


    }

    fun getCenterVertexOfLineGeometry(lineGeometry: LineGeometry): Int{
        //We will round the value we calculated for the index up for now.
        if(lineGeometry.lineRoute.isEmpty()){
            return -1
        }
        return kotlin.math.ceil(lineGeometry.lineRoute.size.toDouble() / 2).toInt() - 1
    }

    fun createEarthAnchorFromPointFeature(earth: Earth, pointFeature: Feature, geospatialPose: GeospatialPose){
        val pointGeometry = pointFeature.geometry.toPointGeometry()

        Log.i("Point Feature Geometry", "${pointGeometry!!.y},${pointGeometry.x}")

        //pointGeometry.z ?: geospatialPose.altitude,
        val earthAnchor = earth.createAnchor(
            pointGeometry.y,
            pointGeometry.x,
            geospatialPose.altitude - 1,
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

    //TODO: Need better collision detection, we should try to account for
    //the size of the rendered feature in our calculation for whether the user
    //intended to select the feature.
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
    var selected: Boolean = false,
    var angle: Float = 0.0f
)

data class WrappedLineEarthAnchor(
    var anchors: ArrayList<Anchor?>,
    var earth: Earth,
    var featureId: String,
    var selected: Boolean = false,
    var angle: Float = 0.0f
)

