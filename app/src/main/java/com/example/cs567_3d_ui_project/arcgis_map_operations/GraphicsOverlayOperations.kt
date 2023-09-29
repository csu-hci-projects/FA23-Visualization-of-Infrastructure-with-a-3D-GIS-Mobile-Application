package com.example.cs567_3d_ui_project.arcgis_map_operations

import android.util.Log
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.example.cs567_3d_ui_project.qgis_driver.QGisClient
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.Feature
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.GetFeatureResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.wfs_request_actions.GetFeatureRequestAction
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.BoundingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("NAME_SHADOWING")
class GraphicsOverlayOperations(private var qGisClient: QGisClient, private var mapView: MapView) {

    private var graphicsOverlay = GraphicsOverlay()
    init {
        mapView.graphicsOverlays.add(graphicsOverlay)
    }

    suspend fun queryFeaturesFromLayer(layerName: String): GetFeatureResponse {
        return withContext(Dispatchers.IO){
            val viewPoint = mapView.getCurrentViewpoint(ViewpointType.BoundingGeometry)
            val spatialReference = viewPoint?.targetGeometry?.spatialReference
            val extent = viewPoint?.targetGeometry?.extent

            val getFeatureRequestAction = GetFeatureRequestAction(
                layer = layerName,
                boundingBox = BoundingBox("EPSG:${spatialReference?.wkid}",
                    extent!!.xMin, extent.yMin, extent.xMax, extent.yMax),
                srs = "EPSG:${spatialReference?.wkid}"
            )

            return@withContext qGisClient.wfs.getFeature(getFeatureRequestAction)
        }
    }

    fun drawFeaturesInGraphicsOverlay(getFeatureResponse: GetFeatureResponse){
        val features = getFeatureResponse.getFeatureResponseContent.features

        val pointFeatures = features.filter { it.geometry.type == "Point" }
        drawPointFeaturesInGraphicsOverlay(pointFeatures)

        val lineFeatures = features.filter { it.geometry.type == "LineString" }
        drawLineFeaturesInGraphicsOverlay(lineFeatures)

        val polygonFeatures = features.filter { it.geometry.type == "Polygon" }
        drawPolygonFeaturesInGraphicsOverlay(polygonFeatures)
    }

    private fun drawPointFeaturesInGraphicsOverlay(features: List<Feature>){
        //Query the collection of features for the point type features
        val pointFeatures = features.filter { it -> it.geometry.type == "Point" }

        for(pointFeature in pointFeatures){
            val pointGeometry = pointFeature.geometry.toPointGeometry()
            val point = Point(pointGeometry!!.x, pointGeometry.y, SpatialReference.wgs84())
            val symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10.0f)
            val pointGraphic = Graphic(point, symbol)

            pointGraphic.attributes["id"] = pointFeature.id
            graphicsOverlay.graphics.add(pointGraphic)
        }
    }

    private fun drawLineFeaturesInGraphicsOverlay(features: List<Feature>){
        //Query the collection of features for the line type features
        val lineFeatures = features.filter { it.geometry.type == "LineString" }

        for(lineFeature in lineFeatures){
            val lineGeometry = lineFeature.geometry.toLineGeometry()
            val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 3f)

            val lineBuilder = PolylineBuilder(SpatialReference.wgs84()){
                lineGeometry!!.lineRoute.forEach{
                    addPoint(it.x, it.y)
                }
            }

            val lineGraphic = Graphic(lineBuilder.toGeometry(), lineSymbol)
            graphicsOverlay.graphics.add(lineGraphic)
        }
    }

    private fun drawPolygonFeaturesInGraphicsOverlay(features: List<Feature>){
        //Query the collection of features for the polygon type features
        val polygonFeatures = features.filter { it.geometry.type == "Polygon" }

        for(polygonFeature in polygonFeatures){
            val polygonGeometry = polygonFeature.geometry.toPolygonGeometry()
            val polygonSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.green, SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black))

            for(ring in polygonGeometry!!.rings){
                val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()){
                    for(vertex in ring){
                        addPoint(vertex.x, vertex.y)
                    }
                }
                val polygon = polygonBuilder.toGeometry()
                val polygonGraphic = Graphic(polygon, polygonSymbol)
                graphicsOverlay.graphics.add(polygonGraphic)
            }
        }
    }
    //The 'suspend' keyword indicates that the method is async
    suspend fun selectGraphics(screenCoordinate: ScreenCoordinate){
        withContext(Dispatchers.IO) {
            try{
                //Run a spatial query with a given buffer around the click point
                //with a buffer of 25 and an unlimited number of maximum results
                val idOverlay = mapView.identifyGraphicsOverlay(
                    graphicsOverlay = graphicsOverlay,
                    screenCoordinate = screenCoordinate,
                    tolerance = 25.0,
                    returnPopupsOnly = false,
                    maximumResults = -1
                )
                idOverlay.apply {
                    onSuccess {
                        val testGraphics = it.graphics

                        //We are defaulting to a new selection anytime the event fires
                        graphicsOverlay.graphics.forEach{ it ->
                            it.isSelected = false
                        }
                        for(graphic in testGraphics){
                            graphic.isSelected = true
                        }
                    }
                    onFailure {
                        Log.e("Test", it.message, it)
                    }
                }
            }
            catch (e: Exception){
                Log.e("setIdentifyGraphicsOverlay", e.message, e)
                throw e
            }
        }
    }
}