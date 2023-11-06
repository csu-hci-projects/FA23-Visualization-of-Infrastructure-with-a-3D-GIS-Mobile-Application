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
    defines: Map<String, String>): Closeable {

    private var programId: Int = 0

    companion object{
        val TAG: String = Shader.javaClass.simpleName

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
                throw GLException(0, "Shader compilation failed: " + infoLog)
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

        private fun createFromAssets(renderer: ARRenderer,
                             vertexShaderFileName: String,
                             fragmentShaderFileName: String,
                             defines: Map<String, String>): Shader{

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


}