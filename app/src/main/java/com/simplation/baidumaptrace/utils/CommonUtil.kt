package com.simplation.baidumaptrace.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.telephony.TelephonyManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 14:17
 * @描述:
 * @更新:
 */
object CommonUtil {
    fun getCurProcessName(context: Context): String {
        val pid = Process.myPid()
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (appProcess in activityManager.runningAppProcesses) {
            if (appProcess.pid == pid) {
                return appProcess.processName
            }
        }
        return ""
    }

    /**
     * 获取当前时间戳(单位：秒)
     */
    fun getCurrentTime(): Long {
        return System.currentTimeMillis() / 1000
    }

    /**
     * 校验double数值是否为0
     */
    fun isEqualToZero(value: Double): Boolean {
        return abs(value - 0.0) < 0.01
    }

    /**
     * 经纬度是否为(0,0)点
     */
    fun isZeroPoint(latitude: Double, longitude: Double): Boolean {
        return isEqualToZero(latitude) && isEqualToZero(longitude)
    }

    /**
     * 将字符串转为时间戳
     */
    fun toTimeStamp(time: String?): Long {
        val sdf = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.CHINA
        )
        val date: Date
        date = try {
            sdf.parse(time)
        } catch (e: ParseException) {
            e.printStackTrace()
            return 0
        }
        return date.getTime() / 1000
    }

    /**
     * 获取设备IMEI码
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun getIMEI(context: Context): String {
        return try {
            (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
        } catch (e: Exception) {
            "myTrace"
        }
    }
}

