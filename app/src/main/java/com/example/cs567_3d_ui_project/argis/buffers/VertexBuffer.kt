package com.example.cs567_3d_ui_project.argis.buffers

import android.opengl.GLES30
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.io.Closeable
import java.nio.FloatBuffer

class VertexBuffer(render: ARRenderer, numberOfEntriesPerVertex: Int, entries: FloatBuffer?): Closeable {

    private val numberOfEntriesPerVertex: Int
    private val gpuBuffer: GpuBuffer

    init{
        if(entries != null){
            if(entries.limit() % numberOfEntriesPerVertex != 0){
                throw IllegalArgumentException("If non-null, " +
                        "vertex buffer data must be divisible by the number of data points per vertex")
            }
        }

        this.numberOfEntriesPerVertex = numberOfEntriesPerVertex
        gpuBuffer = GpuBuffer(GLES30.GL_ARRAY_BUFFER, GpuBuffer.FLOAT_SIZE, entries)
    }

    fun set(entries: FloatBuffer?){
        if(entries != null && entries.limit() % numberOfEntriesPerVertex != 0){
            throw IllegalArgumentException("If non-null, " +
                    "vertex buffer data must be divisible by the number of data points per vertex")
        }

        gpuBuffer.set(entries)
    }

    override fun close() {
        gpuBuffer.free()
    }

    fun getBufferId(): Int{
        return gpuBuffer.getBufferId()
    }

    fun getNumberOfEntriesPerVertex(): Int {
        return numberOfEntriesPerVertex
    }

    fun getNumberOfVertices(): Int {
        return (gpuBuffer.getSize() / numberOfEntriesPerVertex)
    }

}