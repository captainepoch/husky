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

package com.keylesspalace.tusky.components.unifiedpush

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.databinding.BottomSheetTwoOptionsBinding
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.entity.Notification.Type
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber

object UnifiedPushHelper {

    private fun hasUnifiedPushProviders(context: Context): Boolean {
        return UnifiedPush.getDistributors(context).isNotEmpty()
    }

    fun hasUnifiedPushEnrolled(account: AccountEntity): Boolean {
        return (account.unifiedPushUrl.isNotBlank() && account.unifiedPushInstance.isNotBlank())
    }

    fun enableUnifiedPushNotificationsForAccount(
        context: Context,
        account: AccountEntity?
    ) {
        Timber.d("Registering UnifiedPush for ${account?.username}")

        account?.let {
            NotificationHelper.createNotificationChannelsForAccount(account, context)

            UnifiedPush.registerAppWithDialog(
                context,
                it.id.toString(),
                features = arrayListOf(UnifiedPush.FEATURE_BYTES_MESSAGE)
            )
        } ?: Timber.e("Account cannot be null")
    }

    fun showUnifiedPushDialog(
        activity: Activity,
        onPositiveAction: (Boolean) -> Unit,
        onHideDialogAction: (Boolean) -> Unit
    ) {
        val hasProviders = hasUnifiedPushProviders(activity)
        val binding = BottomSheetTwoOptionsBinding.inflate(activity.layoutInflater)

        val message = SpannableStringBuilder()
            .append(activity.getString(R.string.unifiedpush_info_dialog_text))
        if (!hasProviders) {
            message
                .append("\n\n")
                .append(activity.getString(R.string.unifiedpush_info_dialog_text_no_provider))
            binding.stopShowing.isChecked = true
        }
        message
            .append("\n\n")
            .append(activity.getString(R.string.unifiedpush_info_dialog_text_reminder))
        binding.dialogDesc.text = message

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.unifiedpush_info_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.unifiedpush_info_dialog_ok) { _, _ ->
                onPositiveAction(hasProviders)
                onHideDialogAction(binding.stopShowing.isChecked)
            }
            .setNeutralButton(activity.getString(R.string.unifiedpush_info_dialog_cancel), null)

        dialog.create().show()
    }

    fun showPushNotificationInfoDialog(activity: Activity, onPositiveAction: (Boolean) -> Unit) {
        val binding = BottomSheetTwoOptionsBinding.inflate(activity.layoutInflater)
        binding.dialogDesc.text = activity.getString(R.string.push_notifications_info_text)
        binding.stopShowing.text = activity.getString(R.string.push_notifications_info_check)

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.push_notifications_info_title))
            .setView(binding.root)
            .setPositiveButton(activity.getString(R.string.push_notifications_info_ok)) { _, _ ->
                onPositiveAction(binding.stopShowing.isChecked)
            }
            .create()
            .show()
    }

    fun buildPushDataMap(
        notificationManager: NotificationManager,
        account: AccountEntity?
    ): Map<String, Boolean> {
        return buildMap {
            Type.asList.forEach { type ->
                put(
                    "data[alerts][${type.presentation}]",
                    NotificationHelper.filterNotification(account, type, notificationManager)
                )
            }
        }
    }
}
