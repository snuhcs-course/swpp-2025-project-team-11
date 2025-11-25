package com.fiveis.xend.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Email 주소 파싱 유틸리티
 */
object EmailUtils {

    private const val TAG = "EmailUtils"

    /**
     * Supported date format patterns for parsing dateRaw.
     * Gmail API returns dates in RFC 2822 or ISO 8601 formats.
     *
     * Note: SimpleDateFormat instances are created per-call to avoid thread safety issues.
     */
    private val dateFormatPatterns = listOf(
        // RFC 2822 formats
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z",
        // ISO 8601 formats
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd HH:mm:ss"
    )

    /**
     * Parse dateRaw string to epoch timestamp in milliseconds.
     *
     * @param dateRaw Raw date string from Gmail API (RFC 2822 or ISO 8601 format)
     * @return Epoch timestamp in milliseconds, or 0 if parsing fails
     *
     * Examples:
     * - "Tue, 19 Nov 2024 10:30:00 +0900" → 1732001400000
     * - "2024-11-19T10:30:00+09:00" → 1732001400000
     */
    fun parseDateToTimestamp(dateRaw: String): Long {
        if (dateRaw.isBlank()) return 0L

        for (pattern in dateFormatPatterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.ENGLISH)
                val date = format.parse(dateRaw)
                if (date != null) {
                    Log.d(TAG, "Parsed '$dateRaw' -> ${date.time}")
                    return date.time
                }
            } catch (_: Exception) {
                // Try next format
            }
        }

        Log.w(TAG, "Failed to parse dateRaw: $dateRaw")
        return 0L
    }

    /**
     * "이름 <email@example.com>" 또는 "<email@example.com>" 형식에서 이메일만 추출
     *
     * @param emailString 파싱할 이메일 문자열
     * @return 추출된 이메일 주소
     *
     * Examples:
     * - "John Doe <john@example.com>" → "john@example.com"
     * - "<john@example.com>" → "john@example.com"
     * - "john@example.com" → "john@example.com"
     */
    fun extractEmailAddress(emailString: String): String {
        val regex = "<([^>]+)>".toRegex()
        val match = regex.find(emailString)
        return match?.groupValues?.get(1)?.trim() ?: emailString.trim()
    }

    /**
     * "이름 <email@example.com>" 형식에서 이름만 추출
     *
     * @param fromEmail 파싱할 이메일 문자열
     * @return 추출된 발신자 이름
     *
     * Examples:
     * - "John Doe <john@example.com>" → "John Doe"
     * - "\"John Doe\" <john@example.com>" → "John Doe" (따옴표 제거됨)
     * - "<john@example.com>" → "john@example.com"
     * - "john@example.com" → "john@example.com"
     */
    fun extractSenderName(fromEmail: String): String {
        val nameRegex = "(.+?)\\s*<".toRegex()
        val matchResult = nameRegex.find(fromEmail)
        val name = matchResult?.groupValues?.get(1)?.trim()
            ?: fromEmail.substringBefore("<").trim().ifEmpty { fromEmail }
        return name.trim('"', '\'')
    }
}
