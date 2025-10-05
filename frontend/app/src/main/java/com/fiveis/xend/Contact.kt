package com.fiveis.xend

import androidx.compose.ui.graphics.Color

data class Contact(
    val name: String,
    val email: String,
    val color: Color = Color(0xFF5A7DFF) // avatar bg, 추후 그룹 정보 등으로 수정
)
