package com.example.cs567_3d_ui_project.argis.mlutils

import android.graphics.RectF

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

data class OrientedBoundingBoxAnchorPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

fun OrientedBoundingBoxNMS.xyxyxyxy(rect: RectF): ArrayList<OrientedBoundingBoxAnchorPoint> {
    val xyxyxyxy = ArrayList<OrientedBoundingBoxAnchorPoint>()
    val p0 =  OrientedBoundingBoxAnchorPoint(rect.left, rect.top)
    val p1 = OrientedBoundingBoxAnchorPoint(rect.left, rect.bottom)
    val p2 = OrientedBoundingBoxAnchorPoint(rect.right, rect.top)
    val p3 = OrientedBoundingBoxAnchorPoint(rect.right, rect.bottom)

    xyxyxyxy.add(p0)
    xyxyxyxy.add(p1)
    xyxyxyxy.add(p2)
    xyxyxyxy.add(p3)

    return xyxyxyxy
}
