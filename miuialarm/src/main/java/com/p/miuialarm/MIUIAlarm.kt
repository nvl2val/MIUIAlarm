package com.p.miuialarm

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import java.util.*

/**
 * The degrees of hour, minute and second indicator is relative to time '00:00:00',
 * rather than positive direction of axis X.
 */
class MIUIAlarm @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null):
        View(context, attrs){
    private val cam3d = Camera()
    private val tmpMatrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val viewportSideLen: Float = 850f
    private val gradientItemIndexColorMap = SparseIntArray()
    private val path = Path()
    private var rect = Rect()
    private val rectF = RectF()

    // second track arc
    private val secondTrackStrokeWidth = 5f
    private val secondTrackInnerRadius = 326f
    private val secondTrackOuterRadius = 370f

    // triangle second indicator
    private val secondIndicatorBottomOrbitRadius = 286f
    private val secondIndicatorBottomWidth = 41f
    private val secondIndicatorHeight = 33f

    // dot orbit
    private val dotOrbitRadius = 268f
    private val dotOrbitStrokeWidth = 2f

    // dot
    private val dotRadius = 8.5f
    private val dotVirtualOrbitRadius = 268f

    // hour screw and hand
    private val hourScrewInnerRadius = 17f
    private val hourScrewOuterRadius = 20f
    private val hourHeadHeight = 8f
    private val hourBodyHeight = 182f
    private val hourTopWidth = 10f
    private val hourBottomWidth = 16f

    // minute screw and hand
    private val minuteScrewInnerRadius = 11f
    private val minuteScrewOuterRadius = 23f
    private val minuteHeadHeight = 4f
    private val minuteBodyHeight = 225f
    private val minuteTopWidth = 7f
    private val minuteBottomWidth = 10f

    private val secondTrackPieceCount = 200
    private val secondTrackPieceDegree: Double = 360.0/secondTrackPieceCount

    private var hourDegrees: Float = 0f
    private var minuteDegrees: Float = 0f
    private var secondDegrees: Float = 0f
    private val maxCanvasRotateDegrees = 10f
    private var canvasXRotateDegrees: Float = 0f
    private var canvasYRotateDegrees: Float = 0f

    private var secondTrackIndicatorCanvasTransX: Float = 0f
    private var secondTrackIndicatorCanvasTransY: Float = 0f
    private var dotOrbitCanvasTransX: Float = 0f
    private var dotOrbitCanvasTransY: Float = 0f
    private var dotCanvasTransX: Float = 0f
    private var dotCanvasTransY: Float = 0f
    private var hourCanvasTransX: Float = 0f
    private var hourCanvasTransY: Float = 0f
    private var minuteCanvasTransX: Float = 0f
    private var minuteCanvasTransY: Float = 0f

    private var lastTouchEvent: MotionEvent? = null

    private var strongColor: Int = 0xffffffff.toInt()
    private var lightColor: Int = strongColor and LIGHT_COLOR_MASK
    private var alarmPadding: Float = 0f

    init {
        run {
            if (attrs==null) return@run
            val a = context.obtainStyledAttributes(attrs, R.styleable.MIUIAlarm)
            val alarmColor = a.getColor(R.styleable.MIUIAlarm_alarmColor, strongColor)
            alarmPadding = a.getDimension(R.styleable.MIUIAlarm_alarmPadding, alarmPadding)
            a.recycle()

            strongColor = alarmColor and STRONG_COLOR_MASK
            lightColor = strongColor and LIGHT_COLOR_MASK
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean =
            if (event==null) {
                super.onTouchEvent(event)
            }else {
                lastTouchEvent = when(event.action){
                    MotionEvent.ACTION_UP-> null
                    else-> event
                }
                true
            }

    /**
     * @param scale scale from real pixel-width to viewport-width
     */
    private fun setCanvasRotationAndTranslation(scale: Float,
                                                preScaledCanvasTransX: Float,
                                                preScaledCanvasTransY: Float,
                                                cx: Float, cy: Float){
        if (lastTouchEvent == null) {
            canvasXRotateDegrees = 0f
            canvasYRotateDegrees = 0f

            secondTrackIndicatorCanvasTransX = 0f
            secondTrackIndicatorCanvasTransY = 0f
            dotOrbitCanvasTransX = 0f
            dotOrbitCanvasTransY = 0f
            dotCanvasTransX = 0f
            dotCanvasTransY = 0f
            hourCanvasTransX = 0f
            hourCanvasTransY = 0f
            minuteCanvasTransX = 0f
            minuteCanvasTransY = 0f
            return
        }
        getGlobalVisibleRect(rect)
        val x = (lastTouchEvent!!.rawX - rect.left - preScaledCanvasTransX) / scale
        val y = (lastTouchEvent!!.rawY - rect.top - preScaledCanvasTransY) / scale
        val halfSideLen = viewportSideLen /2f
        val xPercent = clamp(-1f, 1f, (x-cx)/halfSideLen)
        val yPercent = clamp(-1f, 1f, (y-cy)/halfSideLen)

        // set rotation
        val xRotationPercent = -yPercent
        val yRotationPercent = xPercent
        canvasXRotateDegrees = maxCanvasRotateDegrees*xRotationPercent
        canvasYRotateDegrees = maxCanvasRotateDegrees*yRotationPercent

        // set translation
        val xTransPercent = xPercent
        val yTransPercent = yPercent
        secondTrackIndicatorCanvasTransX = 5*xTransPercent
        secondTrackIndicatorCanvasTransY = 5*yTransPercent
        dotOrbitCanvasTransX = 10*xTransPercent
        dotOrbitCanvasTransY = 10*yTransPercent
        dotCanvasTransX = 20*xTransPercent
        dotCanvasTransY = 20*yTransPercent
        hourCanvasTransX = 10*xTransPercent
        hourCanvasTransY = 10*yTransPercent
        minuteCanvasTransX = 20*xTransPercent
        minuteCanvasTransY = 20*yTransPercent
    }

    private fun setTimeDegrees(){
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY) % 12
        val m = c.get(Calendar.MINUTE)
        val s = c.get(Calendar.SECOND)
        val millis = c.get(Calendar.MILLISECOND)
        hourDegrees = 360*((millis + s*1000 + m*60000 + h*3600000)/43200000f)
        minuteDegrees = 360*((millis + s*1000 + m*60000)/3600000f)
        secondDegrees = 360*((millis + s*1000)/60000f)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas==null || width==0 || height==0) return
        canvas.save()

        setTimeDegrees()
        val dx = width/2f
        val dy = height/2f
        canvas.translate(dx, dy)

        val preScaledSideLen = Math.max(Math.min(width, height)-alarmPadding*2, 0f)
        if (preScaledSideLen == 0f) return
        val scale = preScaledSideLen/ viewportSideLen
        canvas.scale(scale, scale)

        val cx = 0f
        val cy = 0f
        setCanvasRotationAndTranslation(scale, dx, dy, cx, cy)

        rotateCanvasXY(canvas, cx, cy, canvasXRotateDegrees, canvasYRotateDegrees)
        drawSecondTrack(canvas, cx, cy)
        drawSecondIndicator(canvas, cx, cy)
        drawDotOrbit(canvas, cx, cy)
        drawDot(canvas, cx, cy)
        drawHand(canvas, cx, cy, lightColor,
                hourScrewInnerRadius, hourScrewOuterRadius,
                hourHeadHeight, hourBodyHeight,
                hourTopWidth, hourBottomWidth, hourDegrees, hourCanvasTransX, hourCanvasTransY)
        drawHand(canvas, cx, cy, strongColor,
                minuteScrewInnerRadius, minuteScrewOuterRadius,
                minuteHeadHeight, minuteBodyHeight,
                minuteTopWidth, minuteBottomWidth, minuteDegrees, minuteCanvasTransX, minuteCanvasTransY)
        invalidate()
        canvas.restore()
    }

    /**
     * Rotation is based on [android.graphics.Camera]'s coordination.
     */
    private fun rotateCanvasXY(canvas: Canvas, cx: Float, cy: Float, rotateX: Float, rotateY: Float){
        tmpMatrix.reset()
        cam3d.save()
        cam3d.rotateX(rotateX)
        cam3d.rotateY(rotateY)
        cam3d.getMatrix(tmpMatrix)
        cam3d.restore()
        tmpMatrix.preTranslate(-cx, -cy)
        tmpMatrix.postTranslate(cx, cy)
        canvas.concat(tmpMatrix)
    }

    private fun drawSecondTrack(canvas: Canvas, cx: Float, cy: Float){
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.color = lightColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = secondTrackStrokeWidth

        canvas.save()
        resetGradientItemIndexesAndColors()
        (0 until secondTrackPieceCount).forEach {
            val color = gradientItemIndexColorMap[it]
            if (color != 0)
                paint.color = color
            else
                paint.color = lightColor
            canvas.drawLine(cx, cy-secondTrackInnerRadius,
                    cx, cy-secondTrackOuterRadius, paint)
            canvas.rotate(secondTrackPieceDegree.toFloat(), cx, cy)
        }
        canvas.restore()
    }

    private fun drawSecondIndicator(canvas: Canvas, cx: Float, cy: Float){
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.color = lightColor
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx-secondIndicatorBottomWidth/2, cy-secondIndicatorBottomOrbitRadius)
        path.lineTo(cx, cy-secondIndicatorBottomOrbitRadius-secondIndicatorHeight)
        path.lineTo(cx+secondIndicatorBottomWidth/2, cy-secondIndicatorBottomOrbitRadius)
        canvas.save()
        canvas.translate(secondTrackIndicatorCanvasTransX, secondTrackIndicatorCanvasTransY)
        canvas.rotate(secondDegrees, cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawDotOrbit(canvas: Canvas, cx: Float, cy: Float){
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.color = lightColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dotOrbitStrokeWidth
        canvas.save()
        canvas.translate(dotOrbitCanvasTransX, dotOrbitCanvasTransY)
        canvas.drawCircle(cx, cy, dotOrbitRadius, paint)
        canvas.restore()
    }

    private fun drawDot(canvas: Canvas, cx: Float, cy: Float){
        paint.reset()
        paint.color = strongColor
        paint.style = Paint.Style.FILL
        canvas.save()
        canvas.translate(dotCanvasTransX, dotCanvasTransY)
        canvas.rotate(165f, cx, cy)
        canvas.drawCircle(cx, cy-dotVirtualOrbitRadius, dotRadius, paint)
        canvas.restore()
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, paintColor: Int,
                         screwInnerRadius: Float,
                         screwOuterRadius: Float,
                         handHeadHeight: Float,
                         handBodyHeight: Float,
                         handTopWidth: Float,
                         handBottomWidth: Float,
                         thetaInDegrees: Float, transX: Float, transY: Float){
        canvas.save()

        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.style = Paint.Style.STROKE
        paint.color = paintColor

        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.FILL
        canvas.save()
        canvas.translate(transX, transY)
        canvas.rotate(thetaInDegrees, cx, cy)
        path.reset()
        val bottomY = cy-(Math.sqrt(Math.pow(screwOuterRadius.toDouble(), 2.0)-Math.pow(handBottomWidth/2.0, 2.0))).toFloat()
        val topY = cy-screwOuterRadius-handBodyHeight
        path.moveTo(cx-handBottomWidth/2, bottomY)
        path.lineTo(cx-handTopWidth/2, topY)
        path.quadTo(cx, cy-screwOuterRadius-handBodyHeight-handHeadHeight,
                cx+ handTopWidth /2, topY)
        path.lineTo(cx+handBottomWidth/2, bottomY)
        path.close()
        canvas.drawPath(path, paint)
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        rectF.set(cx-screwOuterRadius, cy-screwOuterRadius, cx+screwOuterRadius, cy+screwOuterRadius)
        path.addOval(rectF, Path.Direction.CW)
        rectF.set(cx-screwInnerRadius, cy-screwInnerRadius, cx+screwInnerRadius, cy+screwInnerRadius)
        path.addOval(rectF, Path.Direction.CW)
        canvas.drawPath(path, paint)
        canvas.restore()

        canvas.restore()
    }

    private fun resetGradientItemIndexesAndColors(){
        gradientItemIndexColorMap.clear()
        var index = (secondDegrees /secondTrackPieceDegree).toInt()
        val endAlpha = Color.alpha(lightColor)
        val deltaAlpha = 255- endAlpha
        val gradientItemCount = (90/secondTrackPieceDegree).toInt()
        (0 until gradientItemCount).forEach {
            val alpha = (255 - deltaAlpha.toFloat()*it/gradientItemCount).toInt()
            val color = (lightColor and 0x00ffffff) or (alpha shl 24)
            gradientItemIndexColorMap.put(index, color)
            index = (index-1+secondTrackPieceCount)%secondTrackPieceCount
        }
    }

    private fun clamp(min: Float, max: Float, x: Float): Float = when {
        x<min -> min
        x>max -> max
        else -> x
    }

    companion object {
        @JvmStatic private val TAG = MIUIAlarm::class.java.simpleName
        @JvmStatic private val STRONG_COLOR_MASK = 0xffffffff.toInt()
        @JvmStatic private val LIGHT_COLOR_MASK = 0x8fffffff.toInt()
    }
}