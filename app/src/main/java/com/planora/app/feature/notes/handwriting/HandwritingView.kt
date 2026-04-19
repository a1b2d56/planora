package com.planora.app.feature.notes.handwriting

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Canvas-based freehand drawing view with velocity-aware stroke rendering. */
class HandwritingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokes = mutableListOf<DrawStroke>()

    private val activePoints = mutableListOf<DrawPoint>()
    private val undoStack = mutableListOf<DrawStroke>()

    private var cacheBitmap: Bitmap? = null
    private var cacheCanvas: Canvas? = null


    var penType: PenType = PenType.PEN
    var penColor: Int = Color.WHITE
    var penWidth: Float = 8f
    var penAlpha: Int = 255
    var paperType: PaperType = PaperType.PLAIN

    var onStrokeCompleted: (() -> Unit)? = null


    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val paperPaint = Paint().apply {
        color = Color.WHITE
        alpha = 15
        strokeWidth = 1f
    }

    private val eraserPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }



    private var eraserX: Float? = null
    private var eraserY: Float? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (penType == PenType.ERASER) {
                    eraserX = event.x
                    eraserY = event.y
                }
                activePoints.clear()
                activePoints.add(DrawPoint(event.x, event.y, event.pressure, event.eventTime))
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    activePoints.add(
                        DrawPoint(
                            event.getHistoricalX(i),
                            event.getHistoricalY(i),
                            event.getHistoricalPressure(i),
                            event.getHistoricalEventTime(i)
                        )
                    )
                }
                if (penType == PenType.ERASER) {
                    eraserX = event.x
                    eraserY = event.y
                }
                activePoints.add(DrawPoint(event.x, event.y, event.pressure, event.eventTime))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                eraserX = null
                eraserY = null
                if (activePoints.size > 1) {
                    val stroke = DrawStroke(
                        points = activePoints.toList(),
                        color = penColor,
                        width = penWidth,
                        penType = penType,
                        alpha = penAlpha
                    )
                    strokes.add(stroke)
                    undoStack.clear()
                    bakeStrokeToCache(stroke)
                    onStrokeCompleted?.invoke()
                }
                activePoints.clear()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        drawPaperBackground(canvas)


        cacheBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }


        if (activePoints.size > 1) {
            val activeStroke = DrawStroke(
                points = activePoints.toList(),
                color = penColor,
                width = penWidth,
                penType = penType,
                alpha = penAlpha
            )
            renderStroke(canvas, activeStroke)
        }


        val ex = eraserX
        val ey = eraserY
        if (penType == PenType.ERASER && ex != null && ey != null) {
            val density = resources.displayMetrics.density
            val r = penWidth * 3f / 2f
            
            val eraserIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.DKGRAY
                strokeWidth = 2f * density
                alpha = 150
            }
            canvas.drawCircle(ex, ey, r, eraserIndicatorPaint)
            
            eraserIndicatorPaint.color = Color.WHITE
            eraserIndicatorPaint.strokeWidth = 1f * density
            canvas.drawCircle(ex, ey, r, eraserIndicatorPaint)
        }
    }


    private fun renderStroke(canvas: Canvas, stroke: DrawStroke) {
        if (stroke.points.size < 2) return

        if (stroke.penType == PenType.ERASER) {
            renderEraserStroke(canvas, stroke)
            return
        }

        val points = stroke.points
        val baseWidth = stroke.width
        val density = resources.displayMetrics.density


        val paint = when (stroke.penType) {
            PenType.MARKER -> {
                fillPaint.apply {
                    color = stroke.color
                    alpha = (0.35f * 255).toInt()
                    strokeWidth = baseWidth * 3f
                    maskFilter = null
                }
            }
            PenType.PENCIL -> {
                strokePaint.apply {
                    color = stroke.color
                    alpha = (0.7f * 255).toInt()
                    strokeWidth = baseWidth
                    maskFilter = null
                }
            }
            else -> {
                fillPaint.apply {
                    color = stroke.color
                    alpha = stroke.alpha
                    strokeWidth = 2f * density
                    maskFilter = BlurMaskFilter(0.3f * density, BlurMaskFilter.Blur.SOLID)
                }
            }
        }

        if (stroke.penType == PenType.PEN) {
            renderVelocityStroke(canvas, points, paint, baseWidth, density)
        } else {
            renderSimpleStroke(canvas, points, paint)
        }
    }

    /** Velocity-aware stroke: builds filled quads whose width varies with drawing speed. */
    private fun renderVelocityStroke(
        canvas: Canvas,
        points: List<DrawPoint>,
        paint: Paint,
        baseWidth: Float,
        density: Float
    ) {
        val path = Path()

        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]


            val dx = p1.x - p0.x
            val dy = p1.y - p0.y
            val dist = hypot(dx, dy)
            val dt = max(1L, p1.timestamp - p0.timestamp).toFloat()
            val velocity = dist / dt


            val widthFactor = (1f / (1f + velocity * 0.12f)).coerceIn(0.3f, 1.0f)
            val w = baseWidth * widthFactor * 0.5f


            val len = max(dist, 0.01f)
            val nx = -dy / len * w
            val ny = dx / len * w


            path.reset()
            path.moveTo(p0.x + nx, p0.y + ny)
            path.lineTo(p1.x + nx, p1.y + ny)
            path.lineTo(p1.x - nx, p1.y - ny)
            path.lineTo(p0.x - nx, p0.y - ny)
            path.close()
            canvas.drawPath(path, paint)
        }


        val capPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            maskFilter = null
        }
        for (i in points.indices) {
            val p = points[i]
            val velocity = if (i > 0) {
                val prev = points[i - 1]
                val d = hypot(p.x - prev.x, p.y - prev.y)
                val t = max(1L, p.timestamp - prev.timestamp).toFloat()
                d / t
            } else 0f
            val r = baseWidth * (1f / (1f + velocity * 0.12f)).coerceIn(0.3f, 1.0f) * 0.5f
            canvas.drawCircle(p.x, p.y, r, capPaint)
            
            // Interpolate between distant points for continuity
            if (i > 0) {
                val prev = points[i - 1]
                val dist = hypot(p.x - prev.x, p.y - prev.y)
                if (dist > r) {
                    val steps = (dist / r).toInt().coerceAtMost(20)
                    for (step in 1 until steps) {
                        val fraction = step.toFloat() / steps
                        val ix = prev.x + (p.x - prev.x) * fraction
                        val iy = prev.y + (p.y - prev.y) * fraction
                        canvas.drawCircle(ix, iy, r, capPaint)
                    }
                }
            }
        }
    }

    /** Quadratic Bézier smoothing for pencil and marker strokes. */
    private fun renderSimpleStroke(canvas: Canvas, points: List<DrawPoint>, paint: Paint) {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)


        for (i in 1 until points.size - 1) {
            val midX = (points[i].x + points[i + 1].x) / 2f
            val midY = (points[i].y + points[i + 1].y) / 2f
            path.quadTo(points[i].x, points[i].y, midX, midY)
        }


        val last = points.last()
        path.lineTo(last.x, last.y)

        canvas.drawPath(path, paint)
    }

    private fun renderEraserStroke(canvas: Canvas, stroke: DrawStroke) {
        val path = Path()
        val points = stroke.points
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size - 1) {
            val midX = (points[i].x + points[i + 1].x) / 2f
            val midY = (points[i].y + points[i + 1].y) / 2f
            path.quadTo(points[i].x, points[i].y, midX, midY)
        }
        path.lineTo(points.last().x, points.last().y)

        eraserPaint.strokeWidth = stroke.width * 3f
        canvas.drawPath(path, eraserPaint)
    }



    private fun ensureCache() {
        if (cacheBitmap == null || cacheBitmap!!.width != width || cacheBitmap!!.height != height) {
            cacheBitmap?.recycle()
            if (width > 0 && height > 0) {
                cacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                cacheCanvas = Canvas(cacheBitmap!!)
                rebakeAllStrokes()
            }
        }
    }

    private fun bakeStrokeToCache(stroke: DrawStroke) {
        ensureCache()
        cacheCanvas?.let { renderStroke(it, stroke) }
    }

    private fun rebakeAllStrokes() {
        cacheBitmap?.eraseColor(Color.TRANSPARENT)
        cacheCanvas?.let { c ->
            strokes.forEach { renderStroke(c, it) }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ensureCache()
    }



    private fun drawPaperBackground(canvas: Canvas) {
        val gridSize = 40f * resources.displayMetrics.density
        when (paperType) {
            PaperType.GRID -> {
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), paperPaint)
                    x += gridSize
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, paperPaint)
                    y += gridSize
                }
            }
            PaperType.DOTTED -> {
                var x = gridSize / 2
                while (x < width) {
                    var y = gridSize / 2
                    while (y < height) {
                        canvas.drawCircle(x, y, 1.5f * resources.displayMetrics.density, paperPaint)
                        y += gridSize
                    }
                    x += gridSize
                }
            }
            PaperType.PLAIN -> {}
        }
    }



    fun undo(): Boolean {
        if (strokes.isEmpty()) return false
        undoStack.add(strokes.removeAt(strokes.size - 1))
        rebakeAllStrokes()
        invalidate()
        return true
    }

    fun redo(): Boolean {
        if (undoStack.isEmpty()) return false
        val stroke = undoStack.removeAt(undoStack.size - 1)
        strokes.add(stroke)
        bakeStrokeToCache(stroke)
        invalidate()
        return true
    }

    val canUndo: Boolean get() = strokes.isNotEmpty()
    val canRedo: Boolean get() = undoStack.isNotEmpty()
    val hasContent: Boolean get() = strokes.isNotEmpty()

    fun getStrokes(): List<DrawStroke> = strokes.toList()

    fun setStrokes(data: List<DrawStroke>) {
        strokes.clear()
        strokes.addAll(data)
        undoStack.clear()
        cacheBitmap = null
        cacheCanvas = null
        ensureCache()
        invalidate()
    }

    fun clearAll() {
        strokes.clear()
        undoStack.clear()
        cacheBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cacheBitmap?.recycle()
        cacheBitmap = null
        cacheCanvas = null
    }
}
