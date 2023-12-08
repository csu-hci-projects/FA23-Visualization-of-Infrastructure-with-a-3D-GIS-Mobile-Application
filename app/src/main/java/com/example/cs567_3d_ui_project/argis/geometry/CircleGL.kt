package com.example.cs567_3d_ui_project.argis.geometry

class CircleGL(val center: PointGL, val radius: Float) {

    fun scale(scale: Float): CircleGL{
        return CircleGL(center, radius + scale)
    }
}