package com.fiveis.xend.utils

/**
 * Email 주소 파싱 유틸리티
 */
object EmailUtils {
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
