package com.apextv.app.utils

import android.content.Context
import android.content.pm.PackageManager

object DeviceUtils {
    fun isTV(ctx: Context): Boolean {
        return ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
               ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }
}
