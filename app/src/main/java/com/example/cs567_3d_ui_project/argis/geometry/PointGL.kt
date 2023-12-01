package com.example.cs567_3d_ui_project.argis.geometry

class PointGL(val x: Float, val y: Float, val z: Float) {
   fun translateY(distance: Float): PointGL{
       return PointGL(x, y + distance, z)
   }
}