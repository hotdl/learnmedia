package com.hotdldl.session003

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class AudioTrackController {
    private var context: WeakReference<Context>
    private var playUri: Uri? = null
    private var inputStream: InputStream? = null
    private var callback: Callback? = null

    var mode: Int = AudioTrack.MODE_STATIC
    private var audioTrack: AudioTrack? = null
    private var playSignal: AtomicBoolean = AtomicBoolean(false)

    constructor(context: WeakReference<Context>) {
        this.context = context
    }

    constructor(context: WeakReference<Context>, callback: Callback) : this(context) {
        this.callback = callback
    }

    fun init(uri: Uri) {
        playUri = uri
    }

    fun isPlaying(): Boolean {
        return playSignal.get()
    }

    fun isInited(): Boolean {
        return playUri != null
    }

    fun play() {
        if (playSignal.get()) return
        playSignal.set(true)
        if (inputStream == null) {
            inputStream = context.get()?.contentResolver?.openInputStream(playUri!!)
        }
        Thread(AudioPlayRunnable(), "AudioTrack Thread").start()
    }

    fun stop() {
        playSignal.set(false)
    }

    fun dispose() {
        stop()
        destroyAudioTrack()
        inputStream?.close()
        inputStream = null
        playUri = null
    }

    private fun createAudioTrack(mode: Int, bufferSize: Int) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(AudioConstants.SAMPLE_RATE_IN_HZ)
            .setEncoding(AudioConstants.AUDIO_FORMAT)
            .setChannelMask(AudioConstants.CHANNEL_OUT_CONFIG)
            .build()
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            mode,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun onStart() {
        callback?.onStart()
    }

    private fun onStop(end: Boolean) {
        playSignal.set(false)
        try {
            audioTrack?.stop()
        } catch (e: Exception) {

        }
        if (end) {
            inputStream?.close()
            inputStream = null
        }
        callback?.onStop()
    }

    private fun destroyAudioTrack() {
        audioTrack?.release()
        audioTrack = null
    }

    inner class AudioPlayRunnable : Runnable {
        override fun run() {
            if (mode == AudioTrack.MODE_STATIC) {
                val audioData = inputStream!!.readBytes()
                if (audioTrack == null) {
                    createAudioTrack(mode, audioData.size)
                }
                audioTrack?.let {
                    onStart()
                    it.write(audioData, 0, audioData.size)
                    it.notificationMarkerPosition = audioData.size / 2
                    it.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onMarkerReached(track: AudioTrack?) {
                            onStop(true)
                        }

                        override fun onPeriodicNotification(track: AudioTrack?) {
                        }
                    })
                    it.play()
                }
            } else {
                if (audioTrack == null) {
                    createAudioTrack(mode, AudioConstants.AUDIO_TRACK_BUFFER_SIZE)
                }
                val byteArray = ByteArray(AudioConstants.AUDIO_TRACK_BUFFER_SIZE)
                audioTrack?.let {
                    onStart()
                    it.play()
                    while (playSignal.get()) {
                        val readCount = inputStream!!.read(byteArray)
                        if (readCount <= 0) break
                        it.write(byteArray, 0, readCount)
                    }
                    onStop(playSignal.get())
                }
            }
        }
    }

    interface Callback {
        fun onStart()
        fun onStop()
    }
}