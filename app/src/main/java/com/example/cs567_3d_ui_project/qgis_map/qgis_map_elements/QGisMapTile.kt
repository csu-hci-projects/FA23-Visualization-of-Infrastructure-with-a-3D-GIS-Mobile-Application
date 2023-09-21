package com.example.cs567_3d_ui_project.qgis_map.qgis_map_elements

import android.graphics.Bitmap

data class QGisMapTile(
    var tileMatrix: Int,
    var tileCol: Int,
    var tileRow: Int,
    var tile: Bitmap
)