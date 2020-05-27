package com.simplation.baidumaptrace.utils

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 14:17
 * @描述:
 * @更新:
 */
object NetUtil {

    /**
     * 检测网络状态是否联通
     */
    fun isNetworkAvailable(context: Context): Boolean {
        try {
            val cm =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = cm.activeNetworkInfo
            if (null != info && info.isConnected && info.isAvailable) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(Constants.TAG, "current network is not available")
            return false
        }
        return false
    }
}
