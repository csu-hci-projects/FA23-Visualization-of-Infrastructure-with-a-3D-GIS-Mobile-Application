package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class Geometry (
    @SerializedName("type")
    var type: String,

    @SerializedName("coordinates")
    var coordinates: JsonArray,

){
    fun toPointGeometry(): PointGeometry?{
        if(type != "Point") {
            return null
        }
        try {
            val typeToken = object : TypeToken<List<Double>>() {}.type
            var pointGeometry = Gson().fromJson<ArrayList<Double>>(coordinates, typeToken)
            return transformCoordinateToPointGeometry(pointGeometry)
        }
        catch (e: Exception){
            Log.e("Error", e.toString())
            throw e
        }
    }
    private fun transformCoordinateToPointGeometry(coordinates: ArrayList<Double>): PointGeometry {
        return PointGeometry(coordinates[0], coordinates[1], coordinates[2])
    }

    fun toLineGeometry(): LineGeometry?{
        if(type != "LineString"){
            return null
        }

        try{
            val typeToken = object : TypeToken<List<ArrayList<Double>>>() {}.type
            var lineGeometryCoordinates = Gson().fromJson<List<ArrayList<Double>>>(coordinates, typeToken)
            var lineGeometry = lineGeometryCoordinates.map { l -> transformCoordinateToPointGeometry(l) }
            Log.i("Test", lineGeometryCoordinates.toString())
            return LineGeometry(lineGeometry)
        }
        catch (e: Exception){
            Log.e("Error", e.message.toString())
            throw e
        }
    }

    fun toPolygonGeometry(): PolygonGeometry?{
        if(type != "Polygon"){
            return null
        }
        try{
            val typeToken = object : TypeToken<ArrayList<ArrayList<ArrayList<Double>>>>() {}.type
            var polygonGeometryCoordinates = Gson().fromJson<ArrayList<ArrayList<ArrayList<Double>>>>(coordinates, typeToken)
            var polygonGeometry = polygonGeometryCoordinates.map { p -> p.map { l -> transformCoordinateToPointGeometry(l) } }
            Log.i("Test", polygonGeometryCoordinates.toString())

            return PolygonGeometry(polygonGeometry)
        }
        catch (e: Exception){
            Log.e("Error", e.message.toString())
            throw e
        }
    }
}

class PointGeometry(
    var x: Double,
    var y: Double,
    var z: Double)


class LineGeometry(
    var lineRoute: List<PointGeometry>
)

class PolygonGeometry(
    var rings: List<List<PointGeometry>>
)




