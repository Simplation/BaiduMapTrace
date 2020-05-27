package com.simplation.baidumaptrace.utils

import android.graphics.Color
import com.baidu.mapapi.map.*
import com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.model.LatLngBounds
import com.baidu.mapapi.utils.CoordinateConverter
import com.baidu.trace.model.CoordType
import com.baidu.trace.model.TraceLocation
import com.simplation.baidumaptrace.model.CurrentLocation
import com.simplation.baidumaptrace.utils.BitmapUtil.bmEnd
import com.simplation.baidumaptrace.utils.BitmapUtil.bmStart
import com.simplation.baidumaptrace.utils.CommonUtil.isZeroPoint


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 15:26
 * @描述:
 * @更新:
 */
class MapUtil {

    private var lastPoint: LatLng? = null
    private var mMoveMarker: Marker? = null

    var baiduMap: BaiduMap? = null

    private var mapStatus: MapStatus? = null

    private var mapView: MapView? = null

    companion object {
        private val INSTANCE: MapUtil = MapUtil()

        private var locData: MyLocationData? = null

        /**
         * 路线覆盖物
         */
        var polylineOverlay: Overlay? = null

        fun getInstance(): MapUtil {
            return INSTANCE
        }

        /**
         * 将轨迹坐标对象转换为地图坐标对象
         */
        fun convertTrace2Map(traceLatLng: com.baidu.trace.model.LatLng): LatLng {
            return LatLng(traceLatLng.latitude, traceLatLng.longitude)
        }
    }

    private var mCurrentZoom = 18.0f

    fun init(view: MapView?) {
        mapView = view
        baiduMap = mapView?.map
        mapView?.showZoomControls(false)
        baiduMap?.isMyLocationEnabled = true
        baiduMap?.setMyLocationConfigeration(
            MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                null
            )
        )
        baiduMap?.setOnMapStatusChangeListener(object : OnMapStatusChangeListener {
            // 缩放比例变化监听
            override fun onMapStatusChangeStart(mapStatus: MapStatus) {}

            override fun onMapStatusChangeStart(p0: MapStatus?, p1: Int) {

            }

            override fun onMapStatusChange(mapStatus: MapStatus) {
                mCurrentZoom = mapStatus.zoom
            }

            override fun onMapStatusChangeFinish(mapStatus: MapStatus) {}
        })
    }

    fun onPause() {
        if (null != mapView) {
            mapView?.onPause()
        }
    }

    fun onResume() {
        if (null != mapView) {
            mapView?.onResume()
        }
    }

    fun clear() {
        lastPoint = null
        if (null != mMoveMarker) {
            mMoveMarker?.remove()
            mMoveMarker = null
        }
        if (null != polylineOverlay) {
            polylineOverlay!!.remove()
            polylineOverlay = null
        }
        if (null != baiduMap) {
            baiduMap?.clear()
            baiduMap = null
        }
        mapStatus = null
        if (null != mapView) {
            mapView?.onDestroy()
            mapView = null
        }
    }

    /**
     * 将轨迹实时定位点转换为地图坐标
     */
    fun convertTraceLocation2Map(location: TraceLocation?): LatLng? {
        if (null == location) {
            return null
        }
        val latitude = location.latitude
        val longitude = location.longitude
        if (kotlin.math.abs(latitude - 0.0) < 0.000001 && kotlin.math.abs(longitude - 0.0) < 0.000001) {
            return null
        }
        var currentLatLng =
            LatLng(latitude, longitude)
        if (CoordType.wgs84 == location.coordType) {
            val sourceLatLng = currentLatLng
            val converter = CoordinateConverter()
            converter.from(CoordinateConverter.CoordType.GPS)
            converter.coord(sourceLatLng)
            currentLatLng = converter.convert()
        }
        return currentLatLng
    }

    /**
     * 将轨迹坐标对象转换为地图坐标对象
     */
    fun convertTrace2Map(traceLatLng: com.baidu.trace.model.LatLng): LatLng? {
        return LatLng(traceLatLng.latitude, traceLatLng.longitude)
    }

    /**
     * 设置地图中心：使用已有定位信息；
     */
    fun setCenter(direction: Int) {
        if (!isZeroPoint(CurrentLocation.latitude, CurrentLocation.longitude)) {
            val currentLatLng =
                LatLng(CurrentLocation.latitude, CurrentLocation.longitude)
            updateMapLocation(currentLatLng, direction)
            animateMapStatus(currentLatLng)
            return
        }
    }

    fun updateMapLocation(
        currentPoint: LatLng?,
        direction: Int
    ) {
        if (currentPoint == null) {
            return
        }
        locData = MyLocationData.Builder().accuracy(0f).direction(direction.toFloat())
            .latitude(currentPoint.latitude).longitude(currentPoint.longitude).build()
        baiduMap?.setMyLocationData(locData)
    }

    /**
     * 绘制历史轨迹
     */
    fun drawHistoryTrack(
        points: List<LatLng?>?,
        staticLine: Boolean,
        direction: Int
    ) {
        // 绘制新覆盖物前，清空之前的覆盖物
        baiduMap?.clear()
        if (points == null || points.isEmpty()) {
            if (null != polylineOverlay) {
                polylineOverlay!!.remove()
                polylineOverlay = null
            }
            return
        }
        if (points.size == 1) {
            val startOptions: OverlayOptions = MarkerOptions().position(points[0]).icon(bmStart)
                .zIndex(9).draggable(true)
            baiduMap?.addOverlay(startOptions)
            updateMapLocation(points[0], direction)
            animateMapStatus(points[0])
            return
        }
        val startPoint = points[0]
        val endPoint = points[points.size - 1]

        // 添加起点图标
        val startOptions: OverlayOptions = MarkerOptions()
            .position(startPoint).icon(bmStart)
            .zIndex(9).draggable(true)

        // 添加路线（轨迹）
        val polylineOptions: OverlayOptions = PolylineOptions().width(10)
            .color(Color.BLUE).points(points)
        if (staticLine) {
            // 添加终点图标
            drawEndPoint(endPoint)
        }
        baiduMap?.addOverlay(startOptions)
        polylineOverlay = baiduMap?.addOverlay(polylineOptions)
        if (staticLine) {
            animateMapStatus(points)
        } else {
            updateMapLocation(points[points.size - 1], direction)
            animateMapStatus(points[points.size - 1])
        }
    }

    fun drawEndPoint(endPoint: LatLng?) {
        // 添加终点图标
        val endOptions: OverlayOptions = MarkerOptions().position(endPoint)
            .icon(bmEnd).zIndex(9).draggable(true)
        baiduMap?.addOverlay(endOptions)
    }

    private fun animateMapStatus(points: List<LatLng?>?) {
        if (null == points || points.isEmpty()) {
            return
        }
        val builder = LatLngBounds.Builder()
        for (point in points) {
            builder.include(point)
        }
        val msUpdate = MapStatusUpdateFactory.newLatLngBounds(builder.build())
        baiduMap?.animateMapStatus(msUpdate)
    }

    fun animateMapStatus(point: LatLng?) {
        val builder = MapStatus.Builder()
        mapStatus = builder.target(point).zoom(mCurrentZoom).build()
        baiduMap?.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus))
    }

}