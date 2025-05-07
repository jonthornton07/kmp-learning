package com.thornton.kmpnotes.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt:  Instant = Clock.System.now()
)