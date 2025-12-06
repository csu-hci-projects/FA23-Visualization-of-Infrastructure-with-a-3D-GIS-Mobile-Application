package com.example.cs567_3d_ui_project.argis.mlutils

data class OrientedBoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val angle: Float
)

data class OrientedBoundingBoxNMS(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val classIndex: Int,
    val angle: Float
)
