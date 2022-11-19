/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>.
 */

package com.keylesspalace.tusky.util

import android.content.res.Resources
import android.graphics.Rect
import android.util.TypedValue
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
/*import java.util.List
import java.util.ArrayList*/

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.visible(visible: Boolean, or: Int = View.GONE) {
    this.visibility = if (visible) View.VISIBLE else or
}

class MultipleTouchDelegate : TouchDelegate {

    var delegates = mutableListOf<TouchDelegate>()

    constructor(v: View) : super(Rect(), v)

    public fun addDelegate(delegate: TouchDelegate) {
        delegates.add(delegate)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var ret = false
        val x = event.x
        val y = event.y

        for (delegate in delegates) {
            event.setLocation(x, y)
            ret = delegate.onTouchEvent(event) || ret
        }

        return ret
    }
}

fun View.increaseHitArea(vdp: Float, hdp: Float) {
    val parent = this.parent as View
    val vpixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, vdp, Resources.getSystem().displayMetrics).toInt()
    val hpixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, hdp, Resources.getSystem().displayMetrics).toInt()
    parent.post {
        val rect = Rect()
        this.getHitRect(rect)
        rect.top -= vpixels
        rect.left -= hpixels
        rect.bottom += vpixels
        rect.right += hpixels
        if (parent.touchDelegate != null && parent.touchDelegate is MultipleTouchDelegate) {
            (parent.touchDelegate as MultipleTouchDelegate).addDelegate(TouchDelegate(rect, this))
        } else {
            val mtd = MultipleTouchDelegate(this)
            mtd.addDelegate(TouchDelegate(rect, this))
            parent.touchDelegate = mtd
        }
    }
}
