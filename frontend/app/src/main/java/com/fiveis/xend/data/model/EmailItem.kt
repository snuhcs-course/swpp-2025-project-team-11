package com.fiveis.xend.data.model

// 메일 데이터 모델
data class EmailItem(
    val id: String,
    val sender: String,
    val subject: String,
    val content: String,
    val timestamp: String,
    val unread: Boolean
)
