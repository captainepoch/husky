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

package com.keylesspalace.tusky.core.crypto

import android.util.Base64
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom

object CryptoUtils {

    private fun getBase64Flags(): Int {
        return Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
    }

    fun generateECPair(ecCurve: String): CryptoECKeyPair {
        val generator = KeyPairGenerator.getInstance(
            CryptoConstants.GENERATOR_EC,
            BouncyCastleProvider.PROVIDER_NAME
        )
        val specCurve = ECNamedCurveTable.getParameterSpec(ecCurve)
        generator.initialize(specCurve)
        val keyPair = generator.genKeyPair()
        val encodedPub = Base64.encodeToString(
            (keyPair.public as ECPublicKey).q.getEncoded(false),
            getBase64Flags()
        )
        val encodedPriv = Base64.encodeToString(
            (keyPair.private as ECPrivateKey).d.toByteArray(),
            getBase64Flags()
        )
        return CryptoECKeyPair(encodedPub, encodedPriv)
    }

    fun getSecureRandomStringBase64(byteArrayLength: Int): String {
        val byteArray = ByteArray(byteArrayLength)
        SecureRandom.getInstance(CryptoConstants.SECURE_RANDOM_SHA1PRNG)
            .nextBytes(byteArray)
        return Base64.encodeToString(byteArray, getBase64Flags())
    }
}
