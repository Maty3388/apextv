package com.apextv.app.models

import java.io.Serializable

data class Episode(
    val title: String = "",
    val season: Int = 1,
    val episode: Int = 1,
    val streamUrl: String = ""
) : Serializable

data class Serie(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val description: String = "",
    val rating: String = "",
    val year: String = "",
    val featured: Boolean = false,
    val episodes: List<Episode> = emptyList()
) : Serializable
