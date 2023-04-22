/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
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

package com.keylesspalace.tusky.components.profile.domain

import com.keylesspalace.tusky.core.extensions.notNull
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.core.functional.Either.Left
import com.keylesspalace.tusky.core.functional.Either.Right
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.network.MastodonService
import com.keylesspalace.tusky.util.Error
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber

class EditProfileRepository(
    private val service: MastodonService
) {

    suspend fun accountUpdateCredentialsData(
        displayName: RequestBody?,
        note: RequestBody?,
        locked: RequestBody?,
        avatar: MultipartBody.Part?,
        header: MultipartBody.Part?,
        fields_attributes: HashMap<String, RequestBody>?
    ): Flow<Either<Error<Nothing>, Account>> = flow {
        service.accountUpdateCredentialsData(
            displayName,
            note,
            locked,
            avatar,
            header,
            fields_attributes
        ).run {
            if (isSuccessful && body().notNull()) {
                Timber.d("Body: [${body()!!}]")
                emit(Right(body()!!))
            } else {
                Timber.e("Error: ${errorBody()?.string()}")
                emit(Left(Error(errorMessage = errorBody()?.string())))
            }
        }
    }
}
