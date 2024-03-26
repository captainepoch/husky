/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2021  Tusky Contributors
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

@file:JvmName("CustomEmojiHelper")

package com.keylesspalace.tusky.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.View
import android.webkit.MimeTypeMap
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.penfeizhou.animation.glide.AnimationDecoderOption
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.MIME.SVG
import java.lang.ref.WeakReference
import java.util.regex.Pattern
import timber.log.Timber

/**
 * Replaces emoji shortcodes in a text with EmojiSpans.
 *
 * @param emojis a list of the custom emojis (nullable for backward compatibility with old
 * mastodon instances).
 * @param view a reference to the a view the emojis will be shown in (should be the TextView,
 * but parents of the TextView are also acceptable).
 * @param forceSmallEmoji force emojis to have a smaller size (default: false).
 *
 * @return The text with the shortcodes replaced by EmojiSpans
 */
fun CharSequence.emojify(
    emojis: List<Emoji>?, view: View, forceSmallEmoji: Boolean = false
): CharSequence {
    if (emojis.isNullOrEmpty()) {
        return this
    }

    val builder = SpannableString.valueOf(this)
    val pm = PreferenceManager.getDefaultSharedPreferences(view.context)
    val smallEmojis = forceSmallEmoji || !pm.getBoolean(PrefKeys.BIG_EMOJIS, true)
    val animate = pm.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)

    emojis.forEach { (shortcode, url) ->
        val matcher = Pattern.compile(":$shortcode:", Pattern.LITERAL).matcher(this)

        while (matcher.find()) {
            val span = createEmojiSpan(url, view, smallEmojis, animate)
            builder.setSpan(span, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    return builder
}

fun CharSequence.emojify(emojis: List<Emoji>?, view: View): CharSequence {
    return this.emojify(emojis, view, false)
}

fun createEmojiSpan(emojiUrl: String, view: View, forceSmallEmoji: Boolean = false): EmojiSpan {
    val pm = PreferenceManager.getDefaultSharedPreferences(view.context)
    val smallEmojis = forceSmallEmoji || !pm.getBoolean(PrefKeys.BIG_EMOJIS, true)
    val animate = pm.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)

    return createEmojiSpan(emojiUrl, view, smallEmojis, animate)
}

private fun createEmojiSpan(
    emojiUrl: String,
    view: View,
    smallEmojis: Boolean = false,
    animate: Boolean = false
): EmojiSpan {
    val span = if (smallEmojis) {
        SmallEmojiSpan(WeakReference<View>(view), 1.0)
    } else {
        EmojiSpan(WeakReference<View>(view))
    }

    var glideRequest =
        Glide.with(view)
                .load(emojiUrl)
                .set(AnimationDecoderOption.DISABLE_ANIMATION_GIF_DECODER, !animate)
                .set(AnimationDecoderOption.DISABLE_ANIMATION_WEBP_DECODER, !animate)
                .set(AnimationDecoderOption.DISABLE_ANIMATION_APNG_DECODER, !animate)
    getMimeType(emojiUrl).takeIf { it == SVG }?.let {
        glideRequest =
            glideRequest.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).override(512, 512)
    }
    glideRequest.into(span.getTarget(animate))

    return span
}

open class EmojiSpan(
    val viewWeakReference: WeakReference<View>, private val aspectRatio: Double = 2.0
) : ReplacementSpan() {

    var imageDrawable: Drawable? = null

    override fun getSize(
        paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?
    ): Int {
        // Update FontMetricsInt or otherwise span does not get drawn when
        // it covers the whole text
        if (fm != null) {
            val metrics = paint.fontMetricsInt
            fm.top = (metrics.top * 1.3f).toInt()
            fm.ascent = (metrics.ascent * 1.3f).toInt()
            fm.descent = (metrics.descent * 2.0f).toInt()
            fm.bottom = (metrics.bottom * 3.5f).toInt()
        }

        return (paint.textSize * aspectRatio).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        imageDrawable?.let { drawable ->
            canvas.save()

            // Start with a width relative to the text size
            var emojiWidth = paint.textSize * aspectRatio

            // Calculate the height, keeping the aspect ratio correct
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight
            var emojiHeight = emojiWidth / drawableWidth * drawableHeight

            // How much vertical space there is draw the emoji
            val drawableSpace = (bottom - top).toDouble()

            // In case the calculated height is bigger than the available space,
            // scale the emoji down, preserving aspect ratio
            if (emojiHeight > drawableSpace) {
                emojiWidth *= drawableSpace / emojiHeight
                emojiHeight = drawableSpace
            }
            drawable.setBounds(0, 0, emojiWidth.toInt(), emojiHeight.toInt())

            // Vertically center the emoji in the line
            val transY = top + (drawableSpace / 2 - emojiHeight / 2)

            canvas.translate(x, transY.toFloat())
            drawable.draw(canvas)
            canvas.restore()
        }
    }

    fun getTarget(animate: Boolean): Target<Drawable> {
        return object : CustomTarget<Drawable>() {

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                viewWeakReference.get()?.let { view ->
                    if (animate && resource is Animatable) {
                        val callback = resource.callback

                        resource.callback = object : Drawable.Callback {
                            override fun unscheduleDrawable(p0: Drawable, p1: Runnable) {
                                callback?.unscheduleDrawable(p0, p1)
                            }

                            override fun scheduleDrawable(p0: Drawable, p1: Runnable, p2: Long) {
                                callback?.scheduleDrawable(p0, p1, p2)
                            }

                            override fun invalidateDrawable(p0: Drawable) {
                                callback?.invalidateDrawable(p0)
                                view.invalidate()
                            }
                        }

                        resource.start()
                    }

                    imageDrawable = resource
                    view.invalidate()
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        }
    }
}

class SmallEmojiSpan(viewWeakReference: WeakReference<View>, aspectRatio: Double) : EmojiSpan(
    viewWeakReference, aspectRatio
) {

    override fun getSize(
        paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?
    ): Int {
        if (fm != null) {
            // Update FontMetricsInt or otherwise span does not get drawn when
            // it covers the whole text
            val metrics = paint.fontMetricsInt
            fm.top = metrics.top
            fm.ascent = metrics.ascent
            fm.descent = metrics.descent
            fm.bottom = metrics.bottom
        }

        return paint.textSize.toInt()
    }
}

/**
 * Get the Mimetype fron the URL.
 *
 * @return MIME - The Mimetype.
 */
private fun getMimeType(url: String?): MIME {
    var type: String? = null
    val extension = MimeTypeMap.getFileExtensionFromUrl(url)
    if (extension != null) {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return MIME.getMime(type)
}

private enum class MIME(private val mimetype: String) {

    NONE(""),
    SVG("image/svg+xml");

    companion object {
        fun getMime(mime: String?): MIME {
            return when (mime) {
                SVG.mimetype -> SVG
                else -> NONE
            }
        }
    }
}
