

# 安卓音视频开发指南004 -- Camera1使用



## 前言

安卓音视频开发中，相机的使用是很重要的一部分，无论拍摄照片还是录制视频，都离不开相机的使用。同时，相机的开发也是很繁复的一个事，这里会分成很多part来写。这一篇就先从简单Camera1 API的使用来说起吧，虽然Camera1已经被遗弃了，但还是可以说一下。



## Camera1 相机开发的关键类

### SurfaceView

**用于绘制相机预览图像的类，展现实时的预览图像。**

普通的View以及派生类都是共享同一个Surface的，所有的绘制都必须在UI线程中进行。 Surface是指向屏幕窗口原始图像缓冲区（raw buffer）的一个句柄，通过它可以获得这块屏幕上对应的canvas，进而完成在屏幕上绘制View的工作。

SurfaceView是一种比较特殊的View，它并不与其他普通View共享Surface，而是在内部持有了一个独立的Surface，SurfaceView负责管理这个Surface的格式、尺寸以及显示位置。由于UI线程还要同时处理其他交互逻辑，因此对View的更新速度和帧率无法保证，而SurfaceView由于持有一个独立的Surface，因而可以在独立的线程中进行绘制，因此可以提供更高的帧率。自定义相机的预览图像由于对更新速度和帧率要求比较高，所以比较适合用SurfaceView来显示。

### SurfaceHolder

**控制surface的一个抽象接口**

SurfaceHolder能够控制surface的尺寸和格式，修改surface的像素，监视surface的变化等等，SurfaceHolder的典型应用就是用于SurfaceView中。SurfaceView通过getHolder()方法获得SurfaceHolder 实例，通过后者管理监听surface 的状态。

### SurfaceHolder.Callback

**负责监听surface状态变化的接口**

SurfaceHolder.Callback有三个方法：

**surfaceCreated(SurfaceHolder holder)**:在surface创建后立即被调用。在开发自定义相机时，可以通过重载这个函数调用camera.open()、camera.setPreviewDisplay()，来实现获取相机资源、连接camera和surface等操作。

**surfaceChanged(SurfaceHolder holder, int format, int width, int height)**:在surface发生format或size变化时调用。在开发自定义相机时，可以通过重载这个函数调用camera.startPreview来开启相机预览，使得camera预览帧数据可以传递给surface，从而实时显示相机预览图像。

**surfaceDestroyed(SurfaceHolder holder)**:在surface销毁之前被调用。在开发自定义相机时，可以通过重载这个函数调用camera.stopPreview()，camera.release()来实现停止相机预览及释放相机资源等操作。

### Camera

**最主要的类，用于管理和操作camera资源**

Camera提供了完整的相机底层接口，支持相机资源切换，设置预览/拍摄尺寸，设定光圈、曝光、聚焦等相关参数，获取预览/拍摄帧数据等功能，主要方法有以下这些：

**open()**: 获取camera实例。

**setPreviewDisplay(SurfaceHolder)**: 通过surfaceHolder可以将Camera和surface连接起来，当camera和surface连接后，camera获得的预览帧数据就可以通过surface显示在屏幕上了。

**setPrameters**: 设置相机参数，包括前后摄像头，闪光灯模式、聚焦模式、预览和拍照尺寸等。

**startPreview()**: 开始预览，将camera底层硬件传来的预览帧数据显示在绑定的surface上。

**stopPreview()**: 停止预览，关闭camra底层的帧数据传递以及surface上的绘制。

**release()**: 释放Camera实例

**takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback jpeg)**: 这个是实现相机拍照的主要方法，包含了三个回调参数。shutter是快门按下时的回调，raw是获取拍照原始数据的回调，jpeg是获取经过压缩成jpg格式的图像数据的回调。



## Camera1 相机开发的主要流程

### 检测并访问相机资源

检查手机是否存在相机资源，如果存在，请求访问相机资源。

使用相机开发需要权限声明

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

也可以在应用清单声明中注明使用相机功能

```xml
<uses-feature android:name="android.hardware.camera" />
```

如果应用没有明确要求相机使用清单声明，那么应该检查一下相机在运行时是否可用。

```kotlin
/** 检查设备是否拥有相机 */
private fun checkCameraHardware(context: Context): Boolean {
    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
        return true
    } else {
        return false
    }
}
```

### 创建预览类

创建继承自SurfaceView并实现SurfaceHolder接口的拍摄预览类。此类能够显示相机的实时预览图像。

```kotlin
class CameraPreview(context: Context) :
    SurfaceView(context), SurfaceHolder.Callback {

    private val mHolder: SurfaceHolder = holder.apply {
        // 添加surface状态回调监听
        addCallback(this@CameraPreview)
    }

    // surface创建时回调，可以在这里获取相机实例
    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    // surface改变时回调，可以在这里进行相机预览
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    // surface销毁时回调，可以在这里释放相机
    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}
```

### 建立预览布局

有了拍摄预览类，即可创建一个布局文件，将预览画面与设计好的用户界面控件融合在一起。

### 设置拍照监听器

给用户界面控件绑定监听器，使其能响应用户操作（如按下按钮）, 开始拍照过程。

### 拍照并保存文件

将拍摄获得的图像转换成位图文件，最终输出保存成各种常用格式的图片。

### 释放相机资源

相机是一个共享资源，必须对其生命周期进行细心的管理。当相机使用完毕后，应用程序必须正确地将其释放，以免其它程序访问使用时，发生冲突。



## 结语

Camera1 API已经被遗弃了，这里简单介绍了一下API的使用及相机开发的一般流程，后续再说说Camera2、CameraX以及一些相机开发的注意项等等。



## 代码

https://github.com/hotdl/learnmedia/tree/main/session004



## 参考

https://developer.android.com/guide/topics/media/camera