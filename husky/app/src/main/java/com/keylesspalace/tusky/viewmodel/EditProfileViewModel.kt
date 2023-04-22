/* Copyright 2018 Conny Duck
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

package com.keylesspalace.tusky.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.EditProfileActivity.Companion.AVATAR_SIZE
import com.keylesspalace.tusky.EditProfileActivity.Companion.HEADER_HEIGHT
import com.keylesspalace.tusky.EditProfileActivity.Companion.HEADER_WIDTH
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.ProfileEditedEvent
import com.keylesspalace.tusky.components.instance.InstanceInfo
import com.keylesspalace.tusky.components.instance.InstanceRepository
import com.keylesspalace.tusky.components.profile.domain.EditProfileRepository
import com.keylesspalace.tusky.core.extensions.cancelIfActive
import com.keylesspalace.tusky.core.functional.Either.Left
import com.keylesspalace.tusky.core.functional.Either.Right
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.StringField
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.Error
import com.keylesspalace.tusky.util.IOUtils
import com.keylesspalace.tusky.util.Loading
import com.keylesspalace.tusky.util.Resource
import com.keylesspalace.tusky.util.RxAwareViewModel
import com.keylesspalace.tusky.util.Success
import com.keylesspalace.tusky.util.getSampledBitmap
import com.keylesspalace.tusky.util.randomAlphanumericString
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

private const val HEADER_FILE_NAME = "header.png"
private const val AVATAR_FILE_NAME = "avatar.png"

class EditProfileViewModel(
    private val mastodonApi: MastodonApi,
    private val instanceRepository: InstanceRepository,
    private val editProfileRepository: EditProfileRepository,
    private val eventHub: EventHub
) : RxAwareViewModel() {

    private var updateCredentialsJob: Job? = null

    private val _profileData = MutableLiveData<Resource<Account>>()
    val profileData: LiveData<Resource<Account>>
        get() = _profileData

    val avatarData = MutableLiveData<Resource<Bitmap>>()
    val headerData = MutableLiveData<Resource<Bitmap>>()

    private val _saveData: MutableLiveData<Resource<Nothing>> = MutableLiveData<Resource<Nothing>>()
    val saveData
        get() = _saveData

    private val _instanceData = MutableStateFlow(InstanceInfo())
    val instanceData = _instanceData.asStateFlow()

    private var getInstanceInfoJob: Job? = null

    private var oldProfileData: Account? = null

    init {
        obtainInstance()
    }

    fun obtainProfile() {
        if (profileData.value == null || profileData.value is Error) {
            _profileData.postValue(Loading())

            mastodonApi.accountVerifyCredentials()
                .subscribe(
                    { profile ->
                        oldProfileData = profile
                        _profileData.postValue(Success(profile))
                    },
                    {
                        _profileData.postValue(Error())
                    }
                ).addTo(disposables)
        }
    }

    fun newAvatar(uri: Uri, context: Context) {
        val cacheFile = getCacheFileForName(context, AVATAR_FILE_NAME)

        resizeImage(uri, context, AVATAR_SIZE, AVATAR_SIZE, cacheFile, avatarData)
    }

    fun newHeader(uri: Uri, context: Context) {
        val cacheFile = getCacheFileForName(context, HEADER_FILE_NAME)

        resizeImage(uri, context, HEADER_WIDTH, HEADER_HEIGHT, cacheFile, headerData)
    }

    private fun resizeImage(
        uri: Uri,
        context: Context,
        resizeWidth: Int,
        resizeHeight: Int,
        cacheFile: File,
        imageLiveData: MutableLiveData<Resource<Bitmap>>
    ) {
        Single.fromCallable {
            val contentResolver = context.contentResolver
            val sourceBitmap = getSampledBitmap(contentResolver, uri, resizeWidth, resizeHeight)
                ?: throw Exception()

            // Do not upscale image if it is smaller than the desired size
            val bitmap =
                if (sourceBitmap.width <= resizeWidth && sourceBitmap.height <= resizeHeight) {
                    sourceBitmap
                } else {
                    Bitmap.createScaledBitmap(sourceBitmap, resizeWidth, resizeHeight, true)
                }

            if (!saveBitmapToFile(bitmap, cacheFile)) {
                throw Exception()
            }

            bitmap
        }.subscribeOn(Schedulers.io())
            .subscribe(
                {
                    imageLiveData.postValue(Success(it))
                },
                {
                    imageLiveData.postValue(Error())
                }
            ).addTo(disposables)
    }

    fun save(
        newDisplayName: String,
        newNote: String,
        newLocked: Boolean,
        newFields: List<StringField>,
        context: Context
    ) {
        updateCredentialsJob?.cancelIfActive()
        updateCredentialsJob = viewModelScope.launch(Dispatchers.IO) {
            if (_saveData.value is Loading || profileData.value !is Success) {
                return@launch
            }

            val displayName = if (oldProfileData?.displayName == newDisplayName) {
                null
            } else {
                newDisplayName.toRequestBody(MultipartBody.FORM)
            }

            val note = if (oldProfileData?.source?.note == newNote) {
                null
            } else {
                newNote.toRequestBody(MultipartBody.FORM)
            }

            val locked = if (oldProfileData?.locked == newLocked) {
                null
            } else {
                newLocked.toString().toRequestBody(MultipartBody.FORM)
            }

            val avatar = if (avatarData.value is Success && avatarData.value?.data != null) {
                val avatarBody = getCacheFileForName(
                    context,
                    AVATAR_FILE_NAME
                ).asRequestBody("image/png".toMediaTypeOrNull())
                MultipartBody.Part.createFormData(
                    "avatar",
                    randomAlphanumericString(12),
                    avatarBody
                )
            } else {
                null
            }

            val header = if (headerData.value is Success && headerData.value?.data != null) {
                val headerBody = getCacheFileForName(
                    context,
                    HEADER_FILE_NAME
                ).asRequestBody("image/png".toMediaTypeOrNull())
                MultipartBody.Part.createFormData(
                    "header",
                    randomAlphanumericString(12),
                    headerBody
                )
            } else {
                null
            }

            val cleanFieldsList = newFields.mapNotNull { value ->
                value.takeIf { it.name.isNotBlank() && it.value.isNotBlank() }
            }

            if (displayName == null &&
                note == null &&
                locked == null &&
                avatar == null &&
                header == null &&
                (oldProfileData?.source?.fields == cleanFieldsList)
            ) {
                // If nothing is changed, there is no need to make a network request
                _saveData.postValue(Success())
                return@launch
            }

            val fieldsMap = hashMapOf<String, RequestBody>()
            cleanFieldsList.forEachIndexed { index, stringField ->
                fieldsMap["fields_attributes[$index][name]"] = stringField.name.toRequestBody()
                fieldsMap["fields_attributes[$index][value]"] = stringField.value.toRequestBody()
            }

            if (fieldsMap.isEmpty()) {
                fieldsMap["fields_attributes[0][name]"] = "".toRequestBody()
                fieldsMap["fields_attributes[0][value]"] = "".toRequestBody()
            }

            editProfileRepository.accountUpdateCredentialsData(
                displayName,
                note,
                locked,
                avatar,
                header,
                fieldsMap
            ).onStart {
                _saveData.postValue(Loading())
            }.catch {
                _saveData.postValue(Error(errorMessage = it.message))
            }.collect { result ->
                when (result) {
                    is Right -> {
                        _saveData.postValue(Success())
                        eventHub.dispatch(ProfileEditedEvent(result.asRight()))
                    }
                    is Left -> {
                        _saveData.postValue(result.asLeft())
                    }
                }
            }
        }
    }

    // Cache activity state for rotation change
    fun updateProfile(
        newDisplayName: String,
        newNote: String,
        newLocked: Boolean,
        newFields: List<StringField>
    ) {
        if (profileData.value is Success) {
            val newProfileSource =
                profileData.value?.data?.source?.copy(note = newNote, fields = newFields)
            val newProfile = profileData.value?.data?.copy(
                displayName = newDisplayName,
                locked = newLocked,
                source = newProfileSource
            )

            _profileData.postValue(Success(newProfile))
        }
    }

    fun addField(name: String = "", value: String = "") {
        val fields = _profileData.value?.data?.source?.fields?.toMutableList()?.apply {
            add(StringField(name, value))
        }?.toList()

        val newProfile = _profileData.value?.data?.copy(
            source = _profileData.value?.data?.source?.copy(
                fields = fields
            )
        )
        _profileData.postValue(Success(newProfile))
    }

    fun updateFieldList(position: Int) {
        val fields = _profileData.value?.data?.source?.fields?.toMutableList()?.apply {
            if (size == 1) {
                removeAt(position)
                add(StringField("", ""))
            } else {
                removeAt(position)
            }
        }?.toList()

        val newProfile = _profileData.value?.data?.copy(
            source = _profileData.value?.data?.source?.copy(
                fields = fields
            )
        )
        _profileData.postValue(Success(newProfile))
    }

    private fun getCacheFileForName(context: Context, filename: String): File {
        return File(context.cacheDir, filename)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        val outputStream: OutputStream

        try {
            outputStream = FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            Timber.e("File not found saving the Bitmap: ${Log.getStackTraceString(e)}", e)

            return false
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        IOUtils.closeQuietly(outputStream)

        return true
    }

    private fun obtainInstance() {
        getInstanceInfoJob?.cancelIfActive()
        getInstanceInfoJob = viewModelScope.launch {
            instanceRepository.getInstanceInfo()
                .catch {
                    _instanceData.emit(
                        instanceRepository.getInstanceInfoDb().toInstanceInfo().apply {
                            isLoadingInfo = false
                        }
                    )
                }
                .collect { response ->
                    _instanceData.emit(
                        response.asRight().toInstanceInfo().apply {
                            isLoadingInfo = false
                        }
                    )
                }
        }
    }
}
