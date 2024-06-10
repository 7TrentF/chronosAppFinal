package com.example.chronostimetracker

import android.app.Dialog
import android.content.Context

class CustomDialog(context: Context) : Dialog(context) {

    private var onDismissListener: (() -> Unit)? = null
    private var onCancelListener: (() -> Unit)? = null

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }

    override fun dismiss() {
        onDismissListener?.invoke()
        super.dismiss()
    }

    override fun cancel() {
        onCancelListener?.invoke()
        super.cancel()
    }
}
