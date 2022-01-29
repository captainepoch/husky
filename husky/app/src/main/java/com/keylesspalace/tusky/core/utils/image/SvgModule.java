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

package com.keylesspalace.tusky.core.utils.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.LibraryGlideModule;
import com.caverock.androidsvg.SVG;
import java.io.InputStream;

/**
 * Module for adding SVG decoding support to Glide.
 */
@GlideModule
public class SvgModule extends LibraryGlideModule {

    @Override
    public void registerComponents(
            @NonNull Context context,
            @NonNull Glide glide,
            @NonNull Registry registry) {
        registry.register(SVG.class, Drawable.class, new SvgDrawableTranscoder(context))
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }
}
