package com.husky.project.features.login.view.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.extensions.addHttpsProtocolUrl
import com.keylesspalace.tusky.core.extensions.dialogWithLink
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.extensions.viewObserve
import com.husky.project.core.ui.fragment.BaseFragment
import com.keylesspalace.tusky.databinding.ActivityLoginBinding
import com.husky.project.features.login.view.viewmodel.LoginViewModel
import com.keylesspalace.tusky.util.afterTextChanged
import com.zhuinden.simplestackextensions.fragmentsktx.lookup

class LoginFragment : BaseFragment(R.layout.activity_login) {

    private val binding by viewBinding(ActivityLoginBinding::bind)
    private val viewModel by lazy { lookup<LoginViewModel>() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObservers()
        initListeners()
    }

    private fun initObservers() {
        with(viewModel) {
            viewObserve(verifyUrl, ::handleVerifyUrl)
        }
    }

    private fun initListeners() {
        binding.domainEditText.afterTextChanged {
            if(it.isEmpty()) {
                setDomainUrlError(false)
            }
        }

        binding.loginButton.setOnClickListener {
            viewModel.verifyUrl(binding.domainEditText.text.toString().addHttpsProtocolUrl())
        }

        binding.whatsAnInstanceTextView.setOnClickListener {
            AlertDialog.Builder(requireActivity()).dialogWithLink(
                getString(R.string.dialog_whats_an_instance),
                getString(R.string.action_close)
            )
        }
    }

    private fun handleVerifyUrl(isValid: Boolean?) {
        isValid?.let { valid ->
            if(valid == true) {
            }
            setDomainUrlError(isValid)
        }
    }

    private fun setDomainUrlError(isValid: Boolean) {
        binding.domainTextInputLayout.error = if(isValid) {
            null
        } else {
            getString(R.string.error_invalid_domain)
        }
    }
}
