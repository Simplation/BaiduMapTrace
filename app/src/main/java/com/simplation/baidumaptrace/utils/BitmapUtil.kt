package com.simplation.baidumaptrace.utils

import com.baidu.mapapi.map.BitmapDescriptor
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.simplation.baidumaptrace.R


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 14:57
 * @描述:
 * @更新:
 */
object BitmapUtil {

    var bmArrowPoint: BitmapDescriptor? = null

    var bmStart: BitmapDescriptor? = null

    var bmEnd: BitmapDescriptor? = null

    /**
     * 创建bitmap，在MainActivity onCreate()中调用
     */
    fun init() {
        bmArrowPoint = BitmapDescriptorFactory.fromResource(R.mipmap.icon_point)
        bmStart = BitmapDescriptorFactory.fromResource(R.mipmap.icon_start)
        bmEnd = BitmapDescriptorFactory.fromResource(R.mipmap.icon_end)
    }

    /**
     * 回收bitmap，在MainActivity onDestroy()中调用
     */
    fun clear() {
        bmArrowPoint!!.recycle()
        bmStart!!.recycle()
        bmEnd!!.recycle()
    }
}