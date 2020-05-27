package com.simplation.baidumaptrace.utils

/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 14:21
 * @描述:
 * @更新:
 */
object Constants {

    const val TAG = "BaiduTraceSDK_V3"

    const val REQUEST_CODE = 1

    const val RESULT_CODE = 1

    const val DEFAULT_RADIUS_THRESHOLD = 0

    const val PAGE_SIZE = 5000

    /**
     * 默认采集周期
     */
    const val DEFAULT_GATHER_INTERVAL = 5

    /**
     * 默认打包周期
     */
    const val DEFAULT_PACK_INTERVAL = 15

    /**
     * 实时定位间隔(单位:秒)
     */
    const val LOC_INTERVAL = 5

    /**
     * 最后一次定位信息
     */
    const val LAST_LOCATION = "last_location"
}
