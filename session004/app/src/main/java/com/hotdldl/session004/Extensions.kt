package com.hotdldl.session004

import android.util.Log

/**
 * author : huangdongliang
 * time   : 2022/8/25
 * desc   :
 */
internal fun String.dLog(tag: String? = null, mark: String = "---") {
    Log.d(tag ?: "LearnMedia", "$mark $this")
}

internal fun String.eLog(tag: String? = null, mark: String = "---") {
    Log.e(tag ?: "LearnMedia", "$mark $this")
}
