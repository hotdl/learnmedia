package com.hotdldl.session004

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

/**
 * author : huangdongliang
 * time   : 2022/8/25
 * desc   : 相机预览
 */
class CameraPreview(context: Context, private var mCamera: Camera) :
    SurfaceView(context), SurfaceHolder.Callback {

    private val mHolder: SurfaceHolder = holder.apply {
        addCallback(this@CameraPreview)
    }

    fun resetCamera(camera: Camera) {
        mCamera = camera
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        "surfaceCreated".dLog()
        mCamera.setDisplayOrientation(90)
//        mCamera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        mCamera.parameters.setPreviewSize(1200, 1600)
        mCamera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        startPreview(mHolder)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        "surfaceChanged".dLog()
        if (mHolder.surface == null) return
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {

        }

        "surface ${width}x$height".dLog()
        for (size in mCamera.parameters.getSupportedPreviewSizes()) {
            "${size.width}x${size.height}".dLog()
        }

        startPreview(mHolder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // do nothing
        "surfaceDestroyed".dLog()
    }

    private fun startPreview(holder: SurfaceHolder) {
        mCamera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                "Camera preview error: ${e.message}".eLog()
            }
        }
    }
}