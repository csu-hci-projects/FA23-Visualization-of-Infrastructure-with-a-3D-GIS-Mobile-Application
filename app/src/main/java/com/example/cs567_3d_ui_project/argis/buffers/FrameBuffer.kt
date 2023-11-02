package com.example.cs567_3d_ui_project.argis.buffers

import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.io.Closeable

class FrameBuffer(render: ARRenderer, width: Int, height: Int): Closeable{

//    private lateinit var colorTexture: Texture

    init{

    }

    override fun close() {
        TODO("Not yet implemented")
    }
}