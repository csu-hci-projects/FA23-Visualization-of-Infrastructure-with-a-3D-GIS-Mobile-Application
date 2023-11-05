package com.example.cs567_3d_ui_project.argis

import android.opengl.GLES30
import android.opengl.GLException
import android.opengl.GLU
import android.util.Log

class GLError {

    companion object{
        fun maybeThrowGLException(reason: String, api: String){
            val errorCodes = getGLErrors()
            if(errorCodes != null){
                throw GLException(errorCodes[0], formatErrorMessage(reason, api, errorCodes))
            }
        }

        fun maybeLogGLError(priority: Int, tag: String, reason: String, api: String){
            val errorCodes = getGLErrors()
            if(errorCodes != null){
                Log.println(priority, tag, formatErrorMessage(reason, api, errorCodes))
            }
        }

        private fun formatErrorMessage(reason: String, api: String, errorCodes: List<Int>): String{
            val stringBuilder = StringBuilder(String.format("%s: %s: ", reason, api))
            val iterator = errorCodes.iterator()
            while(iterator.hasNext()){
                val errorCode = iterator.next()
                stringBuilder.append(String.format("%s (%d)", GLU.gluErrorString(errorCode), errorCode))

                if(iterator.hasNext()){
                    stringBuilder.append(", ")
                }
            }

            return stringBuilder.toString()
        }

        private fun getGLErrors(): List<Int>?{
            //Check the top level code
            var errorCode = GLES30.glGetError()

            //if it does not have an error return
            if(errorCode == GLES30.GL_NO_ERROR){
                return null
            }

            //if it does have an error, cycle through all the
            //errors being reported and add them into the error codes list
            val errorCodes = ArrayList<Int>()

            errorCodes.add(errorCode)
            while(true){
                errorCode = GLES30.glGetError()
                if(errorCode == GLES30.GL_NO_ERROR){
                    break
                }
                errorCodes.add(errorCode)
            }

            return errorCodes
        }
    }
}