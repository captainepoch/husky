/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
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

package com.keylesspalace.tusky

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.bumptech.glide.Glide
import com.keylesspalace.tusky.core.extensions.getNonNullString
import com.keylesspalace.tusky.core.extensions.gone
import com.keylesspalace.tusky.core.extensions.isGone
import com.keylesspalace.tusky.core.extensions.isVisible
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.extensions.visible
import com.keylesspalace.tusky.databinding.ActivityLoginBinding
import com.keylesspalace.tusky.entity.AccessToken
import com.keylesspalace.tusky.entity.AppCredentials
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.ThemeUtils
import okhttp3.HttpUrl
import org.koin.android.ext.android.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class LoginActivity : BaseActivity() {

    private val binding by viewBinding(ActivityLoginBinding::inflate)
    private val mastodonApi: MastodonApi by inject()
    private lateinit var preferences: SharedPreferences

    private val oauthRedirectUri: String
        get() {
            val scheme = getString(R.string.oauth_scheme)
            val host = BuildConfig.APPLICATION_ID
            return "$scheme://$host/"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        if (savedInstanceState == null) {
            if (BuildConfig.CUSTOM_INSTANCE.isNotBlank() && !isAdditionalLogin()) {
                binding.domainEditText.setText(BuildConfig.CUSTOM_INSTANCE)
                binding.domainEditText.setSelection(BuildConfig.CUSTOM_INSTANCE.length)
            }

            binding.appNameEditText.setText(getString(R.string.app_name))
            binding.appNameEditText.setSelection(getString(R.string.app_name).length)

            binding.websiteEditText.setText(getString(R.string.tusky_website))
            binding.websiteEditText.setSelection(getString(R.string.tusky_website).length)
        }

        if (BuildConfig.CUSTOM_LOGO_URL.isNotBlank()) {
            Glide.with(this)
                .load(BuildConfig.CUSTOM_LOGO_URL)
                .placeholder(null)
                .into(binding.loginLogo)
        }

        preferences = getSharedPreferences(
            getString(R.string.preferences_file_key),
            Context.MODE_PRIVATE
        )

        binding.loginButton.setOnClickListener { onButtonClick() }
        binding.settingsButton.setOnClickListener { onSettingsButtonClick() }

        binding.whatsAnInstanceTextView.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setMessage(R.string.dialog_whats_an_instance)
                .setPositiveButton(R.string.action_close, null)
                .show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            textView?.movementMethod = LinkMovementMethod.getInstance()
        }

        if (isAdditionalLogin()) {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } else {
            binding.toolbar.gone()
        }
    }

    override fun requiresLogin(): Boolean {
        return false
    }

    override fun finish() {
        super.finish()
        if (isAdditionalLogin()) {
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onSettingsButtonClick() {
        if (binding.extendedSettings.isGone()) {
            binding.extendedSettings.visible()
        } else {
            binding.extendedSettings.gone()
        }
    }

    /**
     * Obtain the oauth client credentials for this app. This is only necessary the first time the
     * app is run on a given server instance. So, after the first authentication, they are
     * saved in SharedPreferences and every subsequent run they are simply fetched from there.
     */
    private fun onButtonClick() {
        binding.loginButton.isEnabled = false

        // Hack for Play Store releases
        val domainField = binding.domainEditText.text.toString()
        val domainSplit = domainField.split("@")
        val domain = if (domainSplit.isEmpty() || domainSplit.size < 2) {
            domainField
        } else {
            domainSplit[1]
        }

        val oauthDomain = canonicalizeDomain(domain)
        try {
            HttpUrl.Builder().host(oauthDomain).scheme("https").build()
        } catch (e: IllegalArgumentException) {
            setLoading(false)
            binding.domainTextInputLayout.error = getString(R.string.error_invalid_domain)
            return
        }

        val callback = object : Callback<AppCredentials> {
            override fun onResponse(
                call: Call<AppCredentials>,
                response: Response<AppCredentials>
            ) {
                if (!response.isSuccessful) {
                    binding.loginButton.isEnabled = true
                    binding.domainTextInputLayout.error =
                        getString(R.string.error_failed_app_registration)
                    setLoading(false)
                    Timber.e("App authentication failed ${response.message()}")
                    return
                }
                val credentials = response.body()
                val clientId = credentials!!.clientId
                val clientSecret = credentials.clientSecret

                preferences.edit()
                    .putString("domain", oauthDomain)
                    .putString("clientId", clientId)
                    .putString("clientSecret", clientSecret)
                    .apply()

                redirectUserToAuthorizeAndLogin(oauthDomain, clientId)
            }

            override fun onFailure(call: Call<AppCredentials>, t: Throwable) {
                binding.loginButton.isEnabled = true
                binding.domainTextInputLayout.error =
                    getString(R.string.error_failed_app_registration)
                setLoading(false)
                Timber.e(t.stackTraceToString())
            }
        }

        var appname = getString(R.string.app_name)
        var website = getString(R.string.tusky_website)
        if (binding.extendedSettings.isVisible()) {
            appname = binding.appNameEditText.text.toString()
            website = binding.websiteEditText.text.toString()
        }

        mastodonApi
            .authenticateApp(
                oauthDomain,
                appname,
                oauthRedirectUri,
                OAUTH_SCOPES,
                website
            ).enqueue(callback)

        setLoading(true)
    }

    private fun redirectUserToAuthorizeAndLogin(domain: String, clientId: String) {
        /* To authorize this app and log in it's necessary to redirect to the domain given,
         * login there, and the server will redirect back to the app with its response. */
        val endpoint = MastodonApi.ENDPOINT_AUTHORIZE
        val parameters = mapOf(
            "client_id" to clientId,
            "redirect_uri" to oauthRedirectUri,
            "response_type" to "code",
            "scope" to OAUTH_SCOPES
        )
        val url = "https://" + domain + endpoint + "?" + toQueryString(parameters)
        val uri = Uri.parse(url)
        if (!openInCustomTab(uri, this)) {
            val viewIntent = Intent(Intent.ACTION_VIEW, uri)
            if (viewIntent.resolveActivity(packageManager) != null) {
                startActivity(viewIntent)
            } else {
                binding.domainEditText.error = getString(R.string.error_no_web_browser_found)
                setLoading(false)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        /* Check if we are resuming during authorization by seeing if the intent contains the
         * redirect that was given to the server. If so, its response is here! */
        val uri = intent.data
        val redirectUri = oauthRedirectUri

        if (uri != null && uri.toString().startsWith(redirectUri)) {
            // This should either have returned an authorization code or an error.
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")

            /* restore variables from SharedPreferences */
            val domain = preferences.getNonNullString(DOMAIN, "")
            val clientId = preferences.getNonNullString(CLIENT_ID, "")
            val clientSecret = preferences.getNonNullString(CLIENT_SECRET, "")

            if (code != null && domain.isNotEmpty() && clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
                setLoading(true)
                /* Since authorization has succeeded, the final step to log in is to exchange
                 * the authorization code for an access token. */
                val callback = object : Callback<AccessToken> {
                    override fun onResponse(
                        call: Call<AccessToken>,
                        response: Response<AccessToken>
                    ) {
                        if (response.isSuccessful) {
                            onLoginSuccess(response.body()!!.accessToken, domain)
                        } else {
                            setLoading(false)
                            binding.domainTextInputLayout.error =
                                getString(R.string.error_retrieving_oauth_token)
                            Timber.e(
                                String.format(
                                    "%s %s",
                                    getString(R.string.error_retrieving_oauth_token),
                                    response.message()
                                )
                            )
                        }
                    }

                    override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                        setLoading(false)
                        binding.domainTextInputLayout.error =
                            getString(R.string.error_retrieving_oauth_token)
                        Timber.e(
                            String.format(
                                "%s %s",
                                getString(R.string.error_retrieving_oauth_token),
                                t.message
                            )
                        )
                    }
                }

                mastodonApi.fetchOAuthToken(
                    domain,
                    clientId,
                    clientSecret,
                    redirectUri,
                    code,
                    "authorization_code"
                ).enqueue(callback)
            } else if (error != null) {
                /* Authorization failed. Put the error response where the user can read it and they
                 * can try again. */
                setLoading(false)
                binding.domainTextInputLayout.error = getString(R.string.error_authorization_denied)
                Timber.e(
                    String.format(
                        "%s %s",
                        getString(R.string.error_authorization_denied),
                        error
                    )
                )
            } else {
                // This case means a junk response was received somehow.
                setLoading(false)
                binding.domainTextInputLayout.error =
                    getString(R.string.error_authorization_unknown)
            }
        } else {
            // first show or user cancelled login
            setLoading(false)
        }
    }

    private fun setLoading(loadingState: Boolean) {
        if (loadingState) {
            binding.loginLoadingLayout.visible()
            binding.loginInputLayout.gone()
        } else {
            binding.loginLoadingLayout.gone()
            binding.loginInputLayout.visible()
            binding.loginButton.isEnabled = true
        }
    }

    private fun isAdditionalLogin(): Boolean {
        return intent.getBooleanExtra(LOGIN_MODE, false)
    }

    private fun onLoginSuccess(accessToken: String, domain: String) {
        setLoading(true)

        accountManager.value.addAccount(accessToken, domain)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.explode, R.anim.explode)
    }

    companion object {
        private const val OAUTH_SCOPES = "read write follow"
        private const val LOGIN_MODE = "LOGIN_MODE"
        private const val DOMAIN = "domain"
        private const val CLIENT_ID = "clientId"
        private const val CLIENT_SECRET = "clientSecret"

        @JvmStatic
        fun getIntent(context: Context, mode: Boolean): Intent {
            val loginIntent = Intent(context, LoginActivity::class.java)
            loginIntent.putExtra(LOGIN_MODE, mode)
            return loginIntent
        }

        /** Make sure the user-entered text is just a fully-qualified domain name.  */
        private fun canonicalizeDomain(domain: String): String {
            // Strip any schemes out.
            var s = domain.replaceFirst("http://", "")
            s = s.replaceFirst("https://", "")
            // If a username was included (e.g. username@example.com), just take what's after the '@'.
            val at = s.lastIndexOf('@')
            if (at != -1) {
                s = s.substring(at + 1)
            }
            return s.trim { it <= ' ' }
        }

        /**
         * Chain together the key-value pairs into a query string, for either appending to a URL or
         * as the content of an HTTP request.
         */
        private fun toQueryString(parameters: Map<String, String>): String {
            val s = StringBuilder()
            var between = ""
            for ((key, value) in parameters) {
                s.append(between)
                s.append(Uri.encode(key))
                s.append("=")
                s.append(Uri.encode(value))
                between = "&"
            }
            return s.toString()
        }

        private fun openInCustomTab(uri: Uri, context: Context): Boolean {
            val toolbarColor = ThemeUtils.getColor(context, R.attr.colorSurface)
            val navigationbarColor = ThemeUtils.getColor(context, android.R.attr.navigationBarColor)
            val navigationbarDividerColor = ThemeUtils.getColor(context, R.attr.dividerColor)

            val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .setNavigationBarColor(navigationbarColor)
                .setNavigationBarDividerColor(navigationbarDividerColor)
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorSchemeParams)
                .build()

            try {
                customTabsIntent.launchUrl(context, uri)
            } catch (e: ActivityNotFoundException) {
                Timber.w("Activity was not found for intent $customTabsIntent")
                return false
            }

            return true
        }
    }
}
