package com.example.cs567_3d_ui_project.argis.buffers

import android.opengl.GLES30
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.io.Closeable
import java.nio.IntBuffer

class IndexBuffer(renderer: ARRenderer, entries: IntBuffer?): Closeable {

    private val buffer: GpuBuffer

    init{
        buffer = GpuBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GpuBuffer.INT_SIZE, entries)
    }


    override fun close() {
        buffer.free()
    }

    fun set(entries: IntBuffer){
        buffer.set(entries)
    }

    fun getBufferId(): Int{
        return buffer.getBufferId()
    }

    fun getSize(): Int{
        return buffer.getSize()
    }


}