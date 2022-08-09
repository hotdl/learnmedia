package com.hotdldl.session002

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    companion object {
        // 音频来源
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT

        // 采样率，44100Hz是目前唯一能保证在所有设备上都正常工作的采样率
        private const val SAMPLE_RATE_IN_HZ = 44100

        // 声道（单通道/立体声）
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        // 采样位数
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // AudioRecord实例支持的最小Buffer尺寸
        private val MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
    }

    private var tvLog: TextView? = null

    private var audioRecord: AudioRecord? = null
    var recordThread: Thread? = null
    var recordSignal: AtomicBoolean = AtomicBoolean(true)
    var pcmFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvLog = findViewById(R.id.tv_log)

        findViewById<Button>(R.id.btn_start)?.setOnClickListener {
            XXPermissions.with(this)
                .permission(Permission.RECORD_AUDIO) // 录音权限
                .permission(Permission.MANAGE_EXTERNAL_STORAGE) // 写文件权限
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) {
                            if (initAudioRecord()) {
                                genPcmFile()
                                tvLog?.text = "正在录音。。。"
                                // 开始录音
                                audioRecord!!.startRecording()
                                recordThread?.interrupt()
                                // 开启录音线程
                                recordThread = Thread(RecordingRunnable(), "Record Thread")
                                recordThread?.start()
                            }
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
        }

        findViewById<Button>(R.id.btn_stop)?.setOnClickListener {
            if (audioRecord == null) return@setOnClickListener
            pcmFile?.let {
                tvLog?.text = "录音结束，文件：${it.absolutePath}"
            }
            recordSignal.set(false)
            destroyAudioRecord()
            recordThread?.interrupt()
            recordThread = null
            pcmFile = null
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    private fun initAudioRecord(): Boolean {
        if (audioRecord == null) {
            // 创建录音实例
            audioRecord = AudioRecord(
                AUDIO_SOURCE,
                SAMPLE_RATE_IN_HZ,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                MIN_BUFFER_SIZE
            )
        }
        return audioRecord!!.state == AudioRecord.STATE_INITIALIZED
    }

    private fun genPcmFile() {
        // 录音文件存储区域
        val storeArea = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        // 录音文件存储目录
        val pcmDir = File("${storeArea.absolutePath}/session002")
        if (!pcmDir.exists()) {
            pcmDir.mkdirs()
        }
        // 录音文件
        pcmFile = File("${pcmDir.absolutePath}/recording_${System.currentTimeMillis()}.pcm")
    }

    private fun destroyAudioRecord() {
        audioRecord?.let {
            // 停止录音
            it.stop()
            // 释放占用资源
            it.release()
        }
        audioRecord = null
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyAudioRecord()
    }

    inner class RecordingRunnable : Runnable {
        override fun run() {
            var outStream: FileOutputStream? = null
            try {
                outStream = FileOutputStream(pcmFile)
                val buffer = ByteBuffer.allocateDirect(MIN_BUFFER_SIZE)
                while (recordSignal.get()) {
                    // 读取录音数据
                    val result = audioRecord!!.read(buffer, MIN_BUFFER_SIZE)
                    if (result < 0) {
                        throw RuntimeException("Reading of audio buffer failed: ${getBufferReadFailureReason(result)}")
                    }
                    // 录音数据写入文件
                    outStream.write(buffer.array(), 0, buffer.array().size)
                    outStream.flush()
                    buffer.clear()
                }
            } catch (exception: IOException) {
                throw RuntimeException("Writing of recorded audio failed", exception)
            } finally {
                outStream?.close()
            }
        }

        private fun getBufferReadFailureReason(errorCode: Int): String {
            return when (errorCode) {
                AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
                AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
                AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
                else -> "ERROR"
            }
        }
    }
}