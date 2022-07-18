package com.zain.passportmrz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class BoxedWordView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mBoxes = mutableListOf<Rect>()
    private val mPaint = Paint().apply {
        strokeWidth = 2f
        color = context.resources.getColor(android.R.color.holo_red_light)
        style = Paint.Style.STROKE
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        for (box in mBoxes) {
            drawBox(canvas, box)
        }
    }

    private fun drawBox(canvas: Canvas, box: Rect) {
        canvas.drawRect(box, mPaint)
    }

    fun addBox(box: Rect) {
        mBoxes.add(box)
    }

    fun clearBoxes() {
        mBoxes.clear()
    }
}