package com.example.cs567_3d_ui_project.argis.geometry.objects

import android.opengl.GLES30
import com.example.cs567_3d_ui_project.argis.geometry.CircleGL
import com.example.cs567_3d_ui_project.argis.geometry.CylinderGL
import com.example.cs567_3d_ui_project.argis.geometry.interfaces.DrawCommand
import kotlin.math.cos
import kotlin.math.sin

class ObjectBuilder(sizeInVertices: Int) {
    private val FLOATS_PER_VERTEX = 3
    private val vertexData: FloatArray = FloatArray(sizeInVertices * FLOATS_PER_VERTEX)
    private var offset = 0
    private val drawList: ArrayList<DrawCommand> = ArrayList()

    companion object{
        fun sizeOfCircleInVertices(numPoints: Int): Int{
            return 1 + (numPoints + 1) * 2
        }

        fun sizeOfOpenCylinderInVertices(numPoints: Int): Int{
            return (numPoints + 1) * 2
        }

        fun createCylinder(cylinderGL: CylinderGL, numPoints: Int): GeneratedData {
            val size = sizeOfCircleInVertices(numPoints) + sizeOfOpenCylinderInVertices(numPoints)

            val builder = ObjectBuilder(size)

            val cylinderTop = CircleGL(cylinderGL.center.translateY(cylinderGL.height / 2f), cylinderGL.radius)

            builder.appendCircle(cylinderTop, numPoints)
            builder.appendOpenCylinder(cylinderGL, numPoints)

            return builder.build()
        }

    }


    fun appendCircle(circle: CircleGL, numPoints: Int){
        val startVertex = offset / FLOATS_PER_VERTEX
        val numVertices = sizeOfCircleInVertices(numPoints)

        //Center point of fan
        vertexData[offset++] = circle.center.x
        vertexData[offset++] = circle.center.y
        vertexData[offset++] = circle.center.z

        for(i in 0..numPoints){
            val angleInRadians = (i.toFloat() / numPoints.toFloat()) + (Math.PI * 2f)

            vertexData[offset++] = circle.center.x + circle.radius * cos(angleInRadians).toFloat()
            vertexData[offset++] = circle.center.y
            vertexData[offset++] = circle.center.z + circle.radius * sin(angleInRadians).toFloat()
        }

        drawList.add(object : DrawCommand {
            override fun draw() {
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, startVertex, numVertices)

            }
        })
    }

    fun appendOpenCylinder(cylinderGL: CylinderGL, numPoints: Int){
        val startVertex = offset / FLOATS_PER_VERTEX
        val numVertices = sizeOfOpenCylinderInVertices(numPoints)

        val yStart = cylinderGL.center.y - (cylinderGL.height / 2f)
        val yEnd = cylinderGL.center.y + (cylinderGL.height / 2f)

        for(i in 0..numPoints){
            val angleInRadians = (i.toFloat() / numPoints.toFloat()) + (Math.PI * 2f)

            val xPosition = (cylinderGL.center.x + cylinderGL.radius * cos(angleInRadians)).toFloat()
            val zPosition = (cylinderGL.center.z + cylinderGL.radius * sin(angleInRadians)).toFloat()

            vertexData[offset++] = xPosition
            vertexData[offset++] = yStart
            vertexData[offset++] = zPosition

            vertexData[offset++] = xPosition
            vertexData[offset++] = yEnd
            vertexData[offset++] = zPosition
        }

        drawList.add(
            object: DrawCommand{
                override fun draw() {
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, startVertex, numVertices)
                }
            }
        )

    }

    private fun build(): GeneratedData{
        return GeneratedData(vertexData, drawList)
    }


}

