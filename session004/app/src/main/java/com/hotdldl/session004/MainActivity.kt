package com.hotdldl.session004

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

class MainActivity : AppCompatActivity() {

    private var cameraPreview: CameraPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        XXPermissions.with(this)
            .permission(Permission.CAMERA)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(
                    permissions: MutableList<String>?,
                    all: Boolean
                ) {
                    if (all) {
                        init()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "请开启权限",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onDenied(
                    permissions: MutableList<String>,
                    never: Boolean
                ) {
                    if (never) {
                        XXPermissions.startPermissionActivity(
                            this@MainActivity,
                            permissions
                        )
                    }
                }
            })
    }

    override fun onResume() {
        "onResume".dLog()
        super.onResume()
        CameraManager.openCamera()
        CameraManager.camera?.let { cameraPreview?.resetCamera(it) }
    }

    override fun onPause() {
        "onPause".dLog()
        super.onPause()
        CameraManager.releaseCamera()
    }

    private fun init() {
        CameraManager.openCamera()
        cameraPreview = CameraManager.camera?.let {
            CameraPreview(this, it)
        }
        cameraPreview?.also {
            findViewById<FrameLayout>(R.id.camera_preview).addView(it)
        }
        findViewById<Button>(R.id.btn_capture).setOnClickListener {
            CameraManager.takePhoto()
        }
    }
}