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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.adapter.AccountFieldEditAdapter
import com.keylesspalace.tusky.adapter.MutableStringPair
import com.keylesspalace.tusky.components.instance.InstanceInfo
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.extensions.viewObserve
import com.keylesspalace.tusky.databinding.ActivityEditProfileBinding
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.util.Error
import com.keylesspalace.tusky.util.Loading
import com.keylesspalace.tusky.util.Resource
import com.keylesspalace.tusky.util.Success
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.show
import com.keylesspalace.tusky.viewmodel.EditProfileViewModel
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class EditProfileActivity : BaseActivity() {

    private val binding by viewBinding(ActivityEditProfileBinding::inflate)
    private val viewModel: EditProfileViewModel by viewModel()
    private var currentlyPicking: PickType = PickType.NOTHING
    private val accountFieldEditAdapter = AccountFieldEditAdapter()

    private enum class PickType {
        NOTHING,
        AVATAR,
        HEADER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getString(BUNDLE_CURRENTLY_PICKING)?.let {
            currentlyPicking = PickType.valueOf(it)
        }

        setContentView(binding.root)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.run {
            setTitle(R.string.title_edit_profile)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        setupObservers()

        binding.avatarButton.setOnClickListener { onMediaPick(PickType.AVATAR) }
        binding.headerButton.setOnClickListener { onMediaPick(PickType.HEADER) }

        binding.fieldList.layoutManager = LinearLayoutManager(this)
        binding.fieldList.adapter = accountFieldEditAdapter

        binding.addFieldButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
            IconicsDrawable(this, GoogleMaterial.Icon.gmd_add).apply {
                sizeDp = 12; colorInt = Color.WHITE
            },
            null,
            null,
            null
        )

        binding.addFieldButton.setOnClickListener {
            viewModel.addField()
            if (accountFieldEditAdapter.itemCount >= viewModel.instanceData.value.maxBioFields) {
                it.isEnabled = false
            }

            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, it.bottom)
            }
        }
    }

    private fun setupObservers() {
        accountFieldEditAdapter.clearFieldListener = {
            Timber.d("Delete item $it")
            viewModel.updateFieldList(it)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                with(viewModel) {
                    viewObserve(profileData, ::handleProfileData)
                    viewObserve(saveData, ::handleSaveData)

                    observeImage(
                        avatarData,
                        binding.avatarPreview,
                        binding.avatarProgressBar,
                        true
                    )
                    observeImage(
                        headerData,
                        binding.headerPreview,
                        binding.headerProgressBar,
                        false
                    )

                    instanceData.collect { instanceInfo ->
                        handleInstanceInfo(instanceInfo)
                    }
                }
            }
        }
    }

    private fun handleProfileData(result: Resource<Account>?) {
        result?.let { profile ->
            when (profile) {
                is Success -> {
                    val me = profile.data
                    if (me != null) {
                        binding.displayNameEditText.setText(me.displayName)
                        binding.noteEditText.setText(me.source?.note)
                        binding.lockedCheckBox.isChecked = me.locked

                        me.source?.fields?.forEach {
                            Timber.d("F ${it.name} D ${it.value}")
                        }

                        accountFieldEditAdapter.submitList(
                            me.source?.fields?.map {
                                MutableStringPair(it.name, it.value)
                            } ?: emptyList()
                        )
                        // accountFieldEditAdapter.setFields()
                        binding.addFieldButton.isEnabled =
                            (
                            me.source?.fields?.size
                                ?: 0
                            ) < viewModel.instanceData.value.maxBioFields

                        if (viewModel.avatarData.value == null) {
                            Glide.with(this)
                                .load(me.avatar)
                                .placeholder(R.drawable.avatar_default)
                                .transform(
                                    FitCenter(),
                                    RoundedCorners(resources.getDimensionPixelSize(R.dimen.avatar_radius_80dp))
                                ).into(binding.avatarPreview)
                        }

                        if (viewModel.headerData.value == null) {
                            Glide.with(this)
                                .load(me.header)
                                .into(binding.headerPreview)
                        }
                    }
                }

                is Error -> {
                    Snackbar.make(
                        binding.avatarButton,
                        R.string.error_generic,
                        Snackbar.LENGTH_LONG
                    ).apply {
                        setAction(R.string.action_retry) {
                            viewModel.obtainProfile()
                        }
                    }.show()
                }

                else -> Unit
            }
        }
    }

    private fun handleSaveData(result: Resource<Nothing>?) {
        result?.let { resource ->
            when (resource) {
                is Success -> {
                    finish()
                }

                is Loading -> {
                    binding.saveProgressBar.visibility = View.VISIBLE
                }

                is Error -> {
                    onSaveFailure(resource.errorMessage)
                }
            }
        }
    }

    private fun handleInstanceInfo(instanceInfo: InstanceInfo) {
        if (instanceInfo.isLoadingInfo.not()) {
            if (instanceInfo.maxBioLength > 0) {
                binding.noteEditTextLayout.counterMaxLength = instanceInfo.maxBioLength
            }

            viewModel.obtainProfile()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_CURRENTLY_PICKING, currentlyPicking.toString())
    }

    override fun onStop() {
        super.onStop()

        if (!isFinishing) {
            viewModel.updateProfile(
                binding.displayNameEditText.text.toString(),
                binding.noteEditText.text.toString(),
                binding.lockedCheckBox.isChecked,
                accountFieldEditAdapter.getFieldData()
            )
        }
    }

    private fun observeImage(
        liveData: LiveData<Resource<Bitmap>>,
        imageView: ImageView,
        progressBar: View,
        roundedCorners: Boolean
    ) {
        liveData.observe(this) {
            when (it) {
                is Success -> {
                    val glide = Glide.with(imageView)
                        .load(it.data)

                    if (roundedCorners) {
                        glide.transform(
                            FitCenter(),
                            RoundedCorners(resources.getDimensionPixelSize(R.dimen.avatar_radius_80dp))
                        )
                    }

                    glide.into(imageView)

                    imageView.show()
                    progressBar.hide()
                }

                is Loading -> {
                    progressBar.show()
                }

                is Error -> {
                    progressBar.hide()
                    if (!it.consumed) {
                        onResizeFailure()
                        it.consumed = true
                    }
                }
            }
        }
    }

    private fun onMediaPick(pickType: PickType) {
        if (currentlyPicking != PickType.NOTHING) {
            // Ignore inputs if another pick operation is still occurring.
            return
        }

        currentlyPicking = pickType
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        } else {
            initiateMediaPicking()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    initiateMediaPicking()
                } else {
                    endMediaPicking()
                    Snackbar.make(
                        binding.avatarButton,
                        R.string.error_media_upload_permission,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun initiateMediaPicking() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        when (currentlyPicking) {
            PickType.AVATAR -> {
                startActivityForResult(intent, AVATAR_PICK_RESULT)
            }

            PickType.HEADER -> {
                startActivityForResult(intent, HEADER_PICK_RESULT)
            }

            PickType.NOTHING -> {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_profile_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_save -> {
                save()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun save() {
        if (currentlyPicking != PickType.NOTHING) {
            return
        }

        viewModel.save(
            binding.displayNameEditText.text.toString(),
            binding.noteEditText.text.toString(),
            binding.lockedCheckBox.isChecked,
            accountFieldEditAdapter.getFieldData(),
            this
        )
    }

    private fun onSaveFailure(msg: String?) {
        val errorMsg = msg ?: getString(R.string.error_media_upload_sending)
        Snackbar.make(binding.avatarButton, errorMsg, Snackbar.LENGTH_LONG).show()
        binding.saveProgressBar.visibility = View.GONE
    }

    private fun beginMediaPicking() {
        when (currentlyPicking) {
            PickType.AVATAR -> {
                binding.avatarProgressBar.visibility = View.VISIBLE
                binding.avatarPreview.visibility = View.INVISIBLE
                binding.avatarButton.setImageDrawable(null)
            }

            PickType.HEADER -> {
                binding.headerProgressBar.visibility = View.VISIBLE
                binding.headerPreview.visibility = View.INVISIBLE
                binding.headerButton.setImageDrawable(null)
            }

            PickType.NOTHING -> Unit
        }
    }

    private fun endMediaPicking() {
        binding.avatarProgressBar.visibility = View.GONE
        binding.headerProgressBar.visibility = View.GONE

        currentlyPicking = PickType.NOTHING
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AVATAR_PICK_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CropImage.activity(data.data)
                        .setInitialCropWindowPaddingRatio(0f)
                        .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                        .setAspectRatio(AVATAR_SIZE, AVATAR_SIZE)
                        .start(this)
                } else {
                    endMediaPicking()
                }
            }

            HEADER_PICK_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CropImage.activity(data.data)
                        .setInitialCropWindowPaddingRatio(0f)
                        .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                        .setAspectRatio(HEADER_WIDTH, HEADER_HEIGHT)
                        .start(this)
                } else {
                    endMediaPicking()
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                when (resultCode) {
                    Activity.RESULT_OK -> beginResize(result.uri)
                    CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> onResizeFailure()
                    else -> endMediaPicking()
                }
            }
        }
    }

    private fun beginResize(uri: Uri) {
        beginMediaPicking()

        when (currentlyPicking) {
            PickType.AVATAR -> {
                viewModel.newAvatar(uri, this)
            }

            PickType.HEADER -> {
                viewModel.newHeader(uri, this)
            }

            else -> {
                throw AssertionError("PickType not set.")
            }
        }

        currentlyPicking = PickType.NOTHING
    }

    private fun onResizeFailure() {
        Snackbar.make(
            binding.avatarButton,
            R.string.error_media_upload_sending,
            Snackbar.LENGTH_LONG
        ).show()

        endMediaPicking()
    }

    companion object {
        const val AVATAR_SIZE = 400
        const val HEADER_WIDTH = 1500
        const val HEADER_HEIGHT = 500

        private const val AVATAR_PICK_RESULT = 1
        private const val HEADER_PICK_RESULT = 2
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1

        private const val BUNDLE_CURRENTLY_PICKING = "BUNDLE_CURRENTLY_PICKING"
    }
}
