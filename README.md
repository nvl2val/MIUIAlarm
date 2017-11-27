# MIUIAlarm
仿MIUI的闹钟

很大程度上参考了[MiClockView][1]，包括：
* Path的使用
* 利用Canvas的旋转实现路径的绘制
* 利用Camera实现钟表的旋转效果

不同点包括：
* 采用Canvas的scale将不同size映射到固定的viewport，避免了比例计算
* View的背景色和闹钟是分离的，闹钟的颜色可单独设置
* 由于上一条，秒针轨迹渐变的绘制方法采用了手动计算，而非是SweepGradient

添加了定制属性：
* `alarmColor`: 可更改闹钟的颜色
* `alarmPadding`: 可更改闹钟据外沿的距离

# English version
Project [MiClockView][1] helps a lot, which includes:
* using `Path` to draw irregular shape
* using `save()`+`rotate()`+`restore()` of `Canvas` to avoid trigonometric calculation
* using `android.graphics.Camera` to implement the rotation effect

Differences:
* using `Canvas.scale()` to map different size to fixed viewport to avoid ratio calculation
* separating alarm's color from View's background, so as to set alarm's color separately
* because of previous one, calculating color gradient of second's track manually instead of using `SweepGradient`

Add some custom attributes:
* `alarmColor`: change alarm's color
* `alarmPadding`: change distance between alarm's edge and View's edge. Default is 0.



[1]:https://github.com/MonkeyMushroom/MiClockView
