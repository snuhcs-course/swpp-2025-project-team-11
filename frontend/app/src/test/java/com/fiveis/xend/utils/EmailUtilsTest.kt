package com.fiveis.xend.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailUtilsTest {

    @Test
    fun test_extractEmailAddress_withNameAndEmail() {
        val result = EmailUtils.extractEmailAddress("John Doe <john@example.com>")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withOnlyBrackets() {
        val result = EmailUtils.extractEmailAddress("<john@example.com>")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withPlainEmail() {
        val result = EmailUtils.extractEmailAddress("john@example.com")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withWhitespace() {
        val result = EmailUtils.extractEmailAddress("  john@example.com  ")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractSenderName_withNameAndEmail() {
        val result = EmailUtils.extractSenderName("John Doe <john@example.com>")
        assertEquals("John Doe", result)
    }

    @Test
    fun test_extractSenderName_withQuotedName() {
        val result = EmailUtils.extractSenderName("\"John Doe\" <john@example.com>")
        assertEquals("John Doe", result)
    }

    @Test
    fun test_extractSenderName_withOnlyBrackets() {
        val result = EmailUtils.extractSenderName("<john@example.com>")
        assertEquals("<john@example.com>", result)
    }

    @Test
    fun test_extractSenderName_withPlainEmail() {
        val result = EmailUtils.extractSenderName("john@example.com")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractSenderName_withSingleQuotes() {
        val result = EmailUtils.extractSenderName("'John Doe' <john@example.com>")
        assertEquals("John Doe", result)
    }

    @Test
    fun test_extractEmailAddress_withEmptyString() {
        val result = EmailUtils.extractEmailAddress("")
        assertEquals("", result)
    }

    @Test
    fun test_extractEmailAddress_withSpecialCharacters() {
        val result = EmailUtils.extractEmailAddress("User+tag@example.com")
        assertEquals("User+tag@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withUnicodeInName() {
        val result = EmailUtils.extractEmailAddress("홍길동 <hong@example.com>")
        assertEquals("hong@example.com", result)
    }

    @Test
    fun test_extractSenderName_withUnicodeName() {
        val result = EmailUtils.extractSenderName("홍길동 <hong@example.com>")
        assertEquals("홍길동", result)
    }

    @Test
    fun test_extractEmailAddress_withNoClosingBracket() {
        val result = EmailUtils.extractEmailAddress("John Doe <john@example.com")
        assertEquals("John Doe <john@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withMultipleAtSigns() {
        val result = EmailUtils.extractEmailAddress("user@domain@example.com")
        assertEquals("user@domain@example.com", result)
    }

    @Test
    fun test_extractSenderName_withMultipleSpaces() {
        val result = EmailUtils.extractSenderName("John    Doe <john@example.com>")
        assertEquals("John    Doe", result)
    }

    @Test
    fun test_extractEmailAddress_withLeadingTrailingWhitespaceInBrackets() {
        val result = EmailUtils.extractEmailAddress("John <  john@example.com  >")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractSenderName_withOnlyWhitespace() {
        val result = EmailUtils.extractSenderName("   ")
        assertEquals("   ", result)
    }

    @Test
    fun test_extractEmailAddress_withDomainOnly() {
        val result = EmailUtils.extractEmailAddress("@example.com")
        assertEquals("@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withLocalPartOnly() {
        val result = EmailUtils.extractEmailAddress("username")
        assertEquals("username", result)
    }

    @Test
    fun test_extractSenderName_withNumbers() {
        val result = EmailUtils.extractSenderName("User123 <user@example.com>")
        assertEquals("User123", result)
    }

    @Test
    fun test_extractSenderName_withSpecialCharactersInName() {
        val result = EmailUtils.extractSenderName("John-Doe_Jr. <john@example.com>")
        assertEquals("John-Doe_Jr.", result)
    }

    @Test
    fun test_extractEmailAddress_withIPAddress() {
        val result = EmailUtils.extractEmailAddress("user@192.168.1.1")
        assertEquals("user@192.168.1.1", result)
    }

    @Test
    fun test_extractEmailAddress_withSubdomain() {
        val result = EmailUtils.extractEmailAddress("user@mail.example.com")
        assertEquals("user@mail.example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withLongName() {
        val longName = "A".repeat(100)
        val result = EmailUtils.extractEmailAddress("$longName <user@example.com>")
        assertEquals("user@example.com", result)
    }

    @Test
    fun test_extractSenderName_withLongName() {
        val longName = "A".repeat(100)
        val result = EmailUtils.extractSenderName("$longName <user@example.com>")
        assertEquals(longName, result)
    }

    @Test
    fun test_extractEmailAddress_withParentheses() {
        val result = EmailUtils.extractEmailAddress("John Doe (CEO) <john@example.com>")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractSenderName_withParentheses() {
        val result = EmailUtils.extractSenderName("John Doe (CEO) <john@example.com>")
        assertEquals("John Doe (CEO)", result)
    }

    @Test
    fun test_extractEmailAddress_withComma() {
        val result = EmailUtils.extractEmailAddress("Doe, John <john@example.com>")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractSenderName_withComma() {
        val result = EmailUtils.extractSenderName("Doe, John <john@example.com>")
        assertEquals("Doe, John", result)
    }

    @Test
    fun test_extractEmailAddress_caseInsensitive() {
        val result = EmailUtils.extractEmailAddress("John@EXAMPLE.COM")
        assertEquals("John@EXAMPLE.COM", result)
    }

    @Test
    fun test_extractEmailAddress_withEmojiInName() {
        val result = EmailUtils.extractEmailAddress("John 😀 <john@example.com>")
        assertEquals("john@example.com", result)
    }

    @Test
    fun test_extractSenderName_withEmojiInName() {
        val result = EmailUtils.extractSenderName("John 😀 <john@example.com>")
        assertEquals("John 😀", result)
    }

    @Test
    fun test_extractEmailAddress_withHyphenatedDomain() {
        val result = EmailUtils.extractEmailAddress("user@my-domain.com")
        assertEquals("user@my-domain.com", result)
    }

    @Test
    fun test_extractEmailAddress_withUnderscoreInLocal() {
        val result = EmailUtils.extractEmailAddress("user_name@example.com")
        assertEquals("user_name@example.com", result)
    }

    @Test
    fun test_extractEmailAddress_withDotInLocal() {
        val result = EmailUtils.extractEmailAddress("user.name@example.com")
        assertEquals("user.name@example.com", result)
    }

    @Test
    fun test_extractSenderName_withDoubleQuotesInsideName() {
        val result = EmailUtils.extractSenderName("\"John \\\"The Boss\\\" Doe\" <john@example.com>")
        assertEquals("John \\\"The Boss\\\" Doe", result)
    }

    @Test
    fun test_extractEmailAddress_withTLD() {
        val result = EmailUtils.extractEmailAddress("user@example.co.uk")
        assertEquals("user@example.co.uk", result)
    }

    @Test
    fun test_extractSenderName_withTitle() {
        val result = EmailUtils.extractSenderName("Dr. John Doe <john@example.com>")
        assertEquals("Dr. John Doe", result)
    }

    @Test
    fun test_extractEmailAddress_withPlusSign() {
        val result = EmailUtils.extractEmailAddress("user+tag@example.com")
        assertEquals("user+tag@example.com", result)
    }
}
