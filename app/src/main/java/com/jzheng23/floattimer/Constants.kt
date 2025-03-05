package com.jzheng23.floattimer

import android.content.Context

object Constants {
    const val MIN_BUTTON_SIZE = 30
    const val MAX_BUTTON_SIZE = 80
    const val DEFAULT_BUTTON_SIZE = 48
    const val DEFAULT_TEXT_SIZE = 24 // default text size in sp

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun calculateTextSize(buttonSize: Int): Float {
        return (DEFAULT_TEXT_SIZE * (buttonSize.toFloat() / DEFAULT_BUTTON_SIZE))
    }
}