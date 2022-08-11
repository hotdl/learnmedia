package com.hotdldl.session003

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecordController {
    private var context: WeakReference<Context>
    private var callback: Callback? = null
    private var audioRecord: AudioRecord? = null
    var recordThread: Thread? = null
    var recordSignal: AtomicBoolean = AtomicBoolean(false)
    var pcmFile: File? = null
    var saveWav: Boolean = true

    constructor(context: WeakReference<Context>) {
        this.context = context
    }

    constructor(context: WeakReference<Context>, callback: Callback) : this(context) {
        this.callback = callback
    }

    fun isRecording(): Boolean {
        return recordSignal.get()
    }

    fun startRecord() {
        startRecord(true)
    }

    fun startRecord(saveWav: Boolean) {
        if (recordSignal.get()) return
        this.saveWav = saveWav
        recordSignal.set(true)
        callback?.onStart()
        if (audioRecord == null) {
            createAudioRecord()
        }
        genPcmFile()
        // 开始录音
        audioRecord!!.startRecording()
        recordThread?.interrupt()
        // 开启录音线程
        recordThread = Thread(RecordingRunnable(), "Record Thread")
        recordThread?.start()
    }

    fun stopRecord() {
        recordSignal.set(false)
        destroyAudioRecord()
    }

    private fun createAudioRecord() {
        if (audioRecord == null) {
            // 创建录音实例
            audioRecord = AudioRecord(
                AudioConstants.AUDIO_SOURCE,
                AudioConstants.SAMPLE_RATE_IN_HZ,
                AudioConstants.CHANNEL_IN_CONFIG,
                AudioConstants.AUDIO_FORMAT,
                AudioConstants.AUDIO_RECORD_BUFFER_SIZE
            )
        }
    }

    private fun destroyAudioRecord() {
        audioRecord?.let {
            try {
                // 停止录音
                it.stop()
                // 释放占用资源
                it.release()
            } catch (e: Exception) {

            }
        }
        audioRecord = null
    }

    private fun genPcmFile() {
        // 录音文件存储区域
        val storeArea = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        // 录音文件存储目录
        val pcmDir = File("${storeArea.absolutePath}/${context.get()?.getString(R.string.app_name)}")
        if (!pcmDir.exists()) {
            pcmDir.mkdirs()
        }
        // 录音文件
        pcmFile = File("${pcmDir.absolutePath}/recording_${System.currentTimeMillis()}.pcm")
    }

    inner class RecordingRunnable : Runnable {
        override fun run() {
            var outStream: FileOutputStream? = null
            try {
                outStream = FileOutputStream(pcmFile)
                val buffer = ByteBuffer.allocateDirect(AudioConstants.AUDIO_RECORD_BUFFER_SIZE)
                while (recordSignal.get()) {
                    // 读取录音数据
                    val result = audioRecord!!.read(buffer, AudioConstants.AUDIO_RECORD_BUFFER_SIZE)
                    if (result < 0) {
                        throw RuntimeException("Reading of audio buffer failed: ${getBufferReadFailureReason(result)}")
                    }
                    // 录音数据写入文件
                    outStream.write(buffer.array(), 0, result)
                    outStream.flush()
                    buffer.clear()
                }
            } catch (exception: IOException) {
                throw RuntimeException("Writing of recorded audio failed", exception)
            } finally {
                outStream?.close()
            }
            if (saveWav) {
                Thread(SaveWavRunnable(), "SaveWav Thread").start()
            }
            stopRecord()
            callback?.onStop()
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

    inner class SaveWavRunnable : Runnable {
        override fun run() {
            pcmFile?.let {
                val wavFile = File("${it.parentFile.absolutePath}/${it.nameWithoutExtension}.wav")
                towavfile(it, wavFile)
            }
        }

        private fun towavfile(pcmFile: File, wavFile: File) {
            var fis: FileInputStream? = null
            var fos: FileOutputStream? = null
            try {
                fis = FileInputStream(pcmFile)
                fos = FileOutputStream(wavFile)
                fos.write(
                    generateWavHeader(
                        fis.channel.size(),
                        AudioConstants.SAMPLE_RATE_IN_HZ.toLong(),
                        when (AudioConstants.CHANNEL_IN_CONFIG) {
                            AudioFormat.CHANNEL_IN_STEREO -> 2
                            else -> 1
                        }
                    )
                )
                var read: Int
                val temp = ByteArray(1024)
                while (fis.read(temp).also { read = it } > 0) {
                    fos.write(temp, 0, read)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fis?.close()
                fos?.close()
            }
        }

        private fun generateWavHeader(totalAudioLen: Long, longSampleRate: Long, channels: Long): ByteArray? {
            val totalDataLen = totalAudioLen + 36
            val byteRate = longSampleRate * channels * 16 / 8
            val header = ByteArray(44)
            // RIFF
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte() //数据大小
            header[5] = (totalDataLen shr 8 and 0xff).toByte()
            header[6] = (totalDataLen shr 16 and 0xff).toByte()
            header[7] = (totalDataLen shr 24 and 0xff).toByte()
            //WAVE
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            //FMT Chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            //数据大小
            header[16] = 16 // 4 bytes: size of 'fmt ' chunk
            header[17] = 0
            header[18] = 0
            header[19] = 0
            //编码方式 10H为PCM编码格式
            header[20] = 1 // format = 1
            header[21] = 0
            //通道数
            header[22] = channels.toByte()
            header[23] = 0
            //采样率，每个通道的播放速度
            header[24] = (longSampleRate and 0xff).toByte()
            header[25] = (longSampleRate shr 8 and 0xff).toByte()
            header[26] = (longSampleRate shr 16 and 0xff).toByte()
            header[27] = (longSampleRate shr 24 and 0xff).toByte()
            //音频数据传送速率,采样率*通道数*采样深度/8
            header[28] = (byteRate and 0xff).toByte()
            header[29] = (byteRate shr 8 and 0xff).toByte()
            header[30] = (byteRate shr 16 and 0xff).toByte()
            header[31] = (byteRate shr 24 and 0xff).toByte()
            // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
            header[32] = (2 * channels).toByte()
            header[33] = 0
            //每个样本的数据位数
            header[34] = 16
            header[35] = 0
            //Data chunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = (totalAudioLen shr 8 and 0xff).toByte()
            header[42] = (totalAudioLen shr 16 and 0xff).toByte()
            header[43] = (totalAudioLen shr 24 and 0xff).toByte()
            return header
        }
    }

    interface Callback {
        fun onStart()
        fun onStop()
    }
}