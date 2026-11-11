package com.example.practice_pleos

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged

/** Places controls over the reference artwork using its original pixel coordinates. */
class ReRoadLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private val artwork = BitmapFactory.decodeResource(resources, R.drawable.re_road_reference)
    private val artworkPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val artworkBounds = Rect()
    private lateinit var controlBounds: List<Rect>

    init {
        setWillNotDraw(false)
        setBackgroundColor(0xFFE3E3E3.toInt())
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        controlBounds = List(childCount) { index ->
            val (left, top, right, bottom) = (getChildAt(index).tag as String)
                .split(',').map(String::toInt)
            Rect(left, top, right, bottom)
        }
        val search = findViewById<EditText>(R.id.search_input)
        fun updateSearchBackground() {
            // The artwork supplies the idle placeholder. Cover it while entering a query.
            search.setBackgroundColor(
                if (search.hasFocus() || search.text.isNotEmpty()) 0xFFE9F0F7.toInt() else 0x00000000
            )
        }
        search.setOnFocusChangeListener { _, _ -> updateSearchBackground() }
        search.doAfterTextChanged { updateSearchBackground() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(artwork.width, widthMeasureSpec)
        val height = resolveSize(artwork.height, heightMeasureSpec)
        setMeasuredDimension(width, height)

        val scale = minOf(width.toFloat() / artwork.width, height.toFloat() / artwork.height)
        val imageWidth = (artwork.width * scale).toInt()
        val imageHeight = (artwork.height * scale).toInt()
        val left = (width - imageWidth) / 2
        val top = (height - imageHeight) / 2
        artworkBounds.set(left, top, left + imageWidth, top + imageHeight)

        for (index in controlBounds.indices) {
            val bounds = controlBounds[index]
            val child = getChildAt(index)
            child.measure(
                MeasureSpec.makeMeasureSpec(scaleX(bounds.right) - scaleX(bounds.left), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(scaleY(bounds.bottom) - scaleY(bounds.top), MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in controlBounds.indices) {
            val bounds = controlBounds[index]
            getChildAt(index).layout(
                scaleX(bounds.left), scaleY(bounds.top), scaleX(bounds.right), scaleY(bounds.bottom)
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(artwork, null, artworkBounds, artworkPaint)
    }

    private fun scaleX(x: Int) = artworkBounds.left + x * artworkBounds.width() / artwork.width
    private fun scaleY(y: Int) = artworkBounds.top + y * artworkBounds.height() / artwork.height
}
