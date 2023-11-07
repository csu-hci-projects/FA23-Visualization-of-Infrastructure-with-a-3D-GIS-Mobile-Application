package com.example.cs567_3d_ui_project.argis.buffers

import android.opengl.GLES30
import android.util.Log
import com.example.cs567_3d_ui_project.argis.GLError
import java.nio.Buffer


class GpuBuffer(target: Int, numberOfBytesPerEntry: Int, entries: Buffer? = null) {

    private var entries: Buffer?
    private val target: Int
    private val numberOfBytesPerEntry: Int
    private var size: Int
    private var capacity: Int
    private var bufferId: IntArray = arrayOf(0).toIntArray()

    companion object{
        var INT_SIZE: Int = 4
        var FLOAT_SIZE: Int = 4
    }


    init{
        this.entries = null
        if(entries != null){
            if(!entries.isDirect){
                throw IllegalArgumentException("If Non-null, entries buffer must be a direct buffer")
            }

            if(entries.limit() > 0){
                this.entries = entries
            }
        }

        this.target = target
        this.numberOfBytesPerEntry = numberOfBytesPerEntry

        if(this.entries == null){
            this.size = 0
            this.capacity = 0
        }else{
            this.size = this.entries!!.limit()
            this.capacity = this.entries!!.limit()
        }

        try{
            //Clear VertexArray0 to prevent unintended state change.
            GLES30.glBindVertexArray(0)
            GLError.maybeThrowGLException("Failed to unbind vertex array", "glBindVertexArray")

            GLES30.glGenBuffers(1, bufferId, 0)
            GLError.maybeThrowGLException("Failed to generate buffers", "glGenBuffers")

            GLES30.glBindBuffer(this.target, bufferId[0])
            GLError.maybeThrowGLException("Failed to bind buffer object", "glBindBuffer")

            if(entries != null){
                entries.rewind()
                GLES30.glBufferData(this.target,
                    entries.limit() * numberOfBytesPerEntry, entries, GLES30.GL_DYNAMIC_DRAW)
            }

            GLError.maybeThrowGLException("Failed to Populate buffer object", "glBufferData")
        }
        catch (e: Exception){
            free()
            Log.e("GPU Buffer Init Exception", e.message.toString())
        }

    }

    fun set(entries: Buffer?){
        //Some GPU Drivers will fail with OutOfMemoryExceptions if glBufferData or
        //glBufferSubData is called with a size of 0
        if(entries == null || entries.limit() == 0){
            this.size = 0
            return
        }

        if(!entries.isDirect){
            throw IllegalArgumentException("If non-null, entries buffer must be a direct buffer")
        }

        GLES30.glBindBuffer(target, bufferId[0])
        GLError.maybeThrowGLException("Failed to bind vertex buffer object", "glBindBuffer")

        entries.rewind()

        if(entries.limit() <= capacity){
            GLES30.glBufferSubData(target, 0, entries.limit() * numberOfBytesPerEntry, entries)
            GLError.maybeThrowGLException("Failed to populate vertex buffer object", "glBufferSubData")
            this.size = entries.limit()
        }
        else{
            GLES30.glBufferData(target, entries.limit() * numberOfBytesPerEntry, entries, GLES30.GL_DYNAMIC_DRAW)
            GLError.maybeThrowGLException("Failed to populate vertex buffer object", "glBufferData")
            this.size = entries.limit()
            this.capacity = entries.limit()
        }

    }

    fun free(){
        if(bufferId[0] != 0){
            GLES30.glDeleteBuffers(1, bufferId, 0)

            bufferId[0] = 0
        }
    }

    fun getBufferId(): Int {
        return bufferId[0]
    }

    fun getSize(): Int {
        return this.size
    }
}