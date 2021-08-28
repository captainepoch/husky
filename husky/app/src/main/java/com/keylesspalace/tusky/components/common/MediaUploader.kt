/* Copyright 2019 Tusky Contributors
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

package com.keylesspalace.tusky.components.common

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.compose.ComposeActivity.QueuedMedia
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.network.ProgressRequestBody
import com.keylesspalace.tusky.util.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

sealed class UploadEvent {
    data class ProgressEvent(val percentage: Int) : UploadEvent()
    data class FinishedEvent(val attachment: Attachment) : UploadEvent()
}

fun createNewImageFile(context: Context, name: String = "Photo"): File {
    // Create an image file name
    val randomId = randomAlphanumericString(4)
    val imageFileName = "${name}_${randomId}"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
    )
}

data class PreparedMedia(val type: Int, val uri: Uri, val size: Long)

interface MediaUploader {
    fun prepareMedia(inUri: Uri, videoLimit: Long, imageLimit: Long, filename: String?): Single<PreparedMedia>
    fun uploadMedia(media: QueuedMedia, videoLimit: Long, imageLimit: Long): Observable<UploadEvent>
}

class AudioSizeException : Exception()
class VideoSizeException : Exception()
class MediaSizeException : Exception()
class MediaTypeException : Exception()
class CouldNotOpenFileException : Exception()

class MediaUploaderImpl(
        private val context: Context,
        private val mastodonApi: MastodonApi
) : MediaUploader {
    override fun uploadMedia(media: QueuedMedia, videoLimit: Long, imageLimit: Long): Observable<UploadEvent> {
        return Observable
                .fromCallable {
                    if (shouldResizeMedia(media, imageLimit)) {
                        downsize(media, imageLimit)
                    } else media
                }
                .switchMap { upload(it) }
                .subscribeOn(Schedulers.io())
    }

    private fun getMimeTypeAndSuffixFromFilenameOrUri(uri: Uri, filename: String?) : Pair<String?, String> {
        val mimeType = contentResolver.getType(uri)
        return if(mimeType == null && filename != null) {
            val extension = filename.substringAfterLast('.', "tmp")
            Pair(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension), ".$extension")
        } else {
            Pair(mimeType, "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType ?: "tmp"))
        }
    }

	override fun prepareMedia(inUri: Uri, videoLimit: Long, imageLimit: Long, filename: String?): Single<PreparedMedia> {
        return Single.fromCallable {
            var mediaSize = getMediaSize(contentResolver, inUri)
            var uri = inUri
            val (mimeType, suffix) = getMimeTypeAndSuffixFromFilenameOrUri(uri, filename)

            try {
                contentResolver.openInputStream(inUri).use { input ->
                    if (input == null) {
                        Log.w(TAG, "Media input is null")
                        uri = inUri
                        return@use
                    }
                    val file = File.createTempFile("randomTemp1", suffix, context.cacheDir)
                    FileOutputStream(file.absoluteFile).use { out ->
                        input.copyTo(out)
                        uri = FileProvider.getUriForFile(context,
                                BuildConfig.APPLICATION_ID + ".fileprovider",
                                file)
                        mediaSize = getMediaSize(contentResolver, uri)
                    }

                }
            } catch (e: IOException) {
                Log.w(TAG, e)
                uri = inUri
            }
            if (mediaSize == MEDIA_SIZE_UNKNOWN) {
                throw CouldNotOpenFileException()
            }

            if (mimeType != null) {
                val topLevelType = mimeType.substring(0, mimeType.indexOf('/'))
                when (topLevelType) {
                    "video" -> {
                        if (mediaSize > videoLimit) {
                            throw VideoSizeException()
                        }
                        PreparedMedia(QueuedMedia.VIDEO, uri, mediaSize)
                    }
                    "image" -> {
                        PreparedMedia(QueuedMedia.IMAGE, uri, mediaSize)
                    }
                    "audio" -> {
                        if (mediaSize > videoLimit) { // TODO: CHANGE!!11
                            throw AudioSizeException()
                        }
                        PreparedMedia(QueuedMedia.AUDIO, uri, mediaSize)
                    }
                    else -> {
                        if (mediaSize > videoLimit) {
                            throw MediaSizeException()
                        }
                        PreparedMedia(QueuedMedia.UNKNOWN, uri, mediaSize)
                        // throw MediaTypeException()
                    }
                }
            } else {
                throw MediaTypeException()
            }
        }
    }

    private val contentResolver = context.contentResolver
    
    private fun upload(media: QueuedMedia): Observable<UploadEvent> {
        return Observable.create { emitter ->
            var (mimeType, fileExtension) = getMimeTypeAndSuffixFromFilenameOrUri(media.uri, media.originalFileName)
            val filename = if(!media.anonymizeFileName) media.originalFileName else
                String.format("%s_%s_%s%s",
                        context.getString(R.string.app_name),
                        Date().time.toString(),
                        randomAlphanumericString(10),
                        fileExtension)

            val stream = contentResolver.openInputStream(media.uri)

            if (mimeType == null) mimeType = "multipart/form-data"

            var lastProgress = -1
            val fileBody = ProgressRequestBody(stream, media.mediaSize,
                    mimeType.toMediaTypeOrNull()) { percentage ->
                if (percentage != lastProgress) {
                    emitter.onNext(UploadEvent.ProgressEvent(percentage))
                }
                lastProgress = percentage
            }

            val body = MultipartBody.Part.createFormData("file", filename, fileBody)

            val description = if (media.description != null) {
                MultipartBody.Part.createFormData("description", media.description)
            } else {
                null
            }

            val uploadDisposable = mastodonApi.uploadMedia(body, description)
                    .subscribe({ attachment ->
                        emitter.onNext(UploadEvent.FinishedEvent(attachment))
                        emitter.onComplete()
                    }, { e ->
                        emitter.onError(e)
                    })

            // Cancel the request when our observable is cancelled
            emitter.setDisposable(uploadDisposable)
        }
    }

    private fun downsize(media: QueuedMedia, imageLimit: Long): QueuedMedia {
        val file = createNewImageFile(context, media.originalFileName)
        DownsizeImageTask.resize(arrayOf(media.uri), imageLimit, context.contentResolver, file)
        return media.copy(uri = file.toUri(), mediaSize = file.length())
    }

    private fun shouldResizeMedia(media: QueuedMedia, imageLimit: Long): Boolean {
        // resize only images 
        if(media.type == QueuedMedia.Type.IMAGE) {
            // resize when exceed image limit
            if(media.mediaSize >= imageLimit)
                return true
            
            // don't resize when instance permits any image resolution(Pleroma)
            if(media.noChanges)
                return false
            
            // resize when exceed pixel limit
            if(getImageSquarePixels(context.contentResolver, media.uri) > STATUS_IMAGE_PIXEL_SIZE_LIMIT)
                return true
        }
        
        return false
    }

    private companion object {
        private const val TAG = "MediaUploaderImpl"
        private const val STATUS_IMAGE_PIXEL_SIZE_LIMIT = 16777216 // 4096^2 Pixels
    }
}

fun Uri.toFileName(contentResolver: ContentResolver? = null): String {
    var result: String = "unknown"

    if(scheme.equals("content") && contentResolver != null) {
        val cursor = contentResolver.query(this, null, null, null, null)
        cursor?.use{
            if(it.moveToFirst()) {
                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }

    if(result.equals("unknown")) {
        path?.let {
            result = it
            val cut = result.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
    }
    return result
}