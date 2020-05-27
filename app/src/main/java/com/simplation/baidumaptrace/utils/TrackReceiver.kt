package com.simplation.baidumaptrace.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager.WakeLock
import com.baidu.trace.model.StatusCodes


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 15:36
 * @描述: 这个广播意义在于:手机锁屏后一段时间，cpu 可能会进入休眠模式，此时无法严格按照采集周期获取定位依据，导致轨迹点缺失。
 * 避免这种情况的方式是 APP 持有电量锁。还有 doze 模式：Doze模式是 Android6.0 上新出的一种模式，是一种全新的、低能耗的状态，
 * 在后台只有部分任务允许运行，其他都被强制停止。当用户一段时间没有使用手机的时候，Doze模式通过延缓 app 后台的 CPU 和网络活动减少电量的消耗。
 * 若手机厂商生产的定制机型中使用到该模式，需要申请将 app 添加进白名单，可尽量帮助鹰眼服务在后台持续运行
 * @更新:
 */
class TrackReceiver(wakeLock: WakeLock) :BroadcastReceiver() {
    private var wakeLock: WakeLock? = null

    private fun TrackReceiver(wakeLock: WakeLock?) {
        this.wakeLock = wakeLock
    }

    @SuppressLint("Wakelock")
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_SCREEN_OFF == action) {
            if (null != wakeLock && !wakeLock!!.isHeld) {
                wakeLock!!.acquire()
            }
        } else if (Intent.ACTION_SCREEN_ON == action || Intent.ACTION_USER_PRESENT == action) {
            if (null != wakeLock && wakeLock!!.isHeld) {
                wakeLock!!.release()
            }
        } else if (StatusCodes.GPS_STATUS_ACTION == action) {
            val statusCode = intent.getIntExtra("statusCode", 0)
            val statusMessage = intent.getStringExtra("statusMessage")
            println(
                String.format(
                    "GPS status, statusCode:%d, statusMessage:%s", statusCode,
                    statusMessage
                )
            )
        }
    }

}
