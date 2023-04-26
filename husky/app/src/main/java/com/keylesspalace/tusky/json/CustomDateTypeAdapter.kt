/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
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

/*
 * The original code of this class is at https://github.com/google/gson,
 * commit 1a2170b99c9d293e825bf8f511b191196326ea03 (1a2170b).
 *
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keylesspalace.tusky.json

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.JavaVersion
import com.google.gson.internal.PreJava9DateFormatProvider
import com.google.gson.internal.bind.util.ISO8601Utils
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonWriter
import timber.log.Timber
import java.text.DateFormat
import java.text.ParseException
import java.text.ParsePosition
import java.util.Date
import java.util.Locale

/**
 * Adapter for Date. Although this class appears stateless, it is not.
 * DateFormat captures its time zone and locale when it is created, which gives
 * this class state. DateFormat isn't thread safe either, so this class has
 * to synchronize its read and write methods.
 */
class CustomDateTypeAdapter : TypeAdapter<Date>() {

    companion object {
        val FACTORY: TypeAdapterFactory = object : TypeAdapterFactory {

            @SuppressWarnings("unchecked")
            override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
                return if (typeToken.rawType == Date::class.java) {
                    (CustomDateTypeAdapter() as TypeAdapter<T>)
                } else {
                    null
                }
            }
        }
    }

    private val dateFormats = arrayListOf<DateFormat>()

    init {
        dateFormats.add(
            DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT,
                DateFormat.DEFAULT,
                Locale.US
            )
        )

        if (!Locale.getDefault().equals(Locale.US)) {
            dateFormats.add(
                DateFormat.getDateTimeInstance(
                    DateFormat.DEFAULT,
                    DateFormat.DEFAULT
                )
            )
        }

        if (JavaVersion.isJava9OrLater()) {
            dateFormats.add(
                PreJava9DateFormatProvider.getUSDateTimeFormat(
                    DateFormat.DEFAULT,
                    DateFormat.DEFAULT
                )
            )
        }
    }

    override fun read(reader: JsonReader?): Date? {
        if (reader?.peek() == NULL) {
            reader.nextNull()
            return null
        }
        return deserializeToDate(reader)
    }

    override fun write(writer: JsonWriter?, value: Date?) {
        if (value == null) {
            writer?.nullValue()
            return
        }

        val dateFormat = dateFormats[0]
        var dateFormatAsString: String?
        synchronized(dateFormats) {
            dateFormatAsString = dateFormat.format(value)
        }
        writer?.value(dateFormatAsString)
    }

    private fun deserializeToDate(reader: JsonReader?): Date? {
        val s: String? = reader?.nextString()
        synchronized(dateFormats) {
            for (dateFormat in dateFormats) {
                try {
                    s?.let {
                        return dateFormat.parse(it)
                    }
                } catch (ignored: ParseException) {
                }
            }
        }

        return runCatching {
            ISO8601Utils.parse(s, ParsePosition(0))
        }.getOrElse { e ->
            Timber.e("Failed parsing '$s' as Date; at path ${reader?.path}", e)

            null
        }
    }
}
