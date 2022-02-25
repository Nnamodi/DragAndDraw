package com.bignerdranch.android.draganddraw

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

private const val INVALID_POINTER_ID = -1

class BoxDrawingView(context: Context, attr: AttributeSet? = null) : View(context, attr) {
    private var currentBox: Box? = null
    private val boxen = mutableListOf<Box>()
    private val boxPaint = Paint().apply {
        color = 0x22ff0000
    }
    private val backgroundPaint = Paint().apply {
        color = 0xfff8efe0.toInt()
    }
    private var boxes = this.id
    private var posX = 0.0f ; private var posY = 0.0f
    private var lastTouchX = 0.0f ; private var lastTouchY = 0.0f
    // The 'active pointer' is the one currently moving our object.
    private var activePointerId = INVALID_POINTER_ID

    init {
        isSaveEnabled = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val current = PointF(event.x, event.y)
        var action = ""
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                action = "ACTION_DOWN"
                // Reset drawing state
                currentBox = Box(current).also {
                    boxen.add(it)
                }
                // Remember where we started
                lastTouchX = event.x ; lastTouchY = event.y
                // Save the ID of this pointer
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                action = "ACTION_MOVE"
                // Find the index of the active pointer and fetch its position
                val pointerIndex = event.findPointerIndex(activePointerId)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                // Calculate the distance moved
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                // Move the object
                posX += dx ; posY += dy
                // Remember this touch position for the next move event
                lastTouchX = x ; lastTouchY = y
                updateCurrentBox(current)
            }
            MotionEvent.ACTION_UP -> {
                action = "ACTION_UP"
                updateCurrentBox(current)
                currentBox = null
                activePointerId = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_CANCEL -> {
                action = "ACTION_CANCEL"
                currentBox = null
                activePointerId = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // Extract the index of the pointer that left the touch sensor
                val pointerIndex = (event.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new active pointer and adjust accordingly
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        Log.i("BoxDrawView", "$action at x = ${current.x}, y = ${current.y}")
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // Fill the background
        canvas.drawPaint(backgroundPaint)
        canvas.save()
        canvas.translate(posX, posY)
        canvas.restore()

        boxen.forEach { box ->
            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val bundle = Bundle()
        bundle.putInt("points", boxes)
        bundle.putParcelable("superState", superState)
        Log.d("SavedState", "$bundle")
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var view = state
        if (view is Bundle) {
            boxes = view.getInt("points")
            view = view.getParcelable("superState")
        }
        Log.d("SavedState", "$view, $boxes")
        super.onRestoreInstanceState(view)
    }

    private fun updateCurrentBox(current: PointF) {
        currentBox?.let {
            it.end = current
            invalidate()
        }
    }
}