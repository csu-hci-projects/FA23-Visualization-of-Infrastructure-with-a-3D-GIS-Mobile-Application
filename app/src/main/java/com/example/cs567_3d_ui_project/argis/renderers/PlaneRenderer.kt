package com.example.cs567_3d_ui_project.argis.renderers

import android.opengl.Matrix
import com.example.cs567_3d_ui_project.argis.Mesh
import com.example.cs567_3d_ui_project.argis.Shader
import com.example.cs567_3d_ui_project.argis.Texture
import com.example.cs567_3d_ui_project.argis.buffers.IndexBuffer
import com.example.cs567_3d_ui_project.argis.buffers.VertexBuffer
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class PlaneRenderer(renderer: ARRenderer?) {

    companion object{
        val TAG: String = PlaneRenderer::class.java.simpleName

        private const val VERTEX_SHADER_NAME: String = "shaders/plane.vert"

        private const val FRAGMENT_SHADER_NAME: String = "shaders/plane.frag"

        private const val TEXTURE_NAME = "models/trigrid.png"

        private const val BYTES_PER_FLOAT : Int = Float.SIZE_BYTES

        private const val BYTES_PER_INT: Int = Int.SIZE_BYTES

        private const val COORDS_PER_VERTEX = 3

        private const val VERTS_PER_BOUNDARY_VERT = 2

        private const val INDICES_PER_BOUNDARY_VERT = 3

        private const val INITIAL_BUFFER_BOUNDARY_VERTS = 64

        private val INITIAL_VERTEX_BUFFER_SIZE_BYTES =
            BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS

        private val INITIAL_INDEX_BUFFER_SIZE_BYTES = (BYTES_PER_INT
                * INDICES_PER_BOUNDARY_VERT
                * INDICES_PER_BOUNDARY_VERT
                * INITIAL_BUFFER_BOUNDARY_VERTS)

        private const val FADE_RADIUS_M = 0.25f
        private const val DOTS_PER_METER = 10.0f
        private val EQUILATERAL_TRIANGLE_SCALE = (1 / Math.sqrt(3.0)).toFloat()

        // Using the "signed distance field" approach to render sharp lines and circles.
        // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
        // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
        // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
        // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
        private val GRID_CONTROL = floatArrayOf(0.2f, 0.4f, 2.0f, 1.5f)

        fun calculateDistanceToPlane(planePose: Pose, cameraPose: Pose): Float{
            val normal = FloatArray(3)
            val cameraX = cameraPose.tx()
            val cameraY = cameraPose.ty()
            val cameraZ = cameraPose.tz()

            planePose.getTransformedAxis(1, 1.0f, normal, 0)

            return (cameraX - planePose.tx()) * normal[0] +
                    (cameraY - planePose.ty()) * normal[1] +
                    (cameraZ - planePose.tz()) * normal[2]
        }
    }

    private var indexBufferObject: IndexBuffer? = null
    private var vertexBufferObject: VertexBuffer? = null
    private var shader: Shader? = null
    private var mesh: Mesh? = null

    private var vertexBuffer = ByteBuffer.allocateDirect(INITIAL_VERTEX_BUFFER_SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var indexBuffer = ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asIntBuffer()


    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val planeAngleUvMatrix = FloatArray(4) // 2x2 rotation matrix applied to uv coords.

    private val normalVector = FloatArray(3)

    private val planeIndexMap = mutableMapOf<Plane, Int>()

    init{
        val texture = Texture.createFromAsset(renderer!!, TEXTURE_NAME, Texture.WrapMode.REPEAT, Texture.ColorFormat.LINEAR)

        shader = Shader.createFromAssets(renderer!!, VERTEX_SHADER_NAME, FRAGMENT_SHADER_NAME, null)
            .setTexture("u_Texture", texture)
            .setVec4("u_GridControl", GRID_CONTROL)
            .setBlend(
                Shader.BlendFactor.DST_ALPHA,
                Shader.BlendFactor.ONE,
                Shader.BlendFactor.ZERO,
                Shader.BlendFactor.ONE_MINUS_SRC_ALPHA
            ).setDepthWrite(false)

        indexBufferObject = IndexBuffer(renderer, null)
        vertexBufferObject = VertexBuffer(renderer, COORDS_PER_VERTEX, null)
        val vertexBuffers = arrayOf(vertexBufferObject)
        mesh = Mesh(renderer, Mesh.PrimitiveMode.TRIANGLE_STRIP, indexBufferObject, vertexBuffers)
    }

    fun drawPlanes(renderer: ARRenderer?,
                   allPlanes: Collection<Plane>,
                   cameraPose: Pose,
                   cameraProjection: FloatArray){
        val sortedPlanes = ArrayList<SortablePlane>()

        for(plane in allPlanes){
            if(plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null){
                continue
            }

            val distance = calculateDistanceToPlane(plane.centerPose, cameraPose)
            if(distance < 0){
                continue
            }
            
            sortedPlanes.add(SortablePlane(distance, plane))
        }

        sortedPlanes.sortWith(Comparator { a, b -> b!!.distance.compareTo(a!!.distance) })

        cameraPose.inverse().toMatrix(viewMatrix, 0)

        for(sortedPlane in sortedPlanes){
            val plane = sortedPlane.plane
            val planeMatrix = FloatArray(16)
            plane.centerPose.toMatrix(planeMatrix, 0)

            plane.centerPose.getTransformedAxis(1, 1.0f, normalVector, 0)

            updatePlaneParameters(planeMatrix, plane.extentX, plane.extentZ, plane.polygon)

            var planeIndex = planeIndexMap[plane]

            if(planeIndex == null){
                planeIndex = planeIndexMap.size
                planeIndexMap[plane] = planeIndex
            }

            val angleRadians = planeIndex * 0.144f
            val uScale = DOTS_PER_METER
            val vScale = DOTS_PER_METER * EQUILATERAL_TRIANGLE_SCALE

            planeAngleUvMatrix[0] = +(cos(angleRadians.toDouble()) * uScale).toFloat()
            planeAngleUvMatrix[1] = -(sin(angleRadians.toDouble()) * vScale).toFloat()
            planeAngleUvMatrix[2] = +(sin(angleRadians.toDouble()) * uScale).toFloat()
            planeAngleUvMatrix[3] = +(cos(angleRadians.toDouble()) * vScale).toFloat()

            //Build the ModelView and ModelViewProjection matrices for calculating cube position
            // and light
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)

            shader!!.setMat4("u_Model", modelMatrix)
            shader!!.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader!!.setMat2("u_PlaneUvMatrix", planeAngleUvMatrix)
            shader!!.setVec3("u_Normal", normalVector)

            vertexBufferObject!!.set(vertexBuffer)
            indexBufferObject!!.set(indexBuffer)

            renderer!!.draw(mesh, shader)
        }

    }

    private fun updatePlaneParameters(planeMatrix: FloatArray, extentX: Float, extentZ: Float, boundary: FloatBuffer?){
        System.arraycopy(planeMatrix, 0, modelMatrix, 0, 16)
        if(boundary == null){
            vertexBuffer.limit(0)
            indexBuffer.limit(0)
            return
        }

        // Generate a new set of vertices and a corresponding triangle strip index set so that
        // the plane boundary polygon has a fading edge. This is done by making a copy of the
        // boundary polygon vertices and scaling it down around center to push it inwards. Then
        // the index buffer is setup accordingly.
        boundary.rewind()

        val boundaryVertices = boundary.limit() / 2

        val numVertices: Int = boundaryVertices * VERTS_PER_BOUNDARY_VERT
        val numIndices: Int = boundaryVertices * INDICES_PER_BOUNDARY_VERT


        if(vertexBuffer.capacity() < numVertices * COORDS_PER_VERTEX){
            var size = vertexBuffer.capacity()
            while(size < numVertices * COORDS_PER_VERTEX){
                size *= 2
            }

            vertexBuffer = ByteBuffer.allocateDirect(BYTES_PER_FLOAT * size)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }

        vertexBuffer.rewind()
        vertexBuffer.limit(numVertices * COORDS_PER_VERTEX)

        if(indexBuffer.capacity() < numIndices){
            var size = indexBuffer.capacity()
            while(size < numIndices){
                size *= 2
            }

            indexBuffer = ByteBuffer.allocateDirect(BYTES_PER_INT * size)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
        }

        indexBuffer.rewind()
        indexBuffer.limit(numIndices)

        // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
        // generate a bunch of 0-area triangles.  These don't get rendered though so it works
        // out ok.

        // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
        // generate a bunch of 0-area triangles.  These don't get rendered though so it works
        // out ok.
        val xScale = Math.max((extentX - 2 * FADE_RADIUS_M) / extentX, 0.0f)
        val zScale = Math.max((extentZ - 2 * FADE_RADIUS_M) / extentZ, 0.0f)

        while (boundary.hasRemaining()) {
            val x = boundary.get()
            val z = boundary.get()
            vertexBuffer.put(x)
            vertexBuffer.put(z)
            vertexBuffer.put(0.0f)
            vertexBuffer.put(x * xScale)
            vertexBuffer.put(z * zScale)
            vertexBuffer.put(1.0f)
        }

        // step 1, perimeter

        // step 1, perimeter
        indexBuffer.put(((boundaryVertices - 1) * 2).toShort().toInt())
        for (i in 0 until boundaryVertices) {
            indexBuffer.put((i * 2).toShort().toInt())
            indexBuffer.put((i * 2 + 1).toShort().toInt())
        }
        indexBuffer.put(1.toShort().toInt())
        // This leaves us on the interior edge of the perimeter between the inset vertices
        // for boundary verts n-1 and 0.

        // step 2, interior:
        // This leaves us on the interior edge of the perimeter between the inset vertices
        // for boundary verts n-1 and 0.

        // step 2, interior:
        for (i in 1 until boundaryVertices / 2) {
            indexBuffer.put(((boundaryVertices - 1 - i) * 2 + 1).toShort().toInt())
            indexBuffer.put((i * 2 + 1).toShort().toInt())
        }
        if (boundaryVertices % 2 != 0) {
            indexBuffer.put((boundaryVertices / 2 * 2 + 1).toShort().toInt())
        }

    }


}

class SortablePlane(distance: Float, plane: Plane){
    val distance: Float
    val plane: Plane

    init{
        this.distance = distance
        this.plane = plane
    }
}