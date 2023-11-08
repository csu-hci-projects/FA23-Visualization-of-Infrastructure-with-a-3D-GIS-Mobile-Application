package com.example.cs567_3d_ui_project.argis

import android.opengl.GLES30
import android.opengl.GLException
import android.util.Log
import com.example.cs567_3d_ui_project.argis.renderers.ARRenderer
import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8
import java.util.regex.Matcher

class Shader(renderer: ARRenderer,
    vertexShaderCode: String,
    fragmentShaderCode: String,
    defines: Map<String, String>?): Closeable {

    private var programId: Int = 0

    private val uniformLocations: HashMap<String, Int>
        get() = HashMap()

    private val uniformNames: HashMap<Int, String>
        get() = HashMap()

    private val uniforms: HashMap<Int, Uniform>
        get() = HashMap()

    private var maxTextureUnit = 0

    private var depthTest: Boolean = true
    private var depthWrite: Boolean = true

    private var sourceRgbBlend = BlendFactor.ONE
    private var destRgbBlend = BlendFactor.ZERO
    private var sourceAlphaBlend = BlendFactor.ONE
    private var destAlphaBlend = BlendFactor.ZERO

    companion object{
        val TAG: String = Shader::class.java.simpleName

        private fun createShader(type: Int, code: String): Int {
            val shaderId = GLES30.glCreateShader(type)
            GLError.maybeThrowGLException("Shader Creation Failed", "glCreateShader")
            GLES30.glShaderSource(shaderId, code)
            GLError.maybeThrowGLException("SHader source failed", "glShaderSource")
            GLES30.glCompileShader(shaderId)
            GLError.maybeThrowGLException("Shader compilation failed", "glCompileShader")

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

            if(compileStatus[0] == GLES30.GL_FALSE){
                val infoLog = GLES30.glGetShaderInfoLog(shaderId)
                GLError.maybeLogGLError(
                    Log.WARN, TAG, "Failed to retrieve shader info log", "glGetShaderInfoLog"
                )
                GLES30.glDeleteShader(shaderId)
                GLError.maybeLogGLError(Log.WARN, TAG, "Faield to free shader", "glDeleteShader")
                throw GLException(0, "Shader compilation failed: $infoLog")
            }

            return shaderId
        }

        private fun createShaderDefinesCode(defines: Map<String, String>?) : String{
            if(defines == null){
                return ""
            }
            val builder = StringBuilder()
            for(entry: Map.Entry<String, String> in defines.entries.toSet()){
                builder.append("#define " + entry.key + " " + entry.value + "\n")
            }
            return builder.toString()
        }

        fun createFromAssets(renderer: ARRenderer,
                             vertexShaderFileName: String,
                             fragmentShaderFileName: String,
                             defines: Map<String, String>?): Shader{

            val assets = renderer.getAssets()
            return Shader(
                renderer,
                inputStreamToString(assets.open(vertexShaderFileName)),
                inputStreamToString(assets.open(fragmentShaderFileName)),
                defines)
        }

        private fun inputStreamToString(stream: InputStream): String{
            val reader = InputStreamReader(stream, UTF_8.name())
            val buffer = CharArray(1024 * 4)
            val builder = StringBuilder()
            var amount: Int

            while((reader.read(buffer).also { amount = it }) != -1){
                builder.append(buffer, 0, amount)
            }

            reader.close()
            return builder.toString()
        }

        private fun insertShaderDefinesCode(sourceCode: String, definesCode: String): String {
            val result = sourceCode.replace("(?m)^(\\s#\\s*version\\s+.*)", "$1\n" + Matcher.quoteReplacement(definesCode))

            if(result == sourceCode){
                return definesCode + sourceCode
            }
            return result
        }


    }

    init{
        var vertexShaderId = 0
        var fragmentShaderId = 0
        val definesCode = createShaderDefinesCode(defines)
        try{

            vertexShaderId =
                createShader(GLES30.GL_VERTEX_SHADER, insertShaderDefinesCode(vertexShaderCode, definesCode))

            fragmentShaderId =
                createShader(GLES30.GL_FRAGMENT_SHADER, insertShaderDefinesCode(fragmentShaderCode, definesCode))

            programId = GLES30.glCreateProgram()
            GLError.maybeThrowGLException("Shader program creation failed", "glCreateProgram")
            GLES30.glAttachShader(programId, vertexShaderId)
            GLError.maybeThrowGLException("Failed to attach vertex shader", "glAttachShader")
            GLES30.glAttachShader(programId, fragmentShaderId)
            GLError.maybeThrowGLException("Failed to attach fragment shader", "glAttachShader")
            GLES30.glLinkProgram(programId)
            GLError.maybeThrowGLException("Failed to link shader program", "glLinkProgram")

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)

            if(linkStatus[0] == GLES30.GL_FALSE){
                val infoLog = GLES30.glGetShaderInfoLog(programId)
                GLError.maybeLogGLError(
                    Log.WARN, TAG, "Failed to retrieve shader program info log", "glGetProgramInfoLog"
                )
                throw GLException(0, "Shader link failed: $infoLog")
            }

        }
        catch (e : Exception){
            close()
            Log.e("Shader Init failed", e.message.toString())
        }
        finally {
            if(vertexShaderId != 0){
                GLES30.glDeleteShader(vertexShaderId)
                GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free vertex shader", "glDeleteShader")
            }
            if(fragmentShaderId != 0){
                GLES30.glDeleteShader(fragmentShaderId)
                GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free fragment shader", "glDeleteShader")
            }
        }
    }

    override fun close() {
        if(programId != 0){
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
    }

    fun setTexture(name: String, texture: Texture): Shader{
        val location = getUniformLocation(name)
        val uniform = uniforms.get(location)
        var textureUnit: Int
        if(uniform !is UniformTexture){
            textureUnit = maxTextureUnit++
        }
        else{
            val uniformTexture: UniformTexture = uniform as UniformTexture
            textureUnit = uniformTexture.getTextureUnit()
        }
        uniforms[location] = UniformTexture(textureUnit, texture)
        return this
    }

    /** Sets a `vec3` uniform.  */
    fun setVec3(
        name: String?,
        values: FloatArray
    ): Shader? {
        require(values.size == 3) { "Value array length must be 3" }
        uniforms[getUniformLocation(name!!)] = Uniform3f(values.clone())
        return this
    }

    fun setVec4(
        name: String?,
        values: FloatArray
    ): Shader {
        require(values.size == 4) { "Value array length must be 4" }
        uniforms[getUniformLocation(name!!)] =
            Uniform4f(values.clone())
        return this
    }

    /** Sets a `mat2` uniform.  */
    fun setMat2(
        name: String?,
        values: FloatArray
    ): Shader? {
        require(values.size == 4) { "Value array length must be 4 (2x2)" }
        uniforms[getUniformLocation(name!!)] =
            UniformMatrix2f(values.clone())
        return this
    }

    /** Sets a `mat3` uniform.  */
    fun setMat3(
        name: String?,
        values: FloatArray
    ): Shader? {
        require(values.size == 9) { "Value array length must be 9 (3x3)" }
        uniforms[getUniformLocation(name!!)] =
            UniformMatrix3f(values.clone())
        return this
    }

    /** Sets a `mat4` uniform.  */
    fun setMat4(
        name: String?,
        values: FloatArray
    ): Shader? {
        require(values.size == 16) { "Value array length must be 16 (4x4)" }
        uniforms[getUniformLocation(name!!)] =
            UniformMatrix4f(values.clone())
        return this
    }

   private fun getUniformLocation(name: String): Int {
        val locationObject = uniformLocations.get(name)
        if(locationObject != null){
            return locationObject
        }

        val location = GLES30.glGetUniformLocation(programId, name)
        GLError.maybeThrowGLException("Failed to find uniform", "glGetUniformLocation")

        if(location == -1){
            throw IllegalStateException("Shader uniform does not exist: $name")
        }

        uniformLocations[name] = location
        uniformNames[location] = name
        return location
    }

    fun setDepthTest(depthTest: Boolean): Shader{
        this.depthTest = depthTest
        return this
    }

    fun setDepthWrite(depthWrite: Boolean): Shader{
        this.depthWrite = depthWrite
        return this
    }

    fun setBlend(sourceBlend: BlendFactor, destBlend: BlendFactor): Shader{
        this.sourceRgbBlend = sourceBlend
        this.sourceAlphaBlend = sourceBlend
        this.destRgbBlend = destBlend
        this.destAlphaBlend = destBlend
        return this
    }

    fun setBlend(sourceRgbBlend: BlendFactor,
                 destRgbBlend: BlendFactor,
                 sourceAlphaBlend: BlendFactor,
                 destAlphaBlend: BlendFactor): Shader{
        this.sourceRgbBlend = sourceRgbBlend
        this.sourceAlphaBlend = sourceAlphaBlend
        this.destRgbBlend = destRgbBlend
        this.destAlphaBlend = destAlphaBlend
        return this
    }

    fun setFloat(name: String, v0: Float): Shader{
        val values = floatArrayOf(v0)
        uniforms[getUniformLocation(name)] = Uniform1f(values)
        return this
    }

    fun lowLevelUse(){
        if(programId == 0){
            throw IllegalStateException("Attempted to use a freed shader")
        }

        GLES30.glUseProgram(programId)
        GLError.maybeThrowGLException("Failed to use shader program", "glUseProgram")
        GLES30.glBlendFuncSeparate(
            sourceRgbBlend.glesEnum,
            destRgbBlend.glesEnum,
            sourceAlphaBlend.glesEnum,
            destAlphaBlend.glesEnum)
        GLError.maybeThrowGLException("Failed to set blend mode", "glBlendFuncSeparate")
        GLES30.glDepthMask(depthWrite)
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask")

        if(depthTest){
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLError.maybeThrowGLException("Failed to enable depth test", "glEnable")
        }
        else{
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLError.maybeThrowGLException("Failed to disable depth test", "glEnable")
        }

        try{
            //Remove all non-texture uniforms from the map after setting them since they are stored as part of the program
            val obsoleteEntries = ArrayList<Int>(uniforms.size)
            for(entry in uniforms.entries.toSet()){
                try{
                    entry.value.use(entry.key)

                    if(entry.value !is UniformTexture){
                        obsoleteEntries.add(entry.key)
                    }
                } catch(e: GLException){
                    val name = uniformNames[entry.key]
                    throw IllegalStateException("Error setting uniform '$name'", e)
                }
            }
            uniforms.keys.removeAll(obsoleteEntries.toSet())
        }
        finally {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to set active texture", "glActiveTexture")
        }

    }

    private interface Uniform {
        fun use(location: Int)
    }

    class UniformTexture(textureUnit: Int, texture: Texture): Uniform {
        private val textureUnit: Int
        private val texture: Texture

        init{
            this.textureUnit = textureUnit
            this.texture = texture
        }

        fun getTextureUnit(): Int{
            return this.textureUnit
        }

        override fun use(location: Int) {
            if(texture.getTextureId() == 0){
                throw IllegalStateException("Tried to draw with freed texture")
            }

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
            GLError.maybeThrowGLException("Failed to set active texture", "glActiveTexture")
            GLES30.glBindTexture(texture.getTarget().ordinal, texture.getTextureId())
            GLError.maybeThrowGLException("Failed to Bind Texture", "glBindTexture")
            GLES30.glUniform1i(location, textureUnit)
            GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i")
        }

    }

    enum class BlendFactor(glesEnum: Int){
       ZERO(GLES30.GL_ZERO),
        ONE(GLES30.GL_ONE),
        SRC_COLOR(GLES30.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GLES30.GL_ONE_MINUS_SRC_COLOR),
        DST_COLOR(GLES30.GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GLES30.GL_ONE_MINUS_DST_COLOR),
        SRC_ALPHA(GLES30.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GLES30.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GLES30.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GLES30.GL_ONE_MINUS_DST_ALPHA),
        CONSTANT_COLOR(GLES30.GL_CONSTANT_COLOR),
        ONE_MINUS_CONSTANT_COLOR(GLES30.GL_ONE_MINUS_CONSTANT_COLOR),
        CONSTANT_ALPHA(GLES30.GL_CONSTANT_ALPHA),
        ONE_MINUS_CONSTANT_ALPHA(GLES30.GL_ONE_MINUS_CONSTANT_ALPHA);

        val glesEnum: Int

        init{
            this.glesEnum = glesEnum
        }

    }

    class Uniform1f(values: FloatArray): Uniform{
        private val values: FloatArray

        init{
            this.values = values
        }

        override fun use(location: Int) {
            GLES30.glUniform1fv(location, values.size, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 1f", "glUniform1fv")
        }

    }

    private class Uniform2f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform2fv(location, values.size / 2, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 2f", "glUniform2fv")
        }
    }

    private class Uniform3f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform3fv(location, values.size / 3, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 3f", "glUniform3fv")
        }
    }

    class Uniform4f(values: FloatArray): Uniform{

        private val values: FloatArray

        init{
            this.values = values
        }

        override fun use(location: Int) {
            GLES30.glUniform4fv(location, values.size / 4, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 4f", "glUniform4fv")
        }
    }

    private class UniformMatrix2f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniformMatrix2fv(location, values.size / 4,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 2f",
                "glUniformMatrix2fv"
            )
        }
    }

    private class UniformMatrix3f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniformMatrix3fv(location, values.size / 9,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 3f",
                "glUniformMatrix3fv"
            )
        }
    }

    private class UniformMatrix4f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniformMatrix4fv(location, values.size / 16,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 4f",
                "glUniformMatrix4fv"
            )
        }
    }




}