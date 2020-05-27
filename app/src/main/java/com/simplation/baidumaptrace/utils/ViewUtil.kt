package com.simplation.baidumaptrace.utils

import android.app.Activity
import android.content.Intent
import android.text.Html
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.simplation.baidumaptrace.R


/**
 * @作者: W ◕‿-｡ Z
 * @日期: 2020/5/26 15:27
 * @描述:
 * @更新:
 */
class ViewUtil {

    private var mToast: Toast? = null

    private var mTextView: TextView? = null

    companion object {
        fun startActivityForResult(
            fromActivity: Activity,
            toClass: Class<*>?,
            requestCode: Int
        ) {
            val intent = Intent(fromActivity, toClass)
            fromActivity.startActivityForResult(intent, requestCode)
            fromActivity.overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left)
        }

        /**
         * 调整Picker布局
         *
         * @param frameLayout layout
         */
        @JvmStatic
        fun resizePicker(frameLayout: FrameLayout?) {
            val numberPickers = findNumberPicker(frameLayout)
            for (numberPicker in numberPickers) {
                resizeNumberPicker(numberPicker)
            }
        }

        /**
         * 获取ViewGroup中的NumberPicker组件
         *
         * @param viewGroup view
         */
        fun findNumberPicker(viewGroup: ViewGroup?): List<NumberPicker> {
            val numberPickers: MutableList<NumberPicker> = ArrayList()
            var child: View
            if (null != viewGroup) {
                for (i in 0 until viewGroup.childCount) {
                    child = viewGroup.getChildAt(i)
                    if (child is NumberPicker) {
                        numberPickers.add(child as NumberPicker)
                    } else if (child is LinearLayout) {
                        val result =
                            findNumberPicker(child as ViewGroup)
                        if (result.isNotEmpty()) {
                            return result
                        }
                    }
                }
            }
            return numberPickers
        }

        /**
         * 调整NumberPicker大小
         *
         * @param numberPicker picker
         */
        private fun resizeNumberPicker(numberPicker: NumberPicker) {
            val params =
                LinearLayout.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(15, 0, 15, 0)
            numberPicker.layoutParams = params
        }

    }

    fun showToast(activity: Activity, message: String?) {
        val strBuilder =
            StringBuilder("<font face='" + activity.getString(R.string.font_type) + "'>")
        strBuilder.append(message).append("</font>")
        val toastRoot: View = activity.layoutInflater.inflate(R.layout.layout_toast, null)
        if (null == mToast || null == mTextView) {
            mToast = Toast(activity)
            mToast!!.view = toastRoot
            mToast!!.duration = Toast.LENGTH_SHORT
            mTextView = toastRoot.findViewById(R.id.tv_toast_info)
            mTextView!!.text = Html.fromHtml(strBuilder.toString())
        } else {
            mTextView!!.text = Html.fromHtml(strBuilder.toString())
        }
        mToast!!.setGravity(
            Gravity.BOTTOM,
            0,
            activity.resources.displayMetrics.heightPixels / 5
        )
        mToast!!.show()
    }

}