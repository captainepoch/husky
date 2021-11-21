package com.keylesspalace.tusky.core.extensions

import android.R.id
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

fun AlertDialog.Builder.dialogWithLink(message: String, positiveButton: String) {
    val dialog = AlertDialog.Builder(context)
        .setMessage(message)
        .setPositiveButton(positiveButton, null)
        .show()

    val text = dialog.window?.findViewById<TextView>(id.message)
    text?.autoLinkMask = Linkify.ALL
    text?.movementMethod = LinkMovementMethod.getInstance()
}
