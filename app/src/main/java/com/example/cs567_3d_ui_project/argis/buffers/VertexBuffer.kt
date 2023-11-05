package com.example.cs567_3d_ui_project.argis.buffers

import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.nio.FloatBuffer

class VertexBuffer(render: ARRenderer, numberOfEntriesPerVertex: Int, entries: FloatBuffer) {

    private val numberOfEntriesPerVertex: Int
    private val gpuBuffer: GpuBuffer

    init{
        if(entries != null && entries.limit() % numberOfEntriesPerVertex != 0){
            throw IllegalArgumentException("If non-null, " +
                    "vertex buffer data must be divisible by the number of data points per vertex")
        }

        this.numberOfEntriesPerVertex = numberOfEntriesPerVertex
        gpuBuffer = GpuBuffer()

    }
}