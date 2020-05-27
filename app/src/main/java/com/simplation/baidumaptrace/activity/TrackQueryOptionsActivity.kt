package com.simplation.baidumaptrace.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.CompoundButton
import com.simplation.baidumaptrace.MyApp
import com.simplation.baidumaptrace.R
import com.simplation.baidumaptrace.utils.CommonUtil.getCurrentTime
import com.simplation.baidumaptrace.utils.Constants
import com.simplation.baidumaptrace.utils.DateDialog
import kotlinx.android.synthetic.main.activity_trackquery_options.*
import java.text.SimpleDateFormat
import java.util.*

class TrackQueryOptionsActivity : BaseActivity(), CompoundButton.OnCheckedChangeListener {

    private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)

    private var dateDialog: DateDialog? = null
    private var startTimeCallback: DateDialog.Callback? = null
    private var endTimeCallback: DateDialog.Callback? = null
    private var myApp: MyApp? = null
    private var result: Intent? = null
    private var startTime = getCurrentTime()
    private var endTime = getCurrentTime()
    private var isProcessed = true
    private var isDenoise = false
    private var isVacuate = false
    private var isMapmatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.track_query_options_title)
        setOptionsButtonInVisible()
        init()
        myApp = application as MyApp
    }

    private fun init() {
        result = Intent()
        val startTimeBuilder = StringBuilder()
        startTimeBuilder.append(resources.getString(R.string.start_time))
        startTimeBuilder.append(simpleDateFormat.format(System.currentTimeMillis()))
        start_time.text = startTimeBuilder.toString()
        val endTimeBuilder = StringBuilder()
        endTimeBuilder.append(resources.getString(R.string.end_time))
        endTimeBuilder.append(simpleDateFormat.format(System.currentTimeMillis()))
        end_time.text = endTimeBuilder.toString()
        processed.setOnCheckedChangeListener(this)
        denoise.setOnCheckedChangeListener(this)
        vacuate.setOnCheckedChangeListener(this)
        mapmatch.setOnCheckedChangeListener(this)

        start_time.setOnClickListener {
            if (null == startTimeCallback) {
                startTimeCallback = object : DateDialog.Callback {
                    override fun onDateCallback(timeStamp: Long) {
                        startTime = timeStamp
                        val startTimeBuilder = StringBuilder()
                        startTimeBuilder.append(resources.getString(R.string.start_time))
                        startTimeBuilder.append(simpleDateFormat.format(timeStamp * 1000))
                        start_time.text = startTimeBuilder.toString()
                    }
                }
            }
            if (null == dateDialog) {
                dateDialog = DateDialog(this, startTimeCallback)
            } else {
                dateDialog?.setCallback(startTimeCallback)
            }
            dateDialog?.show()
        }

        end_time.setOnClickListener {
            if (null == endTimeCallback) {
                endTimeCallback = object : DateDialog.Callback {
                    override fun onDateCallback(timeStamp: Long) {
                        endTime = timeStamp
                        val endTimeBuilder = StringBuilder()
                        endTimeBuilder.append(resources.getString(R.string.end_time))
                        endTimeBuilder.append(simpleDateFormat.format(timeStamp * 1000))
                        end_time.text = endTimeBuilder.toString()
                    }
                }
            }
            if (null == dateDialog) {
                dateDialog = DateDialog(this, endTimeCallback)
            } else {
                dateDialog?.setCallback(endTimeCallback)
            }
            dateDialog?.show()
        }
    }

    fun onCancel(v: View?) {
        super.onBackPressed()
    }

    fun onFinish(v: View?) {
        result!!.putExtra("startTime", startTime)
        result!!.putExtra("endTime", endTime)
        result!!.putExtra("processed", isProcessed)
        result!!.putExtra("denoise", isDenoise)
        result!!.putExtra("vacuate", isVacuate)
        result!!.putExtra("mapmatch", isMapmatch)

        val radiusStr = radius_threshold.text.toString()
        if (!TextUtils.isEmpty(radiusStr)) {
            try {
                result!!.putExtra("radius", radiusStr.toInt())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        setResult(Constants.RESULT_CODE, result)
        super.onBackPressed()
    }

    override fun onCheckedChanged(
        compoundButton: CompoundButton,
        isChecked: Boolean
    ) {
        when (compoundButton.id) {
            R.id.processed -> isProcessed = isChecked
            R.id.denoise -> isDenoise = isChecked
            R.id.vacuate -> isVacuate = isChecked
            R.id.mapmatch -> isMapmatch = isChecked
            else -> {
            }
        }
    }

    override fun getContentViewId(): Int {
        return R.layout.activity_trackquery_options
    }
}
