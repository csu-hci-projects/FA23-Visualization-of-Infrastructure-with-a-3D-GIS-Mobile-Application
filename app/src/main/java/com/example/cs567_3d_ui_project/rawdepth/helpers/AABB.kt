package com.example.cs567_3d_ui_project.rawdepth.helpers

// An axis-aligned bounding box is defined by the minimum and maximum extends in each dimension.
class AABB {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var maxZ = -Float.MAX_VALUE
    fun update(x: Float, y: Float, z: Float) {
        minX = Math.min(x, minX)
        minY = Math.min(y, minY)
        minZ = Math.min(z, minZ)
        maxX = Math.max(x, maxX)
        maxY = Math.max(y, maxY)
        maxZ = Math.max(z, maxZ)
    }
}