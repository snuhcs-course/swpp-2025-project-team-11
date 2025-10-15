package com.fiveis.xend.data.model

import androidx.compose.ui.graphics.Color

data class Group(
    val id: String,
    val name: String,
    val description: String,
    val members: List<Contact>,
    val color: Color
)
