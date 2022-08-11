package com.hotdldl.session003

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var audioRecordController: AudioRecordController? = null
    private var audioTrackController: AudioTrackController? = null

    private var launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        playAudio(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        XXPermissions.with(this)
            .permission(Permission.RECORD_AUDIO) // 录音权限
            .permission(Permission.MANAGE_EXTERNAL_STORAGE) // 写文件权限
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        init()
                    } else {
                        Toast.makeText(this@MainActivity, "请开启权限", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) {
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    }
                }
            })

        findViewById<Button>(R.id.btn_start_record).setOnClickListener {
            if (audioRecordController == null) {
                audioRecordController =
                    AudioRecordController(WeakReference(this), object : AudioRecordController.Callback {
                        override fun onStart() {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "录音开始", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onStop() {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "录音结束", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
            }
            if (audioRecordController!!.isRecording()) return@setOnClickListener
            audioRecordController!!.startRecord(findViewById<CheckBox>(R.id.cb_save_wav).isChecked)
        }
        findViewById<Button>(R.id.btn_stop_record).setOnClickListener {
            audioRecordController?.stopRecord()
        }

        findViewById<Button>(R.id.btn_play).setOnClickListener {
            if (audioTrackController != null && audioTrackController!!.isInited()) {
                playAudio(null)
            } else {
                launcher.launch(arrayOf("*/*"))
            }
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            audioTrackController?.stop()
        }
        findViewById<Button>(R.id.btn_destroy).setOnClickListener {
            audioTrackController?.dispose()
            audioTrackController = null
        }
    }

    private fun init() {
    }

    private fun playAudio(uri: Uri?) {
        if (audioTrackController == null) {
            audioTrackController = AudioTrackController(WeakReference(this), object : AudioTrackController.Callback {
                override fun onStart() {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "播放开始", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onStop() {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "播放停止", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
        if (audioTrackController!!.isPlaying()) return
        if (!audioTrackController!!.isInited()) {
            audioTrackController?.init(uri!!)
        }
        audioTrackController?.play()
    }
}