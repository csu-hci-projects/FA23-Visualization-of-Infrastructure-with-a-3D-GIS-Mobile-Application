package com.example.cs567_3d_ui_project

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
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
import com.example.cs567_3d_ui_project.arcgis_map_operations.GraphicsOverlayOperations
import com.example.cs567_3d_ui_project.databinding.ActivityMainBinding
import com.example.cs567_3d_ui_project.qgis_driver.QGisClient
import com.example.cs567_3d_ui_project.ui.theme.CS567_3D_UI_ProjectTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//@BindingMethods(value = [BindingMethod(type = ImageView::class, attribute = "android:baseMap", method = "getBaseMap")])
class MainActivity : AppCompatActivity() {


    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    private lateinit var graphicsOverlayOperations: GraphicsOverlayOperations
    private lateinit var locationCallBack: LocationCallback

    //private val zoomImageView:ZoomImageView by lazy {
    //    activityMainBinding.zoomImageView
    //}

    /*private val qGisMapView: QGisMapViewContainer by lazy {
        activityMainBinding.qgisMapView
    }*/

    private val qGisClient: QGisClient by lazy {
        //QGisClient("http://192.168.1.24/cgi-bin/qgis_mapserv.fcgi")
        QGisClient("http://38.147.239.146/cgi-bin/qgis_mapserv.fcgi")
    }

    private val locationDisplay: LocationDisplay by lazy { mapView.locationDisplay }

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private fun setupMap() {

        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the map to be displayed in the layout's MapView
        mapView.map = map
        var latitude = 0.0
        var longitude = 0.0
        var altitude = 0.0



        /*qGisClient.also { this.qGisMapView.setQgisClient(it) }
        qGisMapView.setFusedLocationClient(fusedLocationClient)
        qGisMapView.setDisplayMetrics(resources.displayMetrics)*/

        //val height = resources.displayMetrics.heightPixels
        //val width = resources.displayMetrics.widthPixels
        //val diagonal = sqrt(height.toFloat().pow(2) + width.toFloat().pow(2))

        //val xdpi = resources.displayMetrics.xdpi
        //val ydpi = resources.displayMetrics.ydpi
        //val dpi = resources.displayMetrics.densityDpi
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1)


        //Set the maps location to the current gps location of the phone.
        fusedLocationClient.lastLocation.addOnSuccessListener(this) {
                location: Location? ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                altitude = location.altitude
                //Toast.makeText(this, "Received Location", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "$latitude, $longitude, $altitude", Toast.LENGTH_LONG).show()
                mapView.setViewpoint(Viewpoint(location.latitude, location.longitude, 500.0))

                loadMap()
                requestLocationUpdates()
            }
            else{
                Toast.makeText(this, "Error Retrieving Device Location", Toast.LENGTH_LONG).show()
            }

        }



        Log.i("TestLoc", latitude.toString())
        Log.i("TestLoc", longitude.toString())
    }

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
            var getFeaturesResponse = graphicsOverlayOperations.queryFeaturesFromLayer("phonelocation_z,test_lines,test_polys")
            Log.i("Test", getFeaturesResponse.toString())
            graphicsOverlayOperations.drawFeaturesInGraphicsOverlay(getFeaturesResponse)

//                    //Setup a 'FlowCollector' anytime an single tap event occurs on the map
//                    //this runs asynchronous of the UI thread.
            mapView.onSingleTapConfirmed.collect{ event ->
                event.screenCoordinate.let{ screenCoordinate -> graphicsOverlayOperations.selectGraphics(
                    screenCoordinate
                )}
            }
        }
    }

    private fun requestLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1)

        lifecycleScope.launch(Dispatchers.IO) {
            locationCallBack = object: LocationCallback(){
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult ?: return

                    for(location in locationResult.locations){
                        var viewPoint = mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                        mapView.setViewpoint(Viewpoint(location.latitude, location.longitude, viewPoint!!.targetScale))
                    }

                    super.onLocationResult(locationResult)
                }
            }
            var locationRequest = LocationRequest.Builder(10000).build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray)
    {
        when(requestCode){
            1 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if((ContextCompat.checkSelfPermission(this@MainActivity, ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED)){
                        Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try{
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            lifecycle.addObserver(mapView)

            setApiKey()

            setupMap()

//            lifecycleScope.launch {
//                Log.i("Test", "Test")
//            }

            //val imageView = ImageView(this)

            //lifecycleScope.launch{
                //val getMapResponse = getBaseMap()
                //zoomImageView.setImageBitmap(getBaseMap())
                //imageView.setImageBitmap(getMapResponse.bitmap.asAndroidBitmap())
            //    setContentView(imageView)
            //}

        }
        catch(e: Exception){
            Log.e("Error During onCreate", e.message, e)
            //val alertDialogBuilder = AlertDialog.Builder(this)
            //    .setTitle(e.message)
            //    .setNegativeButton("Cancel", null)

            //alertDialogBuilder.show()
            showError(e.message.toString())


            throw e
        }

        /*
        setContent {
            CS567_3D_UI_ProjectTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
         */
    }

    private fun setApiKey() {
        // It is not best practice to store API keys in source code. We have you insert one here
        // to streamline this tutorial.
        ArcGISEnvironment.apiKey = ApiKey.create("AAPK5765e56473df40e88e5b67060f23c50dZBL9XDYTJyBFAz9VIhUjp4YzVHVZzFfDC860MQFqpMr9Ji1tJtYZtP-d370P5FLs")

    }
    //The 'suspend' keyword indicates that the method is async
    //lifecycleScope.launch runs the task asynchronously
    //lifecycleScope.launch

    //TODO: Very hardcoded method in nature, need to determine how we can signal
    //which layer we want to target.

    //GetMapResponse

    /*private suspend fun getBaseMap(): Bitmap {
        return withContext(Dispatchers.IO){
            try {
                val getSchemaExtensionResponse = qGisClient.wms.getSchemaExtensionAsync()
                val schema = getSchemaExtensionResponse.getSchemaExtensionResponseContent.schema

                Log.i("Test", schema.toString())

                val getCapabilitiesResponse = qGisClient.wms.getCapabilities()
                val wmsCapabilities =
                    getCapabilitiesResponse.getCapabilitiesResponseContent.wmsCapabilities

                //val countriesLayer = wmsCapabilities.capability.layer.layer.first {it.name.equals("countries", true)}
                val esriBasemapLayer = wmsCapabilities.capability.layer.layer.first{it.name.equals("ESRI Topo", true)}

                Log.i("Test2", getCapabilitiesResponse.responseCode.toString())
                //Log.i("Test2", firstSpatialReference.toString())
                Log.i("Test2", wmsCapabilities.toString())

                //val getMapResponse: GetMapResponse = qGisClient.wms.getMap(countriesLayer)
                val getMapResponse: GetMapResponse = qGisClient.wms.getMap(esriBasemapLayer, req=null)
                //val test = getMapResponse.getMapResponseContent
                Log.i("Test3", getMapResponse.toString())
                //var xmlToJson = XmlToJson.Builder(test).build()

                val getWfsCapabilitiesResponse = qGisClient.wfs.getCapabilities()
                Log.i("Test4", getWfsCapabilitiesResponse.getCapabilitiesResponse.toString())

                val airportsLayer = wmsCapabilities.capability.layer.layer.first{it.name.equals("airports", true)}

                val getFeatureResponse = qGisClient.wfs.getFeature(airportsLayer.name)

                Log.i("Test5", getFeatureResponse.getFeatureResponseContent.toString())

                return@withContext getMapResponse.bitmap.asAndroidBitmap()
                //return@withContext getMapResponse
                //var jsonObject = xmlToJson.toJson()
                //Log.i("Output", test)
            } catch (e: Exception) {
                Log.e("Error During Coroutine", e.message, e)
                throw e
            }
        }
    }*//*private suspend fun getBaseMap(): Bitmap {
        return withContext(Dispatchers.IO){
            try {
                val getSchemaExtensionResponse = qGisClient.wms.getSchemaExtensionAsync()
                val schema = getSchemaExtensionResponse.getSchemaExtensionResponseContent.schema

                Log.i("Test", schema.toString())

                val getCapabilitiesResponse = qGisClient.wms.getCapabilities()
                val wmsCapabilities =
                    getCapabilitiesResponse.getCapabilitiesResponseContent.wmsCapabilities

                //val countriesLayer = wmsCapabilities.capability.layer.layer.first {it.name.equals("countries", true)}
                val esriBasemapLayer = wmsCapabilities.capability.layer.layer.first{it.name.equals("ESRI Topo", true)}

                Log.i("Test2", getCapabilitiesResponse.responseCode.toString())
                //Log.i("Test2", firstSpatialReference.toString())
                Log.i("Test2", wmsCapabilities.toString())

                //val getMapResponse: GetMapResponse = qGisClient.wms.getMap(countriesLayer)
                val getMapResponse: GetMapResponse = qGisClient.wms.getMap(esriBasemapLayer, req=null)
                //val test = getMapResponse.getMapResponseContent
                Log.i("Test3", getMapResponse.toString())
                //var xmlToJson = XmlToJson.Builder(test).build()

                val getWfsCapabilitiesResponse = qGisClient.wfs.getCapabilities()
                Log.i("Test4", getWfsCapabilitiesResponse.getCapabilitiesResponse.toString())

                val airportsLayer = wmsCapabilities.capability.layer.layer.first{it.name.equals("airports", true)}

                val getFeatureResponse = qGisClient.wfs.getFeature(airportsLayer.name)

                Log.i("Test5", getFeatureResponse.getFeatureResponseContent.toString())

                return@withContext getMapResponse.bitmap.asAndroidBitmap()
                //return@withContext getMapResponse
                //var jsonObject = xmlToJson.toJson()
                //Log.i("Output", test)
            } catch (e: Exception) {
                Log.e("Error During Coroutine", e.message, e)
                throw e
            }
        }
    }*/
    /*private fun getLocation(callback: Callback){
        fusedLocationClient!!.lastLocation.addOnSuccessListener(this) {

        }
    }*/

    private fun showError(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        Log.e(localClassName, message)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CS567_3D_UI_ProjectTheme {
        Greeting("Android")
    }
}