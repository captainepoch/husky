/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Some code is based on https://github.com/qoqa/glide-svg
 *
 * No copyright asserted on the source code of this repository.
 */

package com.keylesspalace.tusky.core.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.caverock.androidsvg.SVG

class SvgDrawableTranscoder(private val context: Context) : ResourceTranscoder<SVG, Drawable> {

    override fun transcode(toTranscode: Resource<SVG>, options: Options): Resource<Drawable> {
        return SimpleResource(
            BitmapDrawable(
                context.resources,
                getBitmap(toTranscode.get()).get()
            )
        )
    }

    private fun getBitmap(svg: SVG): SimpleResource<Bitmap> {
        val width = svg.documentWidth.toInt()
        val height = svg.documentHeight.toInt()

        val drawable = PictureDrawable(svg.renderToPicture(width, height))
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawPicture(drawable.picture)

        return SimpleResource(bitmap)
    }
}
