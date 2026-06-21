package com.apextv.app.models

import java.io.Serializable

data class Channel(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val number: Int = 999,
    val isLive: Boolean = true,
    val drmKeys: String = "",
    val drmType: String = "",
    val drmLicense: String = "",
    val headers: Map<String, String> = emptyMap()
) : Serializable
