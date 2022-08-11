package com.hotdldl.session003

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder

class AudioConstants {
    companion object {
        // 音频来源
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC

        // 采样率，44100Hz是目前唯一能保证在所有设备上都正常工作的采样率
        const val SAMPLE_RATE_IN_HZ = 44100

        // 录音声道（单通道/立体声）
        const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO

        // 播放声道（单通道/立体声）
        const val CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO

        // 采样位数
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // AudioRecord实例支持的最小Buffer尺寸
        val AUDIO_RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_IN_CONFIG, AUDIO_FORMAT)
        val AUDIO_TRACK_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_OUT_CONFIG, AUDIO_FORMAT)
    }
}