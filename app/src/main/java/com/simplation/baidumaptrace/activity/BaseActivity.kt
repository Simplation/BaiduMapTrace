package com.simplation.baidumaptrace.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.simplation.baidumaptrace.R

/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 14:43
 * @描述:
 * @更新:
 */
abstract class BaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentViewId())
    }

    /**
     * 获取布局文件 ID
     */
    protected abstract fun getContentViewId(): Int

    /**
     * 设置 Activity 标题
     */
    override fun setTitle(resId: Int) {
        val layout = findViewById<LinearLayout>(R.id.layout_top)
        val textView = layout.findViewById<View>(R.id.tv_activity_title) as TextView
        textView.setText(resId)
    }

    /**
     * 设置点击监听器
     */
    open fun setOnClickListener(listener: View.OnClickListener?) {
        val layout = findViewById<LinearLayout>(R.id.layout_top)
        val optionsButton = layout.findViewById<View>(R.id.btn_activity_options) as LinearLayout
        optionsButton.setOnClickListener(listener)
    }

    /**
     * 不显示设置按钮
     */
    open fun setOptionsButtonInVisible() {
        val layout = findViewById<LinearLayout>(R.id.layout_top)
        val optionsButton = layout.findViewById<View>(R.id.btn_activity_options) as LinearLayout
        optionsButton.visibility = View.INVISIBLE
    }

    /**
     * 回退事件
     */
    open fun onBack(v: View?) {
        super.onBackPressed()
    }

}