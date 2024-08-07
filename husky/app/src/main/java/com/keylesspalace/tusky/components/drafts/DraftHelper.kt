/* Copyright 2021 Tusky Contributors
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
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.drafts

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.db.DraftAttachment
import com.keylesspalace.tusky.db.DraftEntity
import com.keylesspalace.tusky.entity.NewPoll
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.util.IOUtils
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DraftHelper(private val context: Context, private val db: AppDatabase) {

    private val draftDao = db.draftDao()

    fun saveDraft(
        draftId: Int,
        accountId: Long,
        inReplyToId: String?,
        content: String?,
        contentWarning: String?,
        sensitive: Boolean,
        visibility: Status.Visibility,
        mediaUris: List<String>,
        mediaDescriptions: List<String?>,
        poll: NewPoll?,
        formattingSyntax: String,
        failedToSend: Boolean,
        quoteId: String?
    ): Completable {
        return Single.fromCallable {
            val draftDirectory = context.getExternalFilesDir("Tusky")

            if (draftDirectory == null || !(draftDirectory.exists())) {
                Log.e("DraftHelper", "Error obtaining directory to save media.")
                throw Exception()
            }

            val uris = mediaUris.map { uriString ->
                uriString.toUri()
            }.map { uri ->
                if (uri.isNotInFolder(draftDirectory)) {
                    uri.copyToFolder(draftDirectory)
                } else {
                    uri
                }
            }

            val types = uris.map { uri ->
                val mimeType = context.contentResolver.getType(uri)
                when (mimeType?.substring(0, mimeType.indexOf('/'))) {
                    "video" -> DraftAttachment.Type.VIDEO
                    "image" -> DraftAttachment.Type.IMAGE
                    "audio" -> DraftAttachment.Type.AUDIO
                    else -> throw IllegalStateException("unknown media type")
                }
            }

            val attachments: MutableList<DraftAttachment> = mutableListOf()
            for (i in mediaUris.indices) {
                attachments.add(
                    DraftAttachment(
                        uriString = uris[i].toString(),
                        description = mediaDescriptions[i],
                        type = types[i]
                    )
                )
            }

            DraftEntity(
                id = draftId,
                accountId = accountId,
                inReplyToId = inReplyToId,
                content = content,
                contentWarning = contentWarning,
                sensitive = sensitive,
                visibility = visibility,
                attachments = attachments,
                poll = poll,
                formattingSyntax = formattingSyntax,
                failedToSend = failedToSend,
                quoteId = quoteId
            )
        }.flatMapCompletable { draft ->
            draftDao.insertOrReplace(draft)
        }.subscribeOn(Schedulers.io())
    }

    fun deleteDraftAndAttachments(draftId: Int): Completable {
        return draftDao.find(draftId).flatMapCompletable { draft ->
            deleteDraftAndAttachments(draft)
        }
    }

    fun deleteDraftAndAttachments(draft: DraftEntity): Completable {
        return deleteAttachments(draft).andThen(draftDao.delete(draft.id))
    }

    fun deleteAttachments(draft: DraftEntity): Completable {
        return Completable.fromCallable {
            draft.attachments.forEach { attachment ->
                if (context.contentResolver.delete(attachment.uri, null, null) == 0) {
                    Log.e("DraftHelper", "Did not delete file ${attachment.uriString}")
                }
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun Uri.isNotInFolder(folder: File): Boolean {
        val filePath = path ?: return true
        return File(filePath).parentFile == folder
    }

    private fun Uri.copyToFolder(folder: File): Uri {
        val contentResolver = context.contentResolver

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val mimeType = contentResolver.getType(this)
        val map = MimeTypeMap.getSingleton()
        val fileExtension = map.getExtensionFromMimeType(mimeType)

        val filename = String.format("Tusky_Draft_Media_%s.%s", timeStamp, fileExtension)
        val file = File(folder, filename)
        IOUtils.copyToFile(contentResolver, this, file)
        return FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file
        )
    }
}
