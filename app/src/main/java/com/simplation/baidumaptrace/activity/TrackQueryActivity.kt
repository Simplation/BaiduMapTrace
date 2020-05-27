package com.simplation.baidumaptrace.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.track.*
import com.baidu.trace.model.*
import com.simplation.baidumaptrace.MyApp
import com.simplation.baidumaptrace.MyApp.Companion.mClient
import com.simplation.baidumaptrace.R
import com.simplation.baidumaptrace.utils.CommonUtil.getCurrentTime
import com.simplation.baidumaptrace.utils.CommonUtil.isZeroPoint
import com.simplation.baidumaptrace.utils.Constants
import com.simplation.baidumaptrace.utils.MapUtil
import com.simplation.baidumaptrace.utils.ViewUtil
import kotlinx.android.synthetic.main.activity_top.*


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 15:21
 * @描述: 轨迹查询
 * @更新:
 */
class TrackQueryActivity : BaseActivity(), View.OnClickListener {

    /**
     * 历史轨迹请求
     */
    private val historyTrackRequest = HistoryTrackRequest()

    /**
     * 轨迹监听器（用于接收历史轨迹回调）
     */
    private var mTrackListener: OnTrackListener? = null

    /**
     * 查询轨迹的开始时间
     */
    private var startTime = getCurrentTime()

    /**
     * 查询轨迹的结束时间
     */
    private var endTime = getCurrentTime()

    /**
     * 轨迹点集合
     */
    private var trackPoints: ArrayList<LatLng>? = ArrayList()

    /**
     * 轨迹排序规则
     */
    private val sortType: SortType = SortType.asc

    private var pageIndex = 1

    companion object {
        private lateinit var myApp: MyApp

        /**
         * 地图工具
         */
        private lateinit var mapUtil: MapUtil

        private lateinit var viewUtil: ViewUtil
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.track_query_title)
        setOptionsText()
        setOnClickListener(this)
        myApp = applicationContext as MyApp
        init()
    }

    private fun setOptionsText() {
        tv_options.text = "查询条件设置"
    }

    /**
     * 初始化
     */
    private fun init() {
        viewUtil = ViewUtil()
        mapUtil = MapUtil.getInstance()
        mapUtil.init(findViewById(R.id.track_query_mapView))
        initListener()
    }

    /**
     * 轨迹查询设置回调
     *
     * @param historyTrackRequestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(
        historyTrackRequestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (null == data) {
            return
        }
        trackPoints?.clear()
        pageIndex = 1
        if (data.hasExtra("startTime")) {
            startTime = data.getLongExtra("startTime", getCurrentTime())
        }
        if (data.hasExtra("endTime")) {
            endTime = data.getLongExtra("endTime", getCurrentTime())
        }
        val processOption = ProcessOption()
        if (data.hasExtra("radius")) {
            processOption.radiusThreshold = data.getIntExtra(
                "radius",
                Constants.DEFAULT_RADIUS_THRESHOLD
            )
        }
        processOption.transportMode = TransportMode.walking
        if (data.hasExtra("denoise")) { // 去噪
            processOption.isNeedDenoise = data.getBooleanExtra("denoise", true)
        }
        if (data.hasExtra("vacuate")) { // 抽稀
            processOption.isNeedVacuate = data.getBooleanExtra("vacuate", true)
        }
        if (data.hasExtra("mapmatch")) { // 绑路
            processOption.isNeedMapMatch = data.getBooleanExtra("mapmatch", true)
        }
        historyTrackRequest.processOption = processOption
        if (data.hasExtra("processed")) { // 纠偏
            historyTrackRequest.isProcessed = data.getBooleanExtra("processed", true)
        }
        queryHistoryTrack()
    }

    /**
     * 查询历史轨迹
     */
    private fun queryHistoryTrack() {
        myApp.initRequest(historyTrackRequest)
        historyTrackRequest.supplementMode = SupplementMode.no_supplement
        historyTrackRequest.sortType = SortType.asc
        historyTrackRequest.coordTypeOutput = CoordType.bd09ll
        historyTrackRequest.entityName = myApp.entityName
        historyTrackRequest.startTime = startTime
        historyTrackRequest.endTime = endTime
        historyTrackRequest.pageIndex = pageIndex
        historyTrackRequest.pageSize = Constants.PAGE_SIZE
        mClient.queryHistoryTrack(historyTrackRequest, mTrackListener)
    }

    /**
     * 按钮点击事件
     *
     * @param view
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_activity_options -> ViewUtil.startActivityForResult(
                this,
                TrackQueryOptionsActivity::class.java, Constants.REQUEST_CODE
            )
            else -> {
            }
        }
    }

    private fun initListener() {
        mTrackListener = object : OnTrackListener() {
            override fun onHistoryTrackCallback(response: HistoryTrackResponse) {
                try {
                    val total = response.total
                    if (StatusCodes.SUCCESS !== response.getStatus()) {
                        viewUtil.showToast(this@TrackQueryActivity, response.getMessage())
                    } else if (0 == total) {
                        viewUtil.showToast(
                            this@TrackQueryActivity,
                            getString(R.string.no_track_data)
                        )
                    } else {
                        val points = response.getTrackPoints()
                        if (null != points) {
                            for (trackPoint in points) {
                                if (!isZeroPoint(
                                        trackPoint.location.getLatitude(),
                                        trackPoint.location.getLongitude()
                                    )
                                ) {
                                    trackPoints?.add(MapUtil.convertTrace2Map(trackPoint.location))
                                }
                            }
                        }
                    }

                    //查找下一页数据
                    if (total > Constants.PAGE_SIZE * pageIndex) {
                        historyTrackRequest.pageIndex = ++pageIndex
                        queryHistoryTrack()
                    } else {
                        mapUtil.drawHistoryTrack(trackPoints, true, 0) // 画轨迹
                    }
                    queryDistance() // 查询里程
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDistanceCallback(response: DistanceResponse) {
                viewUtil.showToast(this@TrackQueryActivity, "里程：" + response.distance)
                super.onDistanceCallback(response)
            }
        }
    }

    private fun queryDistance() {
        val distanceRequest =
            DistanceRequest(myApp.getTAG(), myApp.serviceId, myApp.entityName)
        distanceRequest.startTime = startTime // 设置开始时间
        distanceRequest.endTime = endTime // 设置结束时间
        distanceRequest.isProcessed = true // 纠偏
        val processOption = ProcessOption() // 创建纠偏选项实例
        processOption.isNeedDenoise = true // 去噪
        processOption.isNeedMapMatch = true // 绑路
        processOption.transportMode = TransportMode.walking // 交通方式为步行
        distanceRequest.processOption = processOption // 设置纠偏选项
        distanceRequest.supplementMode = SupplementMode.no_supplement // 里程填充方式为无
        mClient.queryDistance(distanceRequest, mTrackListener) // 查询里程
    }


    override fun onResume() {
        super.onResume()
        mapUtil.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapUtil.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (null != trackPoints) {
            trackPoints?.clear()
        }
        trackPoints = null
        mapUtil.clear()
    }

    override fun getContentViewId(): Int {
        return R.layout.activity_trackquery
    }
}
