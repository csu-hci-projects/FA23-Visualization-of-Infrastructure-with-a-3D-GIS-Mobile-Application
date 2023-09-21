package com.example.cs567_3d_ui_project.qgis_driver.resource_objects

//Abstract HTTP Response class used to establish a pattern
@Suppress("unused")
abstract class HttpResponse(val content: String, val responseCode: Int) {

    protected abstract fun deserializeJsonObject(properties: String): HttpResponseContent
}

//Base class that represents the deserialized JsonObject converted to a Plain Old Object class
abstract class HttpResponseContent{}