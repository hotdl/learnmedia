# 安卓音视频开发指南002 -- AudioRecord录音



## 前言

Android SDK 提供了两套音频采集的API，分别是：MediaRecorder 和 AudioRecord，前者是一个更加上层一点的API，它可以直接把手机麦克风录入的音频数据进行编码压缩（如AMR、MP3等）并存成文件，而后者则更接近底层，能够更加自由灵活地控制，可以得到原始的一帧帧PCM音频数据。如果想简单地做一个录音机，录制成音频文件，则推荐使用 MediaRecorder，而如果需要对音频做进一步的算法处理、或者采用第三方的编码库进行压缩、以及网络传输等应用，则建议使用 AudioRecord，其实 MediaRecorder 底层也是调用了 AudioRecord 与 Android Framework 层的 AudioFlinger 进行交互的。



## PCM

PCM是在由模拟信号向数字信号转化的一种常用的编码格式，称为脉冲编码调制，PCM将模拟信号按照一定的间距划分为多段，然后通过二进制去量化每一个间距的强度。

PCM 有三个重要的参数，它们是：**声道数**、**采样位数**和**采样频率**

- 采样频率：指每秒钟取得声音样本的次数。采样频率越高，声音的质量也就越好，声音的还原也就越真实，但同时它占的资源比较多。由于人耳的分辨率很有限，太高的频率并不能分辨出来。在16位声卡中有22KHz、44KHz等几级，其中，22KHz相当于普通FM广播的音质，44KHz已相当于CD音质了，目前的常用采样频率都不超过48KHz。亦即AudioRecord中的sampleRateInHz参数，在安卓设备上一般使用44KHz。

- 采样位数：即采样值或取样值（就是将采样样本幅度量化）。它是用来衡量声音波动变化的一个参数，也可以说是声卡的分辨率。它的数值越大，分辨率也就越高，所发出声音的能力越强。亦即AudioRecord中的channelConfig参数，在安卓设备上一般使用16位。
- 声道数：很好理解，有单声道和立体声之分，单声道的声音只能使用一个喇叭发声（有的也处理成两个喇叭输出同一个声道的声音），立体声的PCM 可以使两个喇叭都发声（一般左右声道有分工） ，更能感受到空间效果。亦即AndioRecord中的audioFormat参数。



## 录音流程

AudioRecord实现录音的流程为：

1. 构造一个AudioRecord对象，其中需要的最小录音缓存buffer大小可以通过*getMinBufferSize*方法得到。如果buffer容量过小，将导致对象构造的失败。

   ```kotlin
   val SAMPLE_RATE_IN_HZ = 44100 // 采样率
   val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // 声道（单通道/立体声）
   val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT // 采样位数
   var minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
   var audioRecord = AudioRecord(
                   AUDIO_SOURCE,
                   SAMPLE_RATE_IN_HZ,
                   CHANNEL_CONFIG,
                   AUDIO_FORMAT,
                   MIN_BUFFER_SIZE
   )
   ```

2. 创建一个用于读取录音数据的buffer和一个用于存储录音数据的数据流，并开始录音

   ```kotlin
   var pcmFilePath = "" // 自定义pcm文件路径
   var pcmFile = File(pcmFilePath)
   var outStream: FileOutputStream? = null
   try {
     outStream = FileOutputStream(pcmFile)
     val buffer = ByteBuffer.allocateDirect(minBufferSize) // 用于读取录音数据的buffer
     while (recordSignal.get()) {
         // 读取录音数据
         val result = audioRecord!!.read(buffer, minBufferSize)
         // 读取结果小于0表示出错
         if (result < 0) {
             throw RuntimeException("Reading of audio buffer failed: ${getBufferReadFailureReason(result)}")
         }
         // 录音数据写入文件
         outStream.write(buffer.array(), 0, buffer.array().size)
         outStream.flush()
         buffer.clear()
     }
   } finally {
     outStream?.close()
   }
   
   ```

3. 停止录音

   ```kotlin
   recordSignal.set(false)
   audioRecord?.let {
               // 停止录音
               it.stop()
               // 释放占用资源
               it.release()
           }
   audioRecord = null
   ```



## 结语

录音功能需要录音权限，同时如果文件要写到外部存储，还需要存储读写权限。另外，AudioRecord录音最后生成的文件是PCM格式的，这是最原始的录音数据，不能通过常规播放器直接播放。如果需要播放，可以通过将pcm格式转成wav格式的文件，又或者通过安卓提供的AudioTrack类来播放。后续会继续介绍录音的播放，谢谢关注。

还有有关共享音频输入的问题，可以参考https://developer.android.com/guide/topics/media/sharing-audio-input



## 代码

https://github.com/hotdl/learnmedia/tree/main/session002



## 参考

https://developer.android.com/reference/android/media/AudioRecord

https://developer.android.com/guide/topics/media/sharing-audio-input