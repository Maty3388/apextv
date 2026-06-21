package com.apextv.app.utils

import android.content.Context

object Prefs {
    private const val NAME = "apex_prefs"

    fun saveToken(ctx: Context, t: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("token", t).apply()
    fun getToken(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("token", "") ?: ""
    fun saveEmail(ctx: Context, e: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("email", e).apply()
    fun getEmail(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("email", "") ?: ""
    fun saveSubEnd(ctx: Context, s: String) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("sub_end", s).apply()
    fun getSubEnd(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("sub_end", "") ?: ""

    fun getDeviceId(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", "") ?: ""
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun isLoggedIn(ctx: Context) = getToken(ctx).isNotEmpty()

    fun saveProfileSelected(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean("profile_selected", true).apply()
    fun isProfileSelected(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean("profile_selected", false)

    fun logout(ctx: Context) {
        val deviceId = getDeviceId(ctx)
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("device_id", deviceId).apply()
    }

    fun saveProgress(ctx: Context, id: String, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        val pct = positionMs.toFloat() / durationMs.toFloat()
        val editor = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
        if (pct < 0.05f || pct > 0.95f) editor.remove("progress_$id")
        else editor.putLong("progress_$id", positionMs)
        editor.apply()
    }

    fun getProgress(ctx: Context, id: String): Long =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getLong("progress_$id", 0L)

    fun getAllProgressIds(ctx: Context): List<String> =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).all.keys
            .filter { it.startsWith("progress_") }.map { it.removePrefix("progress_") }
}
