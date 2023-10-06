package com.example.cs567_3d_ui_project.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.example.cs567_3d_ui_project.R
import com.example.cs567_3d_ui_project.arcgis_map_operations.GraphicsOverlayOperations
import com.example.cs567_3d_ui_project.databinding.FragmentMapViewBinding
import com.example.cs567_3d_ui_project.qgis_driver.QGisClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapViewFragment: Fragment(R.layout.fragment_map_view) {

    private var binding: FragmentMapViewBinding? = null

    //private lateinit var mapView: MapView
    private lateinit var graphicsOverlayOperations: GraphicsOverlayOperations
    private lateinit var locationCallBack: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val mapView: MapView by lazy {
        binding!!.mapView
    }

    private val locationDisplay: LocationDisplay by lazy { mapView.locationDisplay }

    private val qGisClient: QGisClient by lazy {
        //QGisClient("http://192.168.1.24/cgi-bin/qgis_mapserv.fcgi")
        QGisClient("http://38.147.239.146/cgi-bin/qgis_mapserv.fcgi")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapViewBinding.bind(view)

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            lifecycle.addObserver(mapView)
            setApiKey()
            setupMap()
        }catch (e: Exception){
            Log.e("Error During onViewCreated", e.message, e)
            throw e
        }


    }

    private fun setupMap(){
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the map to be displayed in the layout's MapView
        mapView.map = map
        if (ActivityCompat.checkSelfPermission(
                requireView().context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireView().context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)

        //Set the maps location to the current gps location of the phone.
        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) {
                location: Location? ->
            if (location != null) {
//                val latitude = location.latitude
//                val longitude = location.longitude
//                val altitude = location.altitude
                //Set the map view to the same location of the device with a scale of 500
                mapView.setViewpoint(Viewpoint(location.latitude, location.longitude, 500.0))

                //Query and load the features in the same extent as the device
                loadMap()

                //Listen to single tap events to determine if the user clicked a feature
                listenToOnSingleTapEvents()

                //Listen to location update events
                requestLocationUpdates()

                //Listen to pan events on the map view
                listenToOnUpEvents()

                //Toast.makeText(this, "$latitude, $longitude, $altitude", Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(requireView().context, "Error Retrieving Device Location", Toast.LENGTH_LONG).show()
            }

        }

    }

    private fun setApiKey() {
        // It is not best practice to store API keys in source code. We have you insert one here
        // to streamline project development.
        ArcGISEnvironment.apiKey = ApiKey.create("AAPK5765e56473df40e88e5b67060f23c50dZBL9XDYTJyBFAz9VIhUjp4YzVHVZzFfDC860MQFqpMr9Ji1tJtYZtP-d370P5FLs")
    }

    //Loads the basemap and queries for the features in the extent
    private fun loadMap(){
        lifecycleScope.launch(Dispatchers.IO) {
            //This while loop ensures that the map is actually fully loaded
            //before we try to query features in the extent of where the map loads
            while(mapView.drawStatus.value != DrawStatus.Completed){
                Thread.sleep(1000)
            }

            //Start tracking the location of the device
            locationDisplay.dataSource.start()

            //Load the graphics at the user's start location
            graphicsOverlayOperations = GraphicsOverlayOperations(qGisClient, mapView)

            //Layer is hard coded for now but maybe we should let the user pick the layers they want shown?
            val getFeaturesResponse = graphicsOverlayOperations.queryFeaturesFromLayer("phonelocation_z,test_lines,test_polys")
            graphicsOverlayOperations.drawFeaturesInGraphicsOverlay(getFeaturesResponse)
        }

        listenToOnSingleTapEvents()
    }

    //Draw any spatially collocated features that are near the user's location
    private fun drawGraphicsOnEventRaised(){
        lifecycleScope.launch(Dispatchers.IO) {
            val getFeaturesResponse = graphicsOverlayOperations.queryFeaturesFromLayer("phonelocation_z,test_lines,test_polys")
            graphicsOverlayOperations.drawFeaturesInGraphicsOverlay(getFeaturesResponse)
        }
    }

    private fun listenToOnSingleTapEvents(){
        lifecycleScope.launch(Dispatchers.IO) {
            //Setup a 'FlowCollector' anytime an single tap event occurs on the map
            //this runs asynchronous of the UI thread.
            mapView.onSingleTapConfirmed.collect{ event ->
                event.screenCoordinate.let{ screenCoordinate -> graphicsOverlayOperations.selectGraphics(
                    screenCoordinate
                )}
            }
        }
    }

    //Creates a separate thread to listen to location updates
    private fun requestLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(
                requireView().context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireView().context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)

        //lifecycleScope.launch runs the task asynchronously
        lifecycleScope.launch(Dispatchers.IO) {

            locationCallBack = object: LocationCallback(){
                override fun onLocationResult(locationResult: LocationResult) {

                    //If the map hasn't finished drawing, return
                    if(mapView.drawStatus.value != DrawStatus.Completed){
                        return
                    }

                    //Update the center point of the map based on the user's location
                    for(location in locationResult.locations){
                        //TODO: Make a new fragment to turn off setting the map to the new location
                        val viewPoint = mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                        mapView.setViewpoint(Viewpoint(location.latitude, location.longitude, viewPoint!!.targetScale))
                        drawGraphicsOnEventRaised()
                    }
                    super.onLocationResult(locationResult)
                }
            }
            val locationRequest = LocationRequest.Builder(10000).build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper())
        }
    }

    //Presents the user with the option to allow location tracking
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray)
    {
        when(requestCode){
            1 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if((ContextCompat.checkSelfPermission(requireView().context,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
                        Toast.makeText(requireView().context, "Permission Granted!", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(requireView().context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        //super.
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun listenToOnUpEvents(){
        lifecycleScope.launch(Dispatchers.IO) {
            mapView.onUp.collect{
                drawGraphicsOnEventRaised()
            }
        }
    }


}