package com.fiveis.xend.data.model

import androidx.compose.ui.graphics.Color

data class Contact(
    val name: String,
    val email: String,
    // avatar bg, 추후 그룹 정보 등으로 수정
    val color: Color = Color(0xFF5A7DFF)
)
