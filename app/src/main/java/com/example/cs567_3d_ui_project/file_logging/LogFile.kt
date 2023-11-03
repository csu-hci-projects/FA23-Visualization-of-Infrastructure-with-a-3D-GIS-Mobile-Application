package com.example.cs567_3d_ui_project.file_logging

import java.io.File
import java.time.format.DateTimeFormatter

class LogFile {
    private val sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd").toString()
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss").toString()

    fun createLog(error: String, issue: String) {
        File("LOG-$sdf.log").appendText("$time-$issue: $error")
    }

}