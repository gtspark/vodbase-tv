package net.vodbase.tv.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Chapter(
    val title: String,
    val startTimeMs: Long,
    val previewUrl: String?
)
