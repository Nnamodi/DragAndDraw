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
    private val description = this.apply {
        contentDescription = context.getString(R.string.no_boxes_drawn)
    }
    private var lastTouchX = 0.0f ; private var lastTouchY = 0.0f
    private var degree = 0.0f
    private var downTouch = false
    // The 'active pointer' is the one currently moving our object.
    private var activePointerId = -1

    init {
        isSaveEnabled = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val current = PointF(event.x, event.y)
        var action = ""
        var text = ""
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                action = "ACTION_DOWN"
                // Reset drawing state
                currentBox = Box(current).also {
                    boxen.add(it)
                }
                degree = 0.0f
                // Remember where we started
                lastTouchX = current.x ; lastTouchY = current.y
                // Save the ID of this pointer
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                action = "ACTION_MOVE"
                if (downTouch) degree++
                // Find the index of the active pointer and fetch its position
                val pointerIndex = event.findPointerIndex(activePointerId)
                current.x = event.getX(pointerIndex)
                current.y = event.getY(pointerIndex)
                // Remember this touch position for the next move event
                lastTouchX = current.x ; lastTouchY = current.y
                updateCurrentBox(current)
            }
            MotionEvent.ACTION_UP -> {
                action = "ACTION_UP"
                text = context.getString(R.string.drawn_box_location, current.x, current.y)
                updateCurrentBox(current)
                currentBox = null
                activePointerId = -1
                description
            }
            MotionEvent.ACTION_CANCEL -> {
                action = "ACTION_CANCEL"
                currentBox = null
                activePointerId = -1
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                action = "ACTION_POINTER_DOWN"
                downTouch = true
                val actionIndex = event.actionIndex
                current.x = event.getX(actionIndex) ; current.y = event.getY(actionIndex)
                // Remember this touch position for the next move event
                lastTouchX = current.x ; lastTouchY = current.y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                action = "ACTION_POINTER_UP"
                downTouch = false
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
        this.contentDescription = text
        Log.i("BoxDrawView", "$action at x = ${current.x}, y = ${current.y} and rotated to $degree")
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // Fill the background
        canvas.drawPaint(backgroundPaint)
        boxen.forEach { box ->
            canvas.save()
            canvas.rotate(degree)
            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)
            canvas.restore()
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