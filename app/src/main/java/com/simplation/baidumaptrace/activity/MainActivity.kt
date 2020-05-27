package com.simplation.baidumaptrace.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.baidu.mapapi.SDKInitializer
import com.simplation.baidumaptrace.MyApp
import com.simplation.baidumaptrace.MyApp.Companion.mClient
import com.simplation.baidumaptrace.MyApp.Companion.mTrace
import com.simplation.baidumaptrace.MyApp.Companion.trackConf
import com.simplation.baidumaptrace.R
import com.simplation.baidumaptrace.utils.BitmapUtil
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {

    companion object {
        private lateinit var myApp: MyApp

        private lateinit var mReceiver: SDKReceiver
    }

    /**
     * 构造广播监听类，监听 SDK key 验证以及网络异常广播
     */
    class SDKReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action!!) {
                SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR -> {
                    Toast.makeText(context, "apikey验证失败，地图功能无法正常使用", Toast.LENGTH_SHORT).show()
                }

                SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK -> {
                    Toast.makeText(context, "apikey验证成功", Toast.LENGTH_SHORT).show()
                }

                SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR -> {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // apikey 的授权需要一定的时间，在授权成功之前地图相关操作会出现异常；apikey 授权成功后会发送广播通知，我们这里注册 SDK 广播监听者
        val iFilter = IntentFilter()
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)
        mReceiver = SDKReceiver()
        registerReceiver(mReceiver, iFilter)

        init()
        BitmapUtil.init()
    }

    private fun init() {
        myApp = applicationContext as MyApp

        // 轨迹追踪
        btn_trace.setOnClickListener {
            val intent = Intent(this@MainActivity, TracingActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
            startActivity(intent)
        }

        // 轨迹查询
        btn_query.setOnClickListener {
            val intent = Intent(this@MainActivity, TrackQueryActivity::class.java)
            startActivity(intent)
        }

        // 历史轨迹回放
        btn_back.setOnClickListener {
            val intent = Intent(this@MainActivity, TrackBackActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // 适配android M，检查权限
        val permissions: List<String> = ArrayList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isNeedRequestPermissions(permissions as MutableList<String>)) {
            requestPermissions(permissions.toTypedArray(), 0)
        }
    }

    override fun getContentViewId(): Int {
        return R.layout.activity_main
    }

    private fun isNeedRequestPermissions(permissions: MutableList<String>): Boolean {
        // 定位精确位置
        addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION)
        // 存储权限
        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        addPermission(permissions, Manifest.permission.READ_EXTERNAL_STORAGE)
        // 读取手机状态
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE)
        return permissions.size > 0
    }

    private fun addPermission(
        permissionsList: MutableList<String>,
        permission: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsList.add(permission)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        if (trackConf.contains("is_trace_started")
            && trackConf.getBoolean("is_trace_started", true)
        ) {
            // 退出app停止轨迹服务时，不再接收回调，将OnTraceListener置空
            mClient.setOnTraceListener(null)
            mClient.stopTrace(mTrace, null)
            mClient.clear()
        } else {
            mClient.clear()
        }
        myApp.isTraceStarted = false
        myApp.isGatherStarted = false
        val editor: Editor = trackConf.edit()
        editor.remove("is_trace_started")
        editor.remove("is_gather_started")
        editor.apply()
        BitmapUtil.clear()
    }
}
