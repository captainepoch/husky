package com.keylesspalace.tusky.util

import android.text.SpannableStringBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF

@ConscryptMode(OFF)
@RunWith(AndroidJUnit4::class)
class SmartLengthInputFilterTest {

    @Test
    fun shouldNotTrimStatusWithLength0() {
        assertFalse(shouldTrimStatus(SpannableStringBuilder("")))
    }

    @Test
    fun shouldNotTrimStatusWithLength10() {
        assertFalse(shouldTrimStatus(SpannableStringBuilder("0123456789")))
    }

    @Test
    fun shouldNotTrimStatusWithLength500() {
        assertFalse(
            shouldTrimStatus(
                SpannableStringBuilder(
                    "u1Pc5TbDVYFnzIdqlQkb3xuZ2S61fFD1K4uh" +
                        "cb3q40dnELjAsWxnSH59jqly249Spr0Vod029zfwFHYQ0PkBCNQ7tuk90h6aY661RFC7v" +
                        "IKJna4yDYOBFjRR9u0CsUa6vlgEE5yUrk5LKn3bmnnzRCXmU6HyT2bFu256qoUWbmMQ6G" +
                        "FXUXjO28tru8Q3UiXKLgrotKdSHmmqPwQgtatbMykTW4RZdKTE46nzlbD3mXHdWQkf4uV" +
                        "PYhVT1CMvVbCPMaimfQ0xuU8CpxyVqA8a6lCL3YX9pNnZjD7DoCg2FCejANnjXsTF6vuq" +
                        "PSHjQZDjy696nSAFy95p9kBeJkc70fHzX5TcfUqSaNtvx3LUtpIkwh4q2EYmKISPsxlAN" +
                        "aspEMPuX6r9fSACiEwmHsitZkp4RMKZq5NqRsGPCiAXcNIN3jj9fCYVGxUwVxVeCescDG" +
                        "5naEEszIR3FT1RO4MSn9c2ZZi0UdLizd8ciJAIuwwmcVyYyyM4"
                )
            )
        )
    }

    @Test
    fun shouldNotTrimStatusWithLength666() {
        assertFalse(
            shouldTrimStatus(
                SpannableStringBuilder(
                    "hIAXqY7DYynQGcr3zxcjCjNZFcdwAzwnWvGSbnwXX5KlH" +
                        "NHONtT55rO3r2faeMRZLTG3JlOshq8M1mtLRn0Ca8M9w82nIjJDm1jspxhFc4uLFpOjb9" +
                        "m2BokgRftA8ihpv6wvMwF5Fg8V4qa8GcXcqt1q7S9g09S3PszCXG4wnrR6dp8GGc9TqVA" +
                        "rgmoLSc9EVREIRcLPdzkhV1WWM9ZWw7josT27BfBdMWk0ckQkClHAyqLtlKZ84WamxK2q" +
                        "3NtHR5gr7ohIjU8CZoKDjv1bA8ZI8wBesyOhqbmHf0Ltypq39WKZ63VTGSf5Dd9kuTEjl" +
                        "XJtxZD1DXH4FFplY45DH5WuQ61Ih5dGx0WFEEVb1L3aku3Ht8rKG7YUbOPeanGMBmeI9Y" +
                        "RdiD4MmuTUkJfVLkA9rrpRtiEYw8RS3Jf9iqDkTpES9aLQODMip5xTsT4liIcUbLo0Z1d" +
                        "NhHk7YKubigNQIm1mmh2iU3Q0ZEm8TraDpKu2o27gIwSKbAnTllrOokprPxWQWDVrN9bI" +
                        "iwGHzgTKPI5z8gUybaqewxUYe12GvxnzqpfPFvvHricyZAC9i6Fkil5VmFdae75tLFWRB" +
                        "fE8Wfep0dSjL751m2yzvzZTc6uZRTcUiipvl42DaY8Z5eG2b6xPVhvXshMORvHzwhJhPk"
                )
            )
        )
    }

    @Test
    fun shouldTrimStatusWithLength667() {
        assertTrue(
            shouldTrimStatus(
                SpannableStringBuilder(
                    "hIAXqY7DYynQGcr3zxcjCjNZFcdwAzwnWvlRhSbnwXX5K1" +
                        "NHONtT55rO3r2faeMRZLTG3JlOshq8M1mtLRn0Ca8M9w82nIjJDm1jspxhFc4uLFpOjb9" +
                        "Gm2BokgRftA8ihpv6wvMwF5Fg8V4qa8GcXcqt1q7S9g09S3PszCXG4wnrR6dp8GGc9TqV" +
                        "ArgmoLSc9EVREIRcLPdzkhV1WWM9ZWw7josT27BfBdMWk0ckQkClHAyqLtlKZ84WamxK2" +
                        "q3NtHR5gr7ohIjU8CZoKDjv1bA8ZI8wBesyOhqbmHf0Ltypq39WKZ63VTGSf5Dd9kuTEj" +
                        "XJtxZD1DXH4FFplY45DH5WuQ61Ih5dGx0WFEEVb1L3aku3Ht8rKG7YUbOPeanGMBmeI9Y" +
                        "diD4MmuTUkJfVLkA9rrpRtiEYw8RS3Jf9iqDkTpES9aLQODMip5xTsT4liIcUbLo0Z1dN" +
                        "Hk7YKubigNQIm1mmh2iU3Q0ZEm8TraDpKu2o27gIwSKbAnTllrOokprPxWQWDVrN9bIli" +
                        "wGHzgTKPI5z851m2yzvzZTc6uZRTcUiipvl42DaY8Z5eG2b6xPVhvXshMORvHzwhJhPkH" +
                        "gUybaqewxUYe12GvxnzqpfPFvvHricyZAC9i6Fkil5VmFdae75tLFWRBfE8Wfep0dSjL7"
                )
            )
        )
    }

    @Test
    fun shouldTrimStatusWithLength1000() {
        assertTrue(
            shouldTrimStatus(
                SpannableStringBuilder(
                    "u1Pc5TbDVYFnzIdqlQkb3xuZ2S61fFD1K4uhIKJna4yDYOBFj3UiXKLgrotKdSHNtvx3" +
                        "cb3q40dnELjAsWxnSH59jqly249Spr0Vod029zfwFHYQ0PkBCNQ7tuk90h6aY661RFC7v" +
                        "RR9u0CsUa6vlgEE5yUrk5LKn3bmnnzRCXmU6HyT2bFu256qoUWbmMQ6GFXUXjO28tru8Q" +
                        "mmqPwQgtatbMykTW4RZdKTE46nzlbD3mXHdWQkf4uVPYhVT1CMvVbCPMaimfQ0xuU8Cpx" +
                        "pNnZjD7DoCg2FCejANnjXsTF6vuqPSHjQZDjy696nSAFy95p9kBeJkc70fHzX5TcfUqSa" +
                        "Ntvx3LUtpIkwh4yVqA8a6lCL3YX9GxUwVxVeCescDG3UiXKLgrotKdSHLUtpIkwh43YX9" +
                        "q2EYmKISPsxlANaspEMPuX6r9fSACiEwmHsitZkp4RMKZq5NqRsGPCiAXcNIN3jj9fCYV" +
                        "5naEEszIR3FT1RO4MSn9c2ZZi0UdLizd8ciJAIuwwmcVyYyyM4yVqA8a6lCLGxUwVxVeC" +
                        "cb3q40dnELjAsWxnSH59jqly249Spr0Vod029zfwFHYQ0PkBCNQ7tuk90h6aY661RFC7v" +
                        "hIKJna4yDYOBFju1Pc5TbDVYFnzIdqlQkb3xuZ2S61fFD1K4uescDGmcVyYyyM4JAIuww" +
                        "RR9u0CsUa6vlgEE5yUrk5LKn3bmnnzRCXmU6HyT2bFu256qoUWbmMQ6GFXUXjO28tru8Q" +
                        "mmqPwQgtatbMykTW4RZdKTE46nzlbD3mXHdWQkf4uVPYhVT1CMvVbCPMaimfQ0xuU8Cpx" +
                        "pNnZjD7DoCg2FCejANnjXsTF6vuqPSHjQZDjy696nSAFy95p9kBeJkc70fHzX5TcfUqSa" +
                        "q2EYmKISPsxlANaspEMPuX6r9fSACiEwmHsitZkp4RMKZq5NqRsGPCiAXcNIN3jj9fCYV" +
                        "5naEEszIR3FT1RO4MSn9c2ZZi0UdLizd8ci"
                )
            )
        )
    }
}
