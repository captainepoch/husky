package com.keylesspalace.tusky.core.functional

import com.keylesspalace.tusky.core.functional.CommonState.NetworkError
import com.keylesspalace.tusky.core.functional.CommonState.Nothing
import java.io.IOException

object ErrorMapper {

    fun networkErrorMapper(throwable: Throwable?): CommonState {
        return when (throwable) {
            is IOException -> NetworkError
            else -> Nothing
        }
    }
}
