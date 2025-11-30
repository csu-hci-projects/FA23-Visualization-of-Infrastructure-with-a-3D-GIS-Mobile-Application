package com.example.cs567_3d_ui_project.argis.mlutils

//Implement this in kotlin
//https://github.com/mohamedsamirx/YOLO_OBB_CPP/blob/main/include/YOLO11-OBB.hpp

data class OBBDetection (
    val box: OrientedBoundingBox,
    val confidence: Float,
    val classId: Int
)
