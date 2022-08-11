

# 安卓音视频开发指南003 -- AudioTrack播放PCM



## 前言

上一节介绍了AudioRecord的录音，但是AudioRecord录音生成出来的是原始音频数据，无法直接使用常规播放器播放的。要播放这些数据，可以用安卓提供的AudioTrack类来播放，同时AudioTrack类的参数要跟AudioRecord的参数要保持一致。除此之外，还可以通过给这些原始音频数据添加WAV文件头，让它成为真正意义上的音频文件，这样常规的播放器就可以直接播放该文件了。



## AudioTrack播放PCM

AudioTrack有两种数据加载模式（MODE_STREAM和MODE_STATIC），对应的是数据加载模式和音频流类型， 对应着两种完全不同的使用场景。

- MODE_STREAM：在这种模式下，通过write一次次把音频数据写到AudioTrack中。这和平时通过write系统调用往文件中写数据类似，但这种工作方式每次都需要把数据从用户提供的Buffer中拷贝到AudioTrack内部的Buffer中，这在一定程度上会使引入延时。为解决这一问题，AudioTrack就引入了第二种模式。
- MODE_STATIC：这种模式下，在play之前只需要把所有数据通过一次write调用传递到AudioTrack中的内部缓冲区，后续就不必再传递数据了。这种模式适用于像铃声这种内存占用量较小，延时要求较高的文件。但它也有一个缺点，就是一次write的数据不能太多，否则系统无法分配足够的内存来存储全部数据。

### AudioTrack的创建

```kotlin
val audioAttributes = AudioAttributes.Builder()
    // 除非您的应用是闹钟，否则您应播放使用情况为 AudioAttributes.USAGE_MEDIA 的音频。
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()
val audioFormat = AudioFormat.Builder()
    // 采样率，要保持跟AudioRecord一致
    .setSampleRate(AudioConstants.SAMPLE_RATE_IN_HZ)
    // 采样位数，要保持跟AudioRecord一致
    .setEncoding(AudioConstants.AUDIO_FORMAT)
    // 声道，要保持跟AudioRecord对应（注意channel有in/out之分）
    .setChannelMask(AudioConstants.CHANNEL_OUT_CONFIG)
    .build()
audioTrack = AudioTrack(
    audioAttributes,
    audioFormat,
    bufferSize, // static模式下bufferSize为音频数据大小，stream模式下为不能小于AudioTrack.getMinBufferSize
    mode, // MODE_STATIC或者MODE_STREAM
    AudioManager.AUDIO_SESSION_ID_GENERATE // 这里简单自动生成sessionId即可
)
```

### MODE_STATIC模式

static模式下须先调用write写数据，然后再调用play。

```kotlin
audioTrack?.let {
    // audioData为预先加载的音频数据
    it.write(audioData, 0, audioData.size)
    it.play()
}
```

### MODE_STREAM模式

```kotlin
val AUDIO_TRACK_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_OUT_CONFIG, AUDIO_FORMAT)
val byteArray = ByteArray(AUDIO_TRACK_BUFFER_SIZE)
audioTrack?.let {
    it.play()
    while (playSignal.get()) {
        val readCount = inputStream!!.read(byteArray)
        if (readCount <= 0) break
        it.write(byteArray, 0, readCount)
    }
}
```

### 播放结束时机

在stream模式下，由于是我们主动向audioTrack喂数据的，所以我们可以很清楚的知道播放结束的时机。但是在static模式下，由于是预先给audioTrack喂了全部视频数据再开始播放的，音频播放结束的时机不能直接得到。

AudioTrack有一个setPlaybackPositionUpdateListener方法，该方法接收一个接口参数，该接口参数有一个onMarkerReached的方法，即是播放到了这个marker的时候会调用到这个方法，然后AudioTrack有一个notificationMarkerPosition的属性，是用于设置这个marker的，最多只能设置一个，这个marker指的是Frame的位置。Frame是一个单位，用来描述数据量的多少。1单位的Frame等于1个采样点的字节数×声道数（比如PCM16，双声道的1个Frame等于2×2=4字节）。1个采样点只针对一个声道，而实际上可能会有一或多个声道。由于不能用一个独立的单位来表示全部声道一次采样的数据量，也就引出了Frame的概念。Frame的大小，就是一个采样点的字节数×声道数。另外，在目前的声卡驱动程序中，其内部缓冲区也是采用Frame作为单位来分配和管理的。

```kotlin
audioTrack?.let {
    it.write(audioData, 0, audioData.size)
    val frameSize = 16 / 8 // pcm16，单通道
    it.notificationMarkerPosition = audioData.size / frameSize
    it.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack?) {
            Log.d("TAG", "播放结束")
        }

        override fun onPeriodicNotification(track: AudioTrack?) {
        }
    })
    it.play()
}
```



## PCM转WAV

PCM只有原始的音频数据，但是却不包含录制的配置信息，所以是不能直接播放的。而WAV是一种音频格式，在pcm数据中添加了一个文件头。文件头中包含了播放该pcm数据所需的信息，于是常规播放器就能直接播放了。

| 起始地址 | 占用空间 | 本地址数字的含义                                             |
| :------- | :------- | :----------------------------------------------------------- |
| 00H      | 4byte    | RIFF，资源交换文件标志。                                     |
| 04H      | 4byte    | 从下一个地址开始到文件尾的总字节数。高位字节在后面，这里就是001437ECH，换成十进制是1325036byte，算上这之前的8byte就正好1325044byte了。 |
| 08H      | 4byte    | WAVE，代表wav文件格式。                                      |
| 0CH      | 4byte    | FMT ，波形格式标志                                           |
| 10H      | 4byte    | 00000010H，16PCM，我的理解是用16bit的数据表示一个量化结果。  |
| 14H      | 2byte    | 为1时表示线性PCM编码，大于1时表示有压缩的编码。这里是0001H。 |
| 16H      | 2byte    | 1为单声道，2为双声道，这里是0001H。                          |
| 18H      | 4byte    | 采样频率，这里是00002B11H，也就是11025Hz。                   |
| 1CH      | 4byte    | Byte率=`采样频率*音频通道数*每次采样得到的样本位数/8`，00005622H，也就是`22050Byte/s=11025*1*16/2` |
| 20H      | 2byte    | 块对齐=通道数*每次采样得到的样本位数/8，0002H，也就是 `2 == 1*16/8` |
| 22H      | 2byte    | 样本数据位数，0010H即16，一个量化样本占2byte。               |
| 24H      | 4byte    | data，一个标志而已。                                         |
| 28H      | 4byte    | Wav文件实际音频数据所占的大小，这里是001437C8H即1325000，再加上2CH就正好是1325044，整个文件的大小。 |
| 2CH      | 不定     | 量化数据                                                     |

将pcm文件转化成wav文件，只需添加一个文件头即可。

文件头生成代码如下：

```kotlin
fun generateWavHeader(totalAudioLen: Long, longSampleRate: Long, channels: Long): ByteArray? {
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
```



## 结语

上一节介绍了AudioRecord的使用，这是音频的采集，这一节介绍了AudioTrack的使用以及PCM数据转Wav文件，这是音频的播放，下一节我们就可以来进行视频的采集了。当然，视频采集主要手段就是Camera了。



## 代码

https://github.com/hotdl/learnmedia/tree/main/session003



## 参考

https://developer.android.com/reference/android/media/AudioTrack

https://developer.android.com/guide/topics/media-apps/volume-and-earphones