package com.hotdldl.session001.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomView : View {

    // 设置画笔
    private var paint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private var bitmap: Bitmap?

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        // 获取图片
        bitmap = BitmapFactory.decodeStream(context.assets.open("avatar.jpg"))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        bitmap?.let {
            // 在画布上画位图
            canvas?.drawBitmap(it, 0f, 0f, paint)
        }
    }
}