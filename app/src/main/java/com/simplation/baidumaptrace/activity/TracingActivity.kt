package com.simplation.baidumaptrace.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.entity.OnEntityListener
import com.baidu.trace.api.track.LatestPointResponse
import com.baidu.trace.api.track.OnTrackListener
import com.baidu.trace.model.*
import com.simplation.baidumaptrace.MyApp
import com.simplation.baidumaptrace.MyApp.Companion.isRegisterReceiver
import com.simplation.baidumaptrace.MyApp.Companion.mClient
import com.simplation.baidumaptrace.MyApp.Companion.mTrace
import com.simplation.baidumaptrace.MyApp.Companion.trackConf
import com.simplation.baidumaptrace.R
import com.simplation.baidumaptrace.model.CurrentLocation
import com.simplation.baidumaptrace.utils.CommonUtil.isZeroPoint
import com.simplation.baidumaptrace.utils.CommonUtil.toTimeStamp
import com.simplation.baidumaptrace.utils.Constants
import com.simplation.baidumaptrace.utils.MapUtil
import com.simplation.baidumaptrace.utils.TrackReceiver
import com.simplation.baidumaptrace.utils.ViewUtil
import kotlinx.android.synthetic.main.activity_top.*
import kotlinx.android.synthetic.main.activity_tracing.*
import kotlin.math.abs


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 15:21
 * @描述: 轨迹追踪
 * @更新:
 */
class TracingActivity : BaseActivity(), View.OnClickListener, SensorEventListener {

    /**
     * 打包周期
     */
    var packInterval: Int = Constants.DEFAULT_PACK_INTERVAL

    private var lastX = 0.0
    private var mCurrentDirection = 0

    private var firstLocate = true

    private var trackReceiver: TrackReceiver? = null

    /**
     * 轨迹点集合
     */
    private var trackPoints: ArrayList<LatLng>? = null

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {

        private lateinit var myApp: MyApp

        private lateinit var viewUtil: ViewUtil

        private lateinit var powerManager: PowerManager


        private lateinit var mSensorManager: SensorManager

        /**
         * 实时定位任务
         */
        private var realTimeHandler = RealTimeHandler()

        /**
         * 地图工具
         */
        private lateinit var mapUtil: MapUtil

        /**
         * 轨迹服务监听器
         */
        private lateinit var traceListener: OnTraceListener

        /**
         * 轨迹监听器(用于接收纠偏后实时位置回调)
         */
        private lateinit var trackListener: OnTrackListener

        /**
         * Entity监听器(用于接收实时定位回调)
         */
        private lateinit var entityListener: OnEntityListener

        private lateinit var realTimeLocRunnable: RealTimeLocRunnable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.tracing_title)
        setOptionsText()
        setOnClickListener(this)
        init()
    }

    private fun setOptionsText() {
        tv_options.text = "轨迹追踪设置"
    }

    private fun init() {
        initListener()
        myApp = applicationContext as MyApp
        viewUtil = ViewUtil()
        mapUtil = MapUtil.getInstance()
        mapUtil.init(findViewById(R.id.tracing_mapView))
        mapUtil.setCenter(mCurrentDirection) // 设置地图中心点
        powerManager = myApp.getSystemService(Context.POWER_SERVICE) as PowerManager
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager // 获取传感器管理服务
        btn_trace.setOnClickListener(this)
        btn_gather.setOnClickListener(this)
        setbtn_traceStyle()
        setbtn_gatherStyle()
        trackPoints = ArrayList()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        // 每次方向改变，重新给地图设置定位数据，用上一次的经纬度
        val x = sensorEvent.values[SensorManager.DATA_X].toDouble()
        if (abs(x - lastX) > 1.0) { // 方向改变大于 1 度才设置，以免地图上的箭头转动过于频繁
            mCurrentDirection = x.toInt()
            if (!isZeroPoint(CurrentLocation.latitude, CurrentLocation.longitude)) {
                mapUtil.updateMapLocation(
                    LatLng(
                        CurrentLocation.latitude,
                        CurrentLocation.longitude
                    ), mCurrentDirection
                )
            }
        }
        lastX = x
    }

    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_activity_options -> ViewUtil.startActivityForResult(
                this,
                TracingOptionsActivity::class.java, Constants.REQUEST_CODE
            )
            R.id.btn_trace -> if (myApp.isTraceStarted) {
                mClient.stopTrace(mTrace, traceListener) // 停止服务
            } else {
                mClient.startTrace(mTrace, traceListener) // 开始服务
            }
            R.id.btn_gather -> if (myApp.isGatherStarted) {
                mClient.stopGather(traceListener)
            } else {
                mClient.setInterval(Constants.DEFAULT_GATHER_INTERVAL, packInterval)
                mClient.startGather(traceListener) // 开启采集
            }
            else -> {
            }
        }
    }

    /**
     * 设置服务按钮样式
     */
    private fun setbtn_traceStyle() {
        val isTraceStarted: Boolean =
            trackConf.getBoolean("is_trace_started", false)
        if (isTraceStarted) {
            btn_trace.setText(R.string.stop_trace)
            btn_trace.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btn_trace.background = ResourcesCompat.getDrawable(
                    resources,
                    R.mipmap.bg_btn_sure, null
                )
            } else {
                btn_trace.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.mipmap.bg_btn_sure, null
                    )
                )
            }
        } else {
            btn_trace.setText(R.string.start_trace)
            btn_trace.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.layout_title,
                    null
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btn_trace.background = ResourcesCompat.getDrawable(
                    resources,
                    R.mipmap.bg_btn_cancel, null
                )
            } else {
                btn_trace.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.mipmap.bg_btn_cancel, null
                    )
                )
            }
        }
    }

    /**
     * 设置采集按钮样式
     */
    private fun setbtn_gatherStyle() {
        val isGatherStarted: Boolean =
            trackConf.getBoolean("is_gather_started", false)
        if (isGatherStarted) {
            btn_gather.setText(R.string.stop_gather)
            btn_gather.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btn_gather.background = ResourcesCompat.getDrawable(
                    resources,
                    R.mipmap.bg_btn_sure, null
                )
            } else {
                btn_gather.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.mipmap.bg_btn_sure, null
                    )
                )
            }
        } else {
            btn_gather.setText(R.string.start_gather)
            btn_gather.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.layout_title,
                    null
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btn_gather.background = ResourcesCompat.getDrawable(
                    resources,
                    R.mipmap.bg_btn_cancel, null
                )
            } else {
                btn_gather.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.mipmap.bg_btn_cancel, null
                    )
                )
            }
        }
    }

    /**
     * 实时定位任务
     */
    internal class RealTimeLocRunnable(interval: Int) : Runnable {
        private var interval = 0
        override fun run() {
            myApp.getCurrentLocation(entityListener, trackListener)
            realTimeHandler.postDelayed(this, (interval * 1000).toLong())
        }

        init {
            this.interval = interval
        }
    }

    fun startRealTimeLoc(interval: Int) {
        realTimeLocRunnable = RealTimeLocRunnable(interval)
        realTimeHandler.post(realTimeLocRunnable)
    }

    fun stopRealTimeLoc() {
        if (null != realTimeHandler && null != realTimeLocRunnable) {
            realTimeHandler.removeCallbacks(realTimeLocRunnable)
        }
        mClient.stopRealTimeLoc()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (null == data) {
            return
        }
        if (data.hasExtra("locationMode")) {
            val locationMode =
                LocationMode.valueOf(data.getStringExtra("locationMode"))
            mClient.setLocationMode(locationMode) // 定位模式
        }
        mTrace.isNeedObjectStorage = false
        if (data.hasExtra("gatherInterval") && data.hasExtra("packInterval")) {
            val gatherInterval =
                data.getIntExtra("gatherInterval", Constants.DEFAULT_GATHER_INTERVAL)
            val packInterval =
                data.getIntExtra("packInterval", Constants.DEFAULT_PACK_INTERVAL)
            this@TracingActivity.packInterval = packInterval
            mClient.setInterval(gatherInterval, packInterval) // 设置频率
        }
    }

    private fun initListener() {
        trackListener = object : OnTrackListener() {
            override fun onLatestPointCallback(response: LatestPointResponse) {
                // 经过服务端纠偏后的最新的一个位置点，回调
                try {
                    if (StatusCodes.SUCCESS !== response.getStatus()) {
                        return
                    }
                    val point = response.latestPoint
                    if (null == point || isZeroPoint(
                            point.location.getLatitude(), point.location
                                .getLongitude()
                        )
                    ) {
                        return
                    }
                    val currentLatLng =
                        mapUtil.convertTrace2Map(point.location) ?: return
                    if (firstLocate) {
                        firstLocate = false
                        Toast.makeText(this@TracingActivity, "起点获取中，请稍后...", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }

                    //当前经纬度
                    CurrentLocation.locTime = point.locTime
                    CurrentLocation.latitude = currentLatLng.latitude
                    CurrentLocation.longitude = currentLatLng.longitude
                    if (trackPoints == null) {
                        return
                    }
                    trackPoints?.add(currentLatLng)
                    mapUtil.drawHistoryTrack(trackPoints, false, mCurrentDirection) // 时时动态的画出运动轨迹
                } catch (x: Exception) {
                    x.printStackTrace()
                }
            }
        }
        entityListener = object : OnEntityListener() {
            override fun onReceiveLocation(location: TraceLocation) {
                // 本地 LBSTraceClient 客户端获取的位置
                try {
                    if (StatusCodes.SUCCESS !== location.getStatus() || isZeroPoint(
                            location.latitude,
                            location.longitude
                        )
                    ) {
                        return
                    }
                    val currentLatLng =
                        mapUtil.convertTraceLocation2Map(location) ?: return
                    CurrentLocation.locTime = toTimeStamp(location.time)
                    CurrentLocation.latitude = currentLatLng.latitude
                    CurrentLocation.longitude = currentLatLng.longitude
                    if (null != mapUtil) {
                        mapUtil.updateMapLocation(currentLatLng, mCurrentDirection) // 显示当前位置
                        mapUtil.animateMapStatus(currentLatLng) // 缩放
                    }
                } catch (x: Exception) {
                    x.printStackTrace()
                }
            }
        }
        traceListener = object : OnTraceListener {
            override fun onBindServiceCallback(errorNo: Int, message: String) {
                viewUtil.showToast(
                    this@TracingActivity,
                    String.format(
                        "onBindServiceCallback, errorNo:%d, message:%s ",
                        errorNo,
                        message
                    )
                )
            }

            override fun onInitBOSCallback(p0: Int, p1: String?) {

            }

            override fun onStartTraceCallback(errorNo: Int, message: String) {
                if (StatusCodes.SUCCESS === errorNo || StatusCodes.START_TRACE_NETWORK_CONNECT_FAILED <= errorNo) {
                    myApp.isTraceStarted = true
                    val editor: SharedPreferences.Editor = trackConf.edit()
                    editor.putBoolean("is_trace_started", true)
                    editor.apply()
                    setbtn_traceStyle()
                    registerReceiver()
                }
                viewUtil.showToast(
                    this@TracingActivity,
                    String.format(
                        "onStartTraceCallback, errorNo:%d, message:%s ",
                        errorNo,
                        message
                    )
                )
            }

            override fun onStopTraceCallback(errorNo: Int, message: String) {
                if (StatusCodes.SUCCESS === errorNo || StatusCodes.CACHE_TRACK_NOT_UPLOAD === errorNo) {
                    myApp.isTraceStarted = false
                    myApp.isGatherStarted = false
                    // 停止成功后，直接移除 is_trace_started 记录（便于区分用户没有停止服务，直接杀死进程的情况）
                    val editor: SharedPreferences.Editor = trackConf.edit()
                    editor.remove("is_trace_started")
                    editor.remove("is_gather_started")
                    editor.apply()
                    setbtn_traceStyle()
                    setbtn_gatherStyle()
                    unregisterPowerReceiver()
                    firstLocate = true
                }
                viewUtil.showToast(
                    this@TracingActivity,
                    String.format(
                        "onStopTraceCallback, errorNo:%d, message:%s ",
                        errorNo,
                        message
                    )
                )
            }

            override fun onStartGatherCallback(errorNo: Int, message: String) {
                if (StatusCodes.SUCCESS === errorNo || StatusCodes.GATHER_STARTED === errorNo) {
                    myApp.isGatherStarted = true
                    val editor: SharedPreferences.Editor = trackConf.edit()
                    editor.putBoolean("is_gather_started", true)
                    editor.apply()
                    setbtn_gatherStyle()
                    stopRealTimeLoc()
                    startRealTimeLoc(packInterval)
                }
                viewUtil.showToast(
                    this@TracingActivity,
                    String.format(
                        "onStartGatherCallback, errorNo:%d, message:%s ",
                        errorNo,
                        message
                    )
                )
            }

            override fun onStopGatherCallback(errorNo: Int, message: String) {
                if (StatusCodes.SUCCESS === errorNo || StatusCodes.GATHER_STOPPED === errorNo) {
                    myApp.isGatherStarted = false
                    val editor: SharedPreferences.Editor = trackConf.edit()
                    editor.remove("is_gather_started")
                    editor.apply()
                    setbtn_gatherStyle()
                    firstLocate = true
                    stopRealTimeLoc()
                    startRealTimeLoc(Constants.LOC_INTERVAL)
                    if (trackPoints?.size!! >= 1) {
                        try {
                            mapUtil.drawEndPoint(trackPoints?.get(trackPoints!!.size - 1))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                viewUtil.showToast(
                    this@TracingActivity,
                    String.format(
                        "onStopGatherCallback, errorNo:%d, message:%s ",
                        errorNo,
                        message
                    )
                )
            }

            override fun onPushCallback(
                messageType: Byte,
                pushMessage: PushMessage
            ) {
            }
        }
    }

    internal class RealTimeHandler : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
        }
    }

    /**
     * 注册广播（电源锁、GPS状态）
     */
    private fun registerReceiver() {
        if (isRegisterReceiver) {
            return
        }
        if (null == wakeLock) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "track upload")
        }
        if (null == trackReceiver) {
            trackReceiver = wakeLock?.let { TrackReceiver(it) }!!
        }
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(StatusCodes.GPS_STATUS_ACTION)
        myApp.registerReceiver(trackReceiver, filter)
        isRegisterReceiver = true
    }

    private fun unregisterPowerReceiver() {
        if (!isRegisterReceiver) {
            return
        }
        if (null != trackReceiver) {
            myApp.unregisterReceiver(trackReceiver)
        }
        isRegisterReceiver = false
    }

    override fun onStart() {
        super.onStart()
        if (trackConf.contains("is_trace_started")
            && trackConf.contains("is_gather_started")
            && trackConf.getBoolean("is_trace_started", false)
            && trackConf.getBoolean("is_gather_started", false)
        ) {
            startRealTimeLoc(packInterval)
        } else {
            startRealTimeLoc(Constants.LOC_INTERVAL)
        }
    }

    override fun onResume() {
        super.onResume()
        mapUtil.onResume()
        mSensorManager.registerListener(
            this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_UI
        )

        // 在 Android 6.0 及以上系统，若定制手机使用到 doze 模式，请求将应用添加到白名单。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName: String = myApp.packageName
            val isIgnoring =
                powerManager.isIgnoringBatteryOptimizations(packageName)
            if (!isIgnoring) {
                val intent =
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mapUtil.onPause()
    }

    override fun onStop() {
        super.onStop()
        stopRealTimeLoc()
        mSensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRealTimeLoc()
        trackPoints?.clear()
        trackPoints = null
        mapUtil.clear()
    }

    override fun getContentViewId(): Int {
        return R.layout.activity_tracing
    }
}
