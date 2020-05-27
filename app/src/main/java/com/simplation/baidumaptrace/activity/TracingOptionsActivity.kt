package com.simplation.baidumaptrace.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import com.baidu.trace.model.LocationMode
import com.baidu.trace.model.LocationMode.High_Accuracy
import com.simplation.baidumaptrace.R
import com.simplation.baidumaptrace.utils.Constants
import kotlinx.android.synthetic.main.activity_tracing_options.*


class TracingOptionsActivity : BaseActivity() {

    // 返回结果
    private var result: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.tracing_options_title);
        setOptionsButtonInVisible();
        init();
    }

    private fun init() {

        gather_interval.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                val textView = view as EditText
                val hintStr = textView.hint.toString()
                if (hasFocus) {
                    textView.hint = ""
                } else {
                    textView.hint = hintStr
                }
            }
        pack_interval.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                val textView = view as EditText
                val hintStr = textView.hint.toString()
                if (hasFocus) {
                    textView.hint = ""
                } else {
                    textView.hint = hintStr
                }
            }
    }

    fun onCancel(v: View?) {
        super.onBackPressed()
    }

    fun onFinish(v: View?) {
        result = Intent()
        val locationModeRadio: RadioButton =
            findViewById<View>(location_mode.checkedRadioButtonId) as RadioButton
        var locationMode: LocationMode = High_Accuracy // 定位模式
        when (locationModeRadio.id) {
            R.id.device_sensors -> locationMode = LocationMode.Device_Sensors
            R.id.battery_saving -> locationMode = LocationMode.Battery_Saving
            R.id.high_accuracy -> locationMode = High_Accuracy
            else -> {
            }
        }
        result?.putExtra("locationMode", locationMode.name)
        val gatherIntervalStr = gather_interval.text.toString()
        val packIntervalStr = pack_interval.text.toString()
        if (!TextUtils.isEmpty(gatherIntervalStr)) { //采集频率
            try {
                result?.putExtra("gatherInterval", gatherIntervalStr.toInt())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        if (!TextUtils.isEmpty(packIntervalStr)) { //打包频率
            try {
                result?.putExtra("packInterval", packIntervalStr.toInt())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        setResult(Constants.RESULT_CODE, result)
        super.onBackPressed()
    }

    override fun getContentViewId(): Int {
        return R.layout.activity_tracing_options
    }
}
