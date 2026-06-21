package com.apextv.app.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.apextv.app.models.AppVersion

object AutoUpdater {
    fun check(activity: Activity, currentVersion: String, latest: AppVersion) {
        if (latest.version == currentVersion) return
        val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val new = latest.version.split(".").map { it.toIntOrNull() ?: 0 }
        val needsUpdate = new.zip(current).any { (n, c) -> n > c }
        if (!needsUpdate) return
        android.app.AlertDialog.Builder(activity)
            .setTitle("Nueva versión disponible")
            .setMessage("Versión ${latest.version}\n\n${latest.changelog}")
            .setPositiveButton("Actualizar") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latest.apkUrl))
                activity.startActivity(intent)
                if (latest.forceUpdate) activity.finish()
            }
            .apply { if (!latest.forceUpdate) setNegativeButton("Después", null) }
            .show()
    }
}
