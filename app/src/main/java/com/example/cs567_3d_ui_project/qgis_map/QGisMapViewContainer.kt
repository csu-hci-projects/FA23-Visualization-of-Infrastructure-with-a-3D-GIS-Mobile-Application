package com.example.cs567_3d_ui_project.qgis_map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.location.Location
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs567_3d_ui_project.file_logging.LogFile
import com.example.cs567_3d_ui_project.qgis_driver.QGisClient
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.GetMapResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.wms_request_actions.GetMapHttpRequestAction
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.wmts_request_actions.GetCapabilitiesRequestAction
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.wmts_request_actions.GetTileRequestAction
import com.example.cs567_3d_ui_project.qgis_map.qgis_map_elements.QGisMapRecyclerViewAdapter
import com.example.cs567_3d_ui_project.qgis_map.qgis_map_elements.QGisMapTile
import com.google.android.gms.location.FusedLocationProviderClient
import com.otaliastudios.zoom.ZoomApi
import com.otaliastudios.zoom.ZoomEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

//TODO: The guts of this was lifted right out of the ZoomLayout class (https://github.com/natario1/ZoomLayout/tree/main)
//Determine if we can instead inherit from the class to clean up this up!
class QGisMapViewContainer(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @Suppress("MemberVisibilityCanBePrivate") val engine: ZoomEngine = ZoomEngine(context))
    : FrameLayout(context, attrs, defStyleAttr), ViewTreeObserver.OnGlobalLayoutListener, ZoomApi by engine {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0)
            : this(context, attrs, defStyleAttr, ZoomEngine(context))


    private var hasClickableChildren: Boolean = false
    private lateinit var qGisClient: QGisClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var displayMetrics: DisplayMetrics

    private lateinit var qgisMapRecyclerAdapter: QGisMapRecyclerViewAdapter

    private var qGisHelper: QGisHelpers = QGisHelpers()

    init {
        val a = context.theme.obtainStyledAttributes(attrs, com.otaliastudios.zoom.R.styleable.ZoomEngine, defStyleAttr, 0)
        val overScrollHorizontal = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_overScrollHorizontal, true)
        val overScrollVertical = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_overScrollVertical, true)
        val horizontalPanEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_horizontalPanEnabled, true)
        val verticalPanEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_verticalPanEnabled, true)
        val overPinchable = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_overPinchable, true)
        val zoomEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_zoomEnabled, true)
        val flingEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_flingEnabled, true)
        val scrollEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_scrollEnabled, true)
        val oneFingerScrollEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_oneFingerScrollEnabled, true)
        val twoFingersScrollEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_twoFingersScrollEnabled, true)
        val threeFingersScrollEnabled = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_threeFingersScrollEnabled, true)
        val allowFlingInOverscroll = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_allowFlingInOverscroll, true)
        val hasChildren = a.getBoolean(com.otaliastudios.zoom.R.styleable.ZoomEngine_hasClickableChildren, false)
        val minZoom = a.getFloat(com.otaliastudios.zoom.R.styleable.ZoomEngine_minZoom, ZoomApi.MIN_ZOOM_DEFAULT)
        val maxZoom = a.getFloat(com.otaliastudios.zoom.R.styleable.ZoomEngine_maxZoom, ZoomApi.MAX_ZOOM_DEFAULT)
        @ZoomApi.ZoomType val minZoomMode = a.getInteger(com.otaliastudios.zoom.R.styleable.ZoomEngine_minZoomType, ZoomApi.MIN_ZOOM_DEFAULT_TYPE)
        @ZoomApi.ZoomType val maxZoomMode = a.getInteger(com.otaliastudios.zoom.R.styleable.ZoomEngine_maxZoomType, ZoomApi.MAX_ZOOM_DEFAULT_TYPE)
        val transformation = a.getInteger(com.otaliastudios.zoom.R.styleable.ZoomEngine_transformation, ZoomApi.TRANSFORMATION_CENTER_INSIDE)
        val transformationGravity = a.getInt(com.otaliastudios.zoom.R.styleable.ZoomEngine_transformationGravity, ZoomApi.TRANSFORMATION_GRAVITY_AUTO)
        val alignment = a.getInt(com.otaliastudios.zoom.R.styleable.ZoomEngine_alignment, ZoomApi.ALIGNMENT_DEFAULT)
        val animationDuration = a.getInt(com.otaliastudios.zoom.R.styleable.ZoomEngine_animationDuration, ZoomEngine.DEFAULT_ANIMATION_DURATION.toInt()).toLong()
        a.recycle()

        engine.setContainer(this)
        engine.addListener(object: ZoomEngine.Listener {
            override fun onIdle(engine: ZoomEngine) {}
            override fun onUpdate(engine: ZoomEngine, matrix: Matrix) { onUpdate() }
        })
        setTransformation(transformation, transformationGravity)
        setAlignment(alignment)
        setOverScrollHorizontal(overScrollHorizontal)
        setOverScrollVertical(overScrollVertical)
        setHorizontalPanEnabled(horizontalPanEnabled)
        setVerticalPanEnabled(verticalPanEnabled)
        setOverPinchable(overPinchable)
        setZoomEnabled(zoomEnabled)
        setFlingEnabled(flingEnabled)
        setScrollEnabled(scrollEnabled)
        setOneFingerScrollEnabled(oneFingerScrollEnabled)
        setTwoFingersScrollEnabled(twoFingersScrollEnabled)
        setThreeFingersScrollEnabled(threeFingersScrollEnabled)
        setAllowFlingInOverscroll(allowFlingInOverscroll)
        setAnimationDuration(animationDuration)
        setMinZoom(minZoom, minZoomMode)
        setMaxZoom(maxZoom, maxZoomMode)
        setHasClickableChildren(hasChildren)

        setWillNotDraw(false)
    }

    //region Internal

    override fun onGlobalLayout() {
        if (childCount == 0) {
            return
        }
        val child = getChildAt(0)
        engine.setContentSize(child.width.toFloat(), child.height.toFloat())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        // Measure ourselves as MATCH_PARENT
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
            throw RuntimeException("must be used with fixed dimensions (e.g. match_parent)")
        }
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)

        // Measure our child as unspecified.
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measureChildren(spec, spec)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (childCount > 0) {
            throw RuntimeException("accepts only a single child.")
        }
        super.addView(child, index, params)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return engine.onInterceptTouchEvent(ev) || hasClickableChildren && super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return engine.onTouchEvent(ev) || hasClickableChildren && super.onTouchEvent(ev)
    }

    private fun onUpdate() {
        if (hasClickableChildren) {
            if (childCount > 0) {
                val child = getChildAt(0)
                child.pivotX = 0f
                child.pivotY = 0f
                child.translationX = engine.scaledPanX
                child.translationY = engine.scaledPanY
                child.scaleX = engine.realZoom
                child.scaleY = engine.realZoom
            }
        } else {
            invalidate()
        }
        if ((isHorizontalScrollBarEnabled || isVerticalScrollBarEnabled) && !awakenScrollBars()) {
            invalidate()
        }
    }

    override fun computeHorizontalScrollOffset(): Int = engine.computeHorizontalScrollOffset()

    override fun computeHorizontalScrollRange(): Int = engine.computeHorizontalScrollRange()

    override fun computeVerticalScrollOffset(): Int = engine.computeVerticalScrollOffset()

    override fun computeVerticalScrollRange(): Int = engine.computeVerticalScrollRange()

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val result: Boolean

        if (!hasClickableChildren) {
            val save = canvas.save()
            canvas.concat(engine.matrix)
            result = super.drawChild(canvas, child, drawingTime)
            canvas.restoreToCount(save)
        } else {
            result = super.drawChild(canvas, child, drawingTime)
        }

        return result
    }

    fun setQgisClient(qGisClient: QGisClient){
        this.qGisClient = qGisClient
    }

    fun setFusedLocationClient(fusedLocationClient: FusedLocationProviderClient){
        this.fusedLocationClient = fusedLocationClient
    }

    fun setDisplayMetrics(displayMetrics: DisplayMetrics){
        this.displayMetrics = displayMetrics
    }

    suspend fun loadBaseMap(location: Location?){
        val baseMap = withContext(Dispatchers.IO){
            try{
                //20 is the lowest that our tiles can go
                val boundingBox = qGisHelper.createBoundingBoxFromLocation(location, displayMetrics, 20.0)

                var getMapRequestAction = GetMapHttpRequestAction(
                    256,
                    256,
                    listOf("ESRI_Topo"),
                    boundingBox)

                Log.i("test", boundingBox.minX.toString())



                /*val getCapabilitiesResponse = qGisClient.wms.getCapabilities()
                val wmsCapabilities =
                    getCapabilitiesResponse.getCapabilitiesResponseContent.wmsCapabilities

                val esriBasemapLayer = wmsCapabilities.capability.layer.layer.first{it.name.equals("ESRI Topo", true)}

                Log.i("Test2", getCapabilitiesResponse.responseCode.toString())
                Log.i("Test2", wmsCapabilities.toString())*/

                //val getMapResponse: GetMapResponse = qGisClient.wms.getMap(countriesLayer)
                //val getMapResponse: GetMapResponse = qGisClient.wms.getMap(esriBasemapLayer)
                val getMapResponse: GetMapResponse = qGisClient.wms.getMap(req = getMapRequestAction)
                return@withContext getMapResponse.bitmap.asAndroidBitmap()
            }
            catch (e: Exception){
                LogFile().createLog(e.message.toString(), "QGISMapViewContainer: ")
                throw e
            }

        }
        var spanCount = 1
        val baseMapTiles = withContext(Dispatchers.IO){
            try {
                var getCapabilitiesResponse = qGisClient.wmts.getCapabilities(GetCapabilitiesRequestAction())
                var tileMatrix = 2
                var crs = "EPSG:4326"
                var layer = getCapabilitiesResponse.getCapabilitiesResponseContent.capabilities.contents.layer
                var tileMatrixSetLink = layer.tileMatrixSetLink.first{it.tileMatrixSet == crs}
                var tileMatrixSet = getCapabilitiesResponse.getCapabilitiesResponseContent.capabilities.contents
                    .tileMatrixSet.first{it.owsIdentifier == crs}

                //For testing purposes, get the tiles at the highest scale (tile matrix set 0)
                var highestScaleTileLimits = tileMatrixSetLink.timeMatrixSetLimits.tileMatrixLimits.first{it.tileMatrix == tileMatrix}.to2DArray()

                Log.i("Test", highestScaleTileLimits.toString())
                Log.i("Test", tileMatrixSet.toString())

                //Iterate through the 2D array and query each tile within the bounds
                var rowIndexes = highestScaleTileLimits[0]
                var columnIndexes = highestScaleTileLimits[1]
                spanCount = columnIndexes.size
                var tiles = ArrayList<QGisMapTile>(rowIndexes.size * columnIndexes.size)

                //TODO: Consider trying to do this in parallel somehow
                var tileHashMap = withContext(Dispatchers.IO) {
                    var tileHashMap = ConcurrentHashMap<Int, List<QGisMapTile>>()
                    for(i in rowIndexes) {
                        launch {
                            var tileRowSlice = withContext(Dispatchers.IO) {
                                var tileMap = ConcurrentHashMap<Int, QGisMapTile>(columnIndexes.size)
                                for (j in columnIndexes) {

                                    launch {
                                        Log.i("Test", i.toString() + j.toString())
                                        val getTileRequest = GetTileRequestAction(
                                            layer = layer.owsIdentifier,
                                            tileMatrixSet = crs,
                                            tileMatrix = tileMatrix, //hardcoded for now
                                            tileRow = i,
                                            tileCol = j
                                        )
                                        val getTileResponse = qGisClient.wmts.getTile(getTileRequest)

                                        val qGisMapTile = QGisMapTile(
                                            tileMatrix = tileMatrix,
                                            tileRow = i,
                                            tileCol = j,
                                            tile = getTileResponse.bitmap.asAndroidBitmap()
                                        )
                                        tileMap[j] = qGisMapTile
                                    }
                                }
                                return@withContext tileMap
                            }
                            var tileRow = tileRowSlice.values.toList()
                            tileHashMap[i] = tileRow
                        }
                    }
                    return@withContext tileHashMap
                }
                tileHashMap.forEach{tiles.addAll(it.value)}

                return@withContext tiles

            } catch (e: Exception) {
                LogFile().createLog(e.message.toString(), "QGISMapViewContainer: tiles")
                throw e
            }
        }

        try{
            Log.i("Test", baseMapTiles.toString())
            val recyclerView = RecyclerView(context)
            val layoutManager = GridLayoutManager(context, spanCount)

            recyclerView.layoutManager = layoutManager
            qgisMapRecyclerAdapter = QGisMapRecyclerViewAdapter(baseMapTiles)
            recyclerView.adapter = qgisMapRecyclerAdapter
            addView(recyclerView, 0)
            qgisMapRecyclerAdapter.notifyDataSetChanged()

        }catch (e: Exception){
            LogFile().createLog(e.message.toString(), "QGISMapViewContainer: region")
            throw e
        }

    }

    //endregion

    //region APIs


    /**
     * Whether the view hierarchy inside has (or will have) clickable children.
     * This is false by default.
     *
     * @param hasClickableChildren whether we have clickable children
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setHasClickableChildren(hasClickableChildren: Boolean) {
        //LOG.i("setHasClickableChildren:", "old:", this.hasClickableChildren, "new:", hasClickableChildren)
        if (this.hasClickableChildren && !hasClickableChildren) {
            // Revert any transformation that was applied to our child.
            if (childCount > 0) {
                val child = getChildAt(0)
                child.scaleX = 1f
                child.scaleY = 1f
                child.translationX = 0f
                child.translationY = 0f
            }
        }
        this.hasClickableChildren = hasClickableChildren

        // Update if we were laid out already.
        if (width > 0 && height > 0) {
            if (this.hasClickableChildren) {
                onUpdate()
            } else {
                invalidate()
            }
        }
    }

    //endregion
/*
    companion object {
        private val TAG = ZoomLayout::class.java.simpleName
        private val LOG = ZoomLogger.create(TAG)
    }
    */


}