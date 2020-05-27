package com.simplation.baidumaptrace.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TimePicker
import com.simplation.baidumaptrace.R
import kotlinx.android.synthetic.main.dialog_date.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/27 9:20
 * @描述:
 * @更新:
 */
class DateDialog(context: Context, callback: Callback?) :
    Dialog(context, android.R.style.Theme_Holo_Light_Dialog), DatePicker.OnDateChangedListener,
    TimePicker.OnTimeChangedListener {

    private var callback: Callback? = null
    private var dateTime: String
    private var year: Int
    private var month: Int
    private var day: Int
    private var hour: Int
    private var minute: Int

    companion object {
        private lateinit var calendar: Calendar
        private lateinit var simpleDateFormat: SimpleDateFormat

        private lateinit var datePicker: DatePicker
        private lateinit var timePicker: TimePicker
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_date)
        datePicker = findViewById(R.id.date_picker)
        timePicker = findViewById(R.id.time_picker)
        ViewUtil.resizePicker(datePicker)
        ViewUtil.resizePicker(timePicker)

        btn_cancel.setOnClickListener { dismiss() }
        btn_sure.setOnClickListener {
            if (null != callback) {
                val timeStamp = calendar.time.time / 1000
                callback!!.onDateCallback(timeStamp)
            }
            dismiss()
        }
        datePicker.init(year, month, day, this)
        timePicker.setOnTimeChangedListener(this)
        timePicker.setIs24HourView(true)
    }

    override fun onDateChanged(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        this.year = year
        this.month = month
        this.day = day
        updateDate()
    }

    override fun onTimeChanged(timePicker: TimePicker?, hour: Int, minute: Int) {
        this.hour = hour
        this.minute = minute
        updateDate()
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    private fun updateDate() {
        calendar[year, month, day, hour] = minute
        dateTime = simpleDateFormat.format(calendar.time)
        this.setTitle(dateTime)
    }

    interface Callback {
        fun onDateCallback(timeStamp: Long)
    }

    /**
     * 调用的父activity
     */
    init {
        calendar = Calendar.getInstance()
        simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
        dateTime = simpleDateFormat.format(System.currentTimeMillis())
        this.setTitle(dateTime)
        this.callback = callback
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH)
        day = calendar.get(Calendar.DAY_OF_MONTH)
        hour = calendar.get(Calendar.HOUR_OF_DAY)
        minute = calendar.get(Calendar.MINUTE)
    }
}

