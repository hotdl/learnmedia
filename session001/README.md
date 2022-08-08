# 安卓音视频开发指南001 -- 绘制图片

## 前言
通过ImageView，SurfaceView，自定义 View等不同方式在安卓平台上绘制图片。

## ImageView
这是最基本，也是最常见的方式了。

### 示例代码
```kotlin
ivAvatar = view.findViewById(R.id.iv_avatar)
// 获取图片
val bitmap = BitmapFactory.decodeStream(context?.assets?.open("avatar.jpg"))
bitmap?.let {
    ivAvatar.setImageBitmap(it)
}
```

## SurfaceView
通过创建surfaceView来进行绘制

### SurfaceView简介
View是通过刷新来重绘视图的，系统通过发出 VSYNC 信号来进行屏幕的重绘，刷新的时间间隔是 16 ms，如果我们可以在 16 ms 以内将绘制工作完成，则没有任何问题，如果我们绘制过程逻辑很复杂，而且我们的界面更新还非常频繁，这时候就会造成界面的卡顿，影响用户体验。为此Android提供了 SurfaceView 来解决这一问题。

### SurfaceView与View的区别
1. View 适用于被动更新的场景，而 SurfaceView 适用于主动更新的情况，比如频繁的刷新界面。（具体原因见下条）
2. View 在主线程中对页面进行刷新，而 SurfaceView 则开启一个子线程来对页面进行刷新。（最本质的区别）
3. View 在绘图的时候没有双缓冲机制，而 SurfaceView 在底层中实现了双缓冲机制。

### 示例代码
```kotlin
surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        // 获取图片
        val bitmap = BitmapFactory.decodeStream(context?.assets?.open("avatar.jpg"))
        bitmap?.let {
            // 锁定画布
            val canvas = holder.lockCanvas()
            // 在画布上画位图
            canvas.drawBitmap(it, 0f, 0f, paint)
            // 解锁画布
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}
})
```

## 自定义View
通过自定义View来绘制，继承View，重写onDraw方法，在onDraw方式中进行图片绘制。

### 示例代码
```kotlin
override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    bitmap?.let {
        // 在画布上画位图
        canvas?.drawBitmap(it, 0f, 0f, paint)
    }
}
```

## 结语
除以上3种绘制图片的方法外，还可以通过OpenGl来进行绘制，后面OpenGl相关章节会说到，这里不再详述。Demo代码在[https://github.com/hotdl/learnmedia/tree/main/session001](https://github.com/hotdl/learnmedia/tree/main/session001)

## 参考
[https://developer.android.google.cn/reference/kotlin/android/view/SurfaceView](https://developer.android.google.cn/reference/kotlin/android/view/SurfaceView)
