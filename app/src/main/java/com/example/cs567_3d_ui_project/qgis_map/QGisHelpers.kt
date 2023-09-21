package com.example.cs567_3d_ui_project.qgis_map

import android.location.Location
import android.util.DisplayMetrics
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.BoundingBox
import kotlin.math.pow
import kotlin.math.sqrt

class QGisHelpers {

    //https://gis.stackexchange.com/questions/60752/calculating-bounding-box-by-given-center-and-scale-in-android
    //https://gis.stackexchange.com/questions/227994/calculate-bounding-box-from-center-at-scale
    //both of these links suggest that the EPSG:4326 coordinate system has this many units per inch.
    //TODO: determine if there is a way to get this constant from other coordinate systems
    private val inchesPerUnit = 4374754

    fun createBoundingBoxFromLocation(location: Location?, displayMetrics: DisplayMetrics, scaleDenominator: Double): BoundingBox {

        val resolution = calculateResolution(scaleDenominator, displayMetrics)
        val halfWidth = (displayMetrics.widthPixels * resolution) / 2
        val halfHeight = (displayMetrics.heightPixels * resolution) / 2

        val minX = location?.latitude?.minus(halfWidth)
        val minY = location?.longitude?.minus(halfHeight)

        val maxX = location?.latitude?.plus(halfWidth)
        val maxY = location?.longitude?.plus(halfHeight)

        //Hard coding to this coordinate reference system until we can freely support others
        return BoundingBox(
            "EPSG:4326",
            minX!!,
            minY!!,
            maxX!!,
            maxY!!
        )
    }
    //https://gis.stackexchange.com/questions/60752/calculating-bounding-box-by-given-center-and-scale-in-android
    //resolution = 1 / ((1 / scaleDenominator) * dpi * inches per unit)
    private fun calculateResolution(scaleDenominator: Double = 1000.0, displayMetrics: DisplayMetrics) : Double {
        //val dpi = displayMetrics.densityDpi
        val dpi = displayMetrics.density
        return (1 / ((1 / scaleDenominator ) * dpi * inchesPerUnit))
    }

    @Suppress("unused")
    private fun calculateDiagonalDimension(displayMetrics: DisplayMetrics): Double {
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        return sqrt(height.toFloat().pow(2) + width.toFloat().pow(2)).toDouble()
    }
}