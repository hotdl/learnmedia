package com.hotdldl.session001

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

class SurfaceViewFragment : Fragment() {

    private lateinit var surfaceView: SurfaceView

    // 设置画笔
    var paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_surface_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        surfaceView = view.findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // 获取图片
                val bitmap = BitmapFactory.decodeStream(context?.assets?.open("avatar.jpg"))
                bitmap?.let {
                    // 锁定画布
                    val canvas = holder.lockCanvas()
                    // 在画布上画位图
                    canvas.drawBitmap(it, 0f, 0f, paint)
                    // 解锁画布
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = SurfaceViewFragment()
    }
}