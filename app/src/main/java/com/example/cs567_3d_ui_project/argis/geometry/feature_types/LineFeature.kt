package com.example.cs567_3d_ui_project.argis.geometry.feature_types

import com.example.cs567_3d_ui_project.argis.buffers.VertexBuffer
import com.example.cs567_3d_ui_project.argis.geometry.Constants.Companion.BYTES_PER_FLOAT
import com.example.cs567_3d_ui_project.argis.geometry.CylinderGL
import com.example.cs567_3d_ui_project.argis.geometry.PointGL
import com.example.cs567_3d_ui_project.argis.geometry.interfaces.DrawCommand
import com.example.cs567_3d_ui_project.argis.geometry.objects.ObjectBuilder
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.Feature
import com.otaliastudios.opengl.types.ByteBuffer
import java.nio.ByteOrder

class LineFeature(val renderer: ARRenderer?, val radius: Float, val height: Float, val numberOfPoints: Int, val feature: Feature) {
    private val vertexBuffer: VertexBuffer
    private val drawList: List<DrawCommand>

    init {
        val generatedData = ObjectBuilder.createCylinder(CylinderGL(PointGL(0f, 0f, 0f), radius, height), numberOfPoints)
        val lineGeometry = feature.geometry.toLineGeometry()

        var floatBuffer = ByteBuffer.allocateDirect(lineGeometry!!.lineRoute.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        lineGeometry!!.lineRoute.forEach{
            floatBuffer.put(it.x.toFloat())
            floatBuffer.put(it.y.toFloat())
            floatBuffer.put(it.z!!.toFloat())
        }

        vertexBuffer = VertexBuffer(renderer, 3, floatBuffer)
        drawList = generatedData.drawList
    }

    fun draw(){
        for(drawCommand in drawList){
            drawCommand.draw()
        }
    }
}