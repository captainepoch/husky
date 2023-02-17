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
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.transition.Transition
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.BuildConfig.APPLICATION_ID
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityViewMediaBinding
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.fragment.ViewImageFragment
import com.keylesspalace.tusky.pager.AvatarImagePagerAdapter
import com.keylesspalace.tusky.pager.ImagePagerAdapter
import com.keylesspalace.tusky.util.getTemporaryMediaFilename
import com.keylesspalace.tusky.viewdata.AttachmentViewData
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import timber.log.Timber

typealias ToolbarVisibilityListener = (isVisible: Boolean) -> Unit

class ViewMediaActivity : BaseActivity(), ViewImageFragment.PhotoActionsListener {

    companion object {
        private const val EXTRA_ATTACHMENTS = "attachments"
        private const val EXTRA_ATTACHMENT_INDEX = "index"
        private const val EXTRA_AVATAR_URL = "avatar"
        private const val TAG = "ViewMediaActivity"

        @JvmStatic
        fun newIntent(
            context: Context?,
            attachments: List<AttachmentViewData>,
            index: Int
        ): Intent {
            val intent = Intent(context, ViewMediaActivity::class.java)
            intent.putParcelableArrayListExtra(EXTRA_ATTACHMENTS, ArrayList(attachments))
            intent.putExtra(EXTRA_ATTACHMENT_INDEX, index)
            return intent
        }

        @JvmStatic
        fun newIntent(context: Context?, attachment: Attachment): Intent {
            val intent = Intent(context, ViewMediaActivity::class.java)
            intent.putParcelableArrayListExtra(
                EXTRA_ATTACHMENTS,
                arrayListOf(AttachmentViewData(attachment, null, null))
            )
            intent.putExtra(EXTRA_ATTACHMENT_INDEX, 0)
            return intent
        }

        fun newSingleImageIntent(context: Context?, url: String): Intent {
            val intent = Intent(context, ViewMediaActivity::class.java)
            intent.putExtra(EXTRA_AVATAR_URL, url)
            return intent
        }
    }

    private val binding by viewBinding(ActivityViewMediaBinding::inflate)

    private var attachments: ArrayList<AttachmentViewData>? = null
    private val toolbarVisibilityListeners = mutableListOf<ToolbarVisibilityListener>()
    private var avatarUrl: String? = null

    var isToolbarVisible = true
        private set

    fun addToolbarVisibilityListener(listener: ToolbarVisibilityListener): Function0<Boolean> {
        this.toolbarVisibilityListeners.add(listener)
        listener(isToolbarVisible)
        return { toolbarVisibilityListeners.remove(listener) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportPostponeEnterTransition()

        // Gather the parameters.
        attachments = intent.getParcelableArrayListExtra(EXTRA_ATTACHMENTS)
        val initialPosition = intent.getIntExtra(EXTRA_ATTACHMENT_INDEX, 0)

        // Adapter is actually of existential type PageAdapter & SharedElementsTransitionListener
        // but it cannot be expressed and if I don't specify type explicitly compilation fails
        // (probably a bug in compiler)
        val adapter: ViewMediaAdapter = if (attachments != null) {
            val realAttachs = attachments!!.map(AttachmentViewData::attachment)
            // Setup the view pager.
            ImagePagerAdapter(this, realAttachs, initialPosition)
        } else {
            avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL)
                ?: throw IllegalArgumentException("attachment list or avatar url has to be set")

            AvatarImagePagerAdapter(this, avatarUrl!!)
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(initialPosition, false)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.toolbar.title = getPageTitle(position)
            }
        })

        // Setup the toolbar.
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.title = getPageTitle(initialPosition)
        }
        binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        binding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_open_in_external_app -> openInExternalApp()
                R.id.action_download -> requestDownloadMedia()
                R.id.action_open_status -> onOpenStatus()
                R.id.action_share_media -> shareMedia()
                R.id.action_copy_media_link -> copyLink()
            }
            true
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        window.statusBarColor = Color.BLACK
        window.sharedElementEnterTransition.addListener(object : NoopTransitionListener {
            override fun onTransitionEnd(transition: Transition) {
                adapter.onTransitionEnd(binding.viewPager.currentItem)
                window.sharedElementEnterTransition.removeListener(this)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.view_media_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_share_media)?.isEnabled = !isCreating
        return true
    }

    override fun onBringUp() {
        supportStartPostponedEnterTransition()
    }

    override fun onDismiss() {
        supportFinishAfterTransition()
    }

    override fun onPhotoTap() {
        isToolbarVisible = !isToolbarVisible
        for (listener in toolbarVisibilityListeners) {
            listener(isToolbarVisible)
        }

        val visibility = if (isToolbarVisible) View.VISIBLE else View.INVISIBLE
        val alpha = if (isToolbarVisible) 1.0f else 0.0f
        if (isToolbarVisible) {
            // If to be visible, need to make visible immediately and animate alpha
            binding.toolbar.alpha = 0.0f
            binding.toolbar.visibility = visibility
        }

        binding.toolbar.animate().alpha(alpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.toolbar.visibility = visibility
                    animation.removeListener(this)
                }
            })
            .start()
    }

    fun getToolbar(): Toolbar {
        return binding.toolbar
    }

    private fun getPageTitle(position: Int): CharSequence {
        if (attachments == null) {
            return ""
        }
        return String.format(Locale.getDefault(), "%d/%d", position + 1, attachments?.size)
    }

    private fun downloadMedia() {
        val url = avatarUrl ?: attachments!![binding.viewPager.currentItem].attachment.url
        val filename = Uri.parse(url).lastPathSegment
        Toast.makeText(
            applicationContext,
            resources.getString(R.string.download_image, filename),
            Toast.LENGTH_SHORT
        ).show()

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_PICTURES,
            getString(R.string.app_name) + "/" + filename
        )
        downloadManager.enqueue(request)
    }

    private fun requestDownloadMedia() {
        if (VERSION.SDK_INT < VERSION_CODES.Q) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)) { _, grantResults ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadMedia()
                } else {
                    showErrorDialog(
                        binding.toolbar,
                        R.string.error_media_download_permission,
                        R.string.action_retry
                    ) { requestDownloadMedia() }
                }
            }
        } else {
            downloadMedia()
        }
    }

    private fun onOpenStatus() {
        if (attachments != null) {
            val attach = attachments!![binding.viewPager.currentItem]
            startActivityWithSlideInAnimation(ViewThreadActivity.startIntent(this, attach.statusId, attach.statusUrl))
        } else {
            Snackbar.make(
                binding.root,
                "You're viewing an image without status.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun copyLink() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                null,
                attachments!![binding.viewPager.currentItem].attachment.url
            )
        )
    }

    private fun shareMedia() {
        val directory = applicationContext.getExternalFilesDir("Husky")
        if (directory == null || !(directory.exists())) {
            Timber.e("Error obtaining directory to save temporary media")
            return
        }

        val attachment = attachments!![binding.viewPager.currentItem].attachment
        when (attachment.type) {
            Attachment.Type.IMAGE -> shareImage(directory, attachment.url)
            Attachment.Type.AUDIO,
            Attachment.Type.VIDEO,
            Attachment.Type.GIFV -> shareMediaFile(directory, attachment.url)
            else -> Timber.e("Unknown media format to share")
        }
    }

    private fun shareFile(file: File, mimeType: String?) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                applicationContext, "$APPLICATION_ID.fileprovider", file
            )
        )
        sendIntent.type = mimeType
        startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.send_media_to)))
    }

    private fun openInExternalApp() {
        val url = avatarUrl ?: attachments!![binding.viewPager.currentItem].attachment.url
        val intent = Intent(Intent.ACTION_VIEW)
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            intent.setDataAndType(
                Uri.parse(url),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            )
        } else {
            intent.data = Uri.parse(url)
        }

        startActivity(intent)
    }

    private var isCreating: Boolean = false

    private fun shareImage(directory: File, url: String) {
        isCreating = true
        binding.progressBarShare.visibility = View.VISIBLE
        invalidateOptionsMenu()
        val file = File(directory, getTemporaryMediaFilename("png"))
        val futureTask: FutureTarget<Bitmap> =
            Glide.with(applicationContext).asBitmap().load(Uri.parse(url)).submit()
        Single.fromCallable {
            val bitmap = futureTask.get()
            try {
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
                return@fromCallable true
            } catch (fnfe: FileNotFoundException) {
                Log.e(TAG, "Error writing temporary media.")
            } catch (ioe: IOException) {
                Log.e(TAG, "Error writing temporary media.")
            }
            return@fromCallable false
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose {
                futureTask.cancel(true)
            }
            .autoDispose(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_DESTROY))
            .subscribe(
                { result ->
                    Log.d(TAG, "Download image result: $result")
                    isCreating = false
                    invalidateOptionsMenu()
                    binding.progressBarShare.visibility = View.GONE
                    if (result)
                        shareFile(file, "image/png")
                },
                { error ->
                    isCreating = false
                    invalidateOptionsMenu()
                    binding.progressBarShare.visibility = View.GONE
                    Log.e(TAG, "Failed to download image", error)
                }
            )
    }

    private fun shareMediaFile(directory: File, url: String) {
        val uri = Uri.parse(url)
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        val mimeType = mimeTypeMap.getMimeTypeFromExtension(extension)
        val filename = getTemporaryMediaFilename(extension)
        val file = File(directory, filename)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
        request.setDestinationUri(Uri.fromFile(file))
        request.setVisibleInDownloadsUi(false)
        downloadManager.enqueue(request)

        shareFile(file, mimeType)
    }
}

abstract class ViewMediaAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    abstract fun onTransitionEnd(position: Int)
}

interface NoopTransitionListener : Transition.TransitionListener {
    override fun onTransitionEnd(transition: Transition) {
    }

    override fun onTransitionResume(transition: Transition) {
    }

    override fun onTransitionPause(transition: Transition) {
    }

    override fun onTransitionCancel(transition: Transition) {
    }

    override fun onTransitionStart(transition: Transition) {
    }
}
