package com.zakhrafa.keyboard

import android.content.Context
import android.view.View
import android.widget.FrameLayout

internal class CroppedBackgroundFrame(context: Context) : FrameLayout(context) {
    var backgroundView: View? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val background = backgroundView
        if (background == null || background.parent !== this) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val originalVisibility = background.visibility
        background.visibility = View.GONE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        background.visibility = originalVisibility

        background.measure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
    }
}
