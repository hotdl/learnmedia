package com.hotdldl.session004

import android.hardware.Camera
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * author : huangdongliang
 * time   : 2022/8/25
 * desc   :
 */
object CameraManager {
    const val MEDIA_TYPE_IMAGE = 1
    const val MEDIA_TYPE_VIDEO = 2

    var camera: Camera? = null
        private set

    /**
     * 拍摄回调，保存相片
     */
    private val pictureCallback = Camera.PictureCallback { data, _ ->
        val pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
            "create picture file error".eLog()
            return@PictureCallback
        }

        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
        } catch (e: Exception) {
            "save picture error: ${e.message}".eLog()
        }

        // 重新开启预览（takePicture过程中底层会调用stopPreview）
        camera?.startPreview()
    }

    /**
     * 打开相机
     */
    fun openCamera() {
        "openCamera".dLog()
        camera = camera ?: try {
            Camera.open()
        } catch (e: Exception) {
            "Camera open error: ${e.message}".eLog()
            null
        }
        camera?.parameters.toString().dLog()
    }

    /**
     * 释放相机
     */
    fun releaseCamera() {
        "releaseCamera".dLog()
        camera?.release()
        camera = null
    }

    /**
     * 拍摄相片
     */
    fun takePhoto() {
        camera?.takePicture(null, null, pictureCallback)
    }

    private fun getOutputMediaFile(type: Int = 0): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "session004"
        )
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    "failed to create directory".eLog()
                    return null
                }
            }
        }

        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}/IMG_$timeStamp.jpg")
            }
            MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}/VID_$timeStamp.mp4")
            }
            else -> null
        }
    }
}