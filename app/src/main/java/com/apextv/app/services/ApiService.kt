package com.apextv.app.services

import com.apextv.app.models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    private const val BASE = "http://31.40.212.205:25461"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    var token = ""
    var subEnd = ""
    var loginError = ""
    var userRole = ""
    var appContext: android.content.Context? = null

    private const val CACHE_NAME = "apex_cache"

    private fun saveCache(key: String, json: String) {
        appContext?.getSharedPreferences(CACHE_NAME, android.content.Context.MODE_PRIVATE)
            ?.edit()?.putString(key, json)?.apply()
    }

    private fun getCache(key: String): String? =
        appContext?.getSharedPreferences(CACHE_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(key, null)

    fun getCachedChannels(): List<Channel> {
        val cached = getCache("channels") ?: return emptyList()
        return parseChannels(cached)
    }

    private fun parseChannels(jsonStr: String): List<Channel> {
        val json = JSONObject(jsonStr)
        val arr = json.optJSONArray("channels") ?: return emptyList()
        return (0 until arr.length()).map {
            val ch = arr.getJSONObject(it)
            Channel(ch.optString("_id"), ch.optString("name"), ch.optString("category"),
                ch.optString("logo"), ch.optString("stream_url"), ch.optInt("number", 999),
                drmKeys = ch.optString("drm_keys"), drmType = ch.optString("drm_type"),
                drmLicense = ch.optString("drm_license"))
        }
    }

    fun login(email: String, password: String): String? {
        val bodyJson = JSONObject().put("email", email).put("password", password).toString()
        val body = bodyJson.toRequestBody("application/json".toMediaType())
        val res = client.newCall(Request.Builder().url("$BASE/auth/login").post(body).build()).execute()
        val json = JSONObject(res.body?.string() ?: return null)
        loginError = json.optString("error", "")
        userRole = json.optJSONObject("user")?.optString("role", "client") ?: "client"
        subEnd = json.optJSONObject("user")?.optString("subscription_end") ?: ""
        return json.optString("token").takeIf { it.isNotEmpty() }
    }

    fun getChannels(): List<Channel> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/channels?limit=5000")
                .header("Authorization", "Bearer $token").build()).execute()
            val bodyStr = res.body?.string() ?: return getCachedChannels()
            saveCache("channels", bodyStr)
            parseChannels(bodyStr)
        } catch (_: Exception) { getCachedChannels() }
    }

    fun getFavorites(): List<Channel> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/favorites")
                .header("Authorization", "Bearer $token").build()).execute()
            val bodyStr = res.body?.string() ?: return emptyList()
            val json = JSONObject(bodyStr)
            val arr = json.optJSONArray("channels") ?: return emptyList()
            (0 until arr.length()).map {
                val ch = arr.getJSONObject(it)
                Channel(ch.optString("_id"), ch.optString("name"), ch.optString("category"),
                    ch.optString("logo"), ch.optString("stream_url"), ch.optInt("number", 999))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addFavorite(channelId: String) {
        try {
            val body = "{}".toRequestBody("application/json".toMediaType())
            client.newCall(Request.Builder().url("$BASE/favorites/$channelId")
                .header("Authorization", "Bearer $token").post(body).build()).execute()
        } catch (_: Exception) {}
    }

    fun removeFavorite(channelId: String) {
        try {
            client.newCall(Request.Builder().url("$BASE/favorites/$channelId")
                .header("Authorization", "Bearer $token").delete().build()).execute()
        } catch (_: Exception) {}
    }

    fun getMovies(featured: Boolean = false): List<Movie> {
        return try {
            val url = if (featured) "$BASE/movies?featured=true" else "$BASE/movies?limit=200"
            val res = client.newCall(Request.Builder().url(url)
                .header("Authorization", "Bearer $token").build()).execute()
            val bodyStr = res.body?.string() ?: return emptyList()
            val json = JSONObject(bodyStr)
            val arr = json.optJSONArray("movies") ?: return emptyList()
            (0 until arr.length()).map {
                val m = arr.getJSONObject(it)
                Movie(m.optString("_id"), m.optString("title"), m.optString("category"),
                    m.optString("posterUrl"), m.optString("backdropUrl"), m.optString("stream_url"),
                    m.optString("description"), m.optString("rating"), m.optString("year"),
                    m.optBoolean("featured"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun getSeries(featured: Boolean = false): List<Serie> {
        return try {
            val url = if (featured) "$BASE/series?featured=true" else "$BASE/series"
            val res = client.newCall(Request.Builder().url(url)
                .header("Authorization", "Bearer $token").build()).execute()
            val bodyStr = res.body?.string() ?: return emptyList()
            val json = JSONObject(bodyStr)
            val arr = json.optJSONArray("series") ?: return emptyList()
            (0 until arr.length()).map {
                val s = arr.getJSONObject(it)
                val epArr = s.optJSONArray("episodes")
                val episodes = if (epArr != null) (0 until epArr.length()).map { j ->
                    val e = epArr.getJSONObject(j)
                    Episode(e.optString("title"), e.optInt("season"), e.optInt("episode"), e.optString("streamUrl"))
                } else emptyList()
                Serie(s.optString("_id"), s.optString("title"), s.optString("category"),
                    s.optString("posterUrl"), s.optString("stream_url"), s.optString("description"),
                    s.optString("rating"), s.optString("year"), s.optBoolean("featured"), episodes)
            }
        } catch (_: Exception) { emptyList() }
    }

    fun getProfile(): JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/profile")
                .header("Authorization", "Bearer $token").get().build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun selectProfile(profileId: Int, deviceId: String): JSONObject {
        return try {
            val body = JSONObject().put("profileId", profileId).put("deviceId", deviceId)
                .toString().toRequestBody("application/json".toMediaType())
            val res = client.newCall(Request.Builder().url("$BASE/profile/select")
                .header("Authorization", "Bearer $token").post(body).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun verifyParentalPin(pin: String): JSONObject {
        return try {
            val body = JSONObject().put("pin", pin).toString()
                .toRequestBody("application/json".toMediaType())
            val res = client.newCall(Request.Builder().url("$BASE/parental/verify")
                .header("Authorization", "Bearer $token").post(body).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }

    fun setParentalPin(pin: String): JSONObject {
        return try {
            val body = JSONObject().put("pin", pin).toString()
                .toRequestBody("application/json".toMediaType())
            val res = client.newCall(Request.Builder().url("$BASE/parental/pin")
                .header("Authorization", "Bearer $token").put(body).build()).execute()
            JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { JSONObject() }
    }


    fun getAdminUsers(): List<org.json.JSONObject> {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/users")
                .header("Authorization", "Bearer $token").build()).execute()
            val json = org.json.JSONObject(res.body?.string() ?: return emptyList())
            val arr = json.optJSONArray("users") ?: return emptyList()
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun getAdminStats(): org.json.JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/stats")
                .header("Authorization", "Bearer $token").build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun getAdminCredits(): org.json.JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/credits")
                .header("Authorization", "Bearer $token").build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun createAdminUser(email: String, password: String, role: String, subEnd: String, notes: String): org.json.JSONObject {
        return try {
            val body = org.json.JSONObject()
            body.put("email", email).put("password", password).put("role", role)
            if (subEnd.isNotEmpty()) body.put("subscription_end", subEnd)
            if (notes.isNotEmpty()) body.put("notes", notes)
            val res = client.newCall(Request.Builder().url("$BASE/admin/users")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun editAdminUser(id: String, body: org.json.JSONObject): org.json.JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/users/$id")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun deleteAdminUser(id: String): org.json.JSONObject {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/admin/users/$id")
                .header("Authorization", "Bearer $token").delete().build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun renewAdminUser(id: String, days: Int): org.json.JSONObject {
        return try {
            val body = org.json.JSONObject().put("days", days)
            val res = client.newCall(Request.Builder().url("$BASE/admin/users/$id/renew")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun assignCredits(userId: String, credits: Int): org.json.JSONObject {
        return try {
            val body = org.json.JSONObject().put("userId", userId).put("credits", credits)
            val res = client.newCall(Request.Builder().url("$BASE/admin/credits")
                .header("Authorization", "Bearer $token")
                .put(body.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            org.json.JSONObject(res.body?.string() ?: "{}")
        } catch (_: Exception) { org.json.JSONObject() }
    }

    fun getVersion(): AppVersion? {
        return try {
            val res = client.newCall(Request.Builder().url("$BASE/apextv/version").build()).execute()
            val json = JSONObject(res.body?.string() ?: return null)
            AppVersion(json.optString("version", "1.0.0"), json.optString("apkUrl"),
                json.optBoolean("forceUpdate"), json.optString("changelog"))
        } catch (_: Exception) { null }
    }
}
