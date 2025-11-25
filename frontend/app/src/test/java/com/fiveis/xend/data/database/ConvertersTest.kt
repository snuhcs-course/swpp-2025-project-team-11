package com.fiveis.xend.data.database

import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun from_string_list_with_empty_list_returns_empty_json_array() {
        val list = emptyList<String>()
        val result = converters.fromStringList(list)
        assertEquals("[]", result)
    }

    @Test
    fun from_string_list_with_single_item_returns_json_array_with_one_element() {
        val list = listOf("item1")
        val result = converters.fromStringList(list)
        val expectedJson = """["item1"]"""
        assertEquals(expectedJson, result)
    }

    @Test
    fun from_string_list_with_multiple_items_returns_correct_json_array() {
        val list = listOf("item1", "item2", "item3")
        val result = converters.fromStringList(list)
        val expectedJson = """["item1","item2","item3"]"""
        assertEquals(expectedJson, result)
    }

    @Test
    fun from_string_list_with_special_characters_is_handled_correctly() {
        val list = listOf("item with \"quotes\"", "item with, comma")
        val result = converters.fromStringList(list)
        val expectedJson = """["item with \"quotes\"","item with, comma"]"""
        assertEquals(expectedJson, result)
    }

    @Test
    fun to_string_list_with_empty_json_array_returns_empty_list() {
        val json = "[]"
        val result = converters.toStringList(json)
        assertTrue(result.isEmpty())
    }

    @Test
    fun to_string_list_with_valid_json_array_returns_correct_list() {
        val json = """["item1","item2","item3"]"""
        val result = converters.toStringList(json)
        assertEquals(listOf("item1", "item2", "item3"), result)
    }

    @Test
    fun to_string_list_with_single_item_json_array_returns_list_with_one_element() {
        val json = """["item1"]"""
        val result = converters.toStringList(json)
        assertEquals(listOf("item1"), result)
    }

    @Test
    fun to_string_list_with_json_containing_special_characters_is_handled_correctly() {
        val json = """["item with \"quotes\"","item with, comma"]"""
        val result = converters.toStringList(json)
        assertEquals(listOf("item with \"quotes\"", "item with, comma"), result)
    }

    @Test(expected = JsonSyntaxException::class)
    fun to_string_list_with_malformed_json_throws_json_syntax_exception() {
        val json = "[item1, item2"
        converters.toStringList(json)
    }

    @Test
    fun from_string_list_and_to_string_list_roundtrip() {
        val originalList = listOf("a", "b", "c")
        val json = converters.fromStringList(originalList)
        val finalList = converters.toStringList(json)
        assertEquals(originalList, finalList)
    }

    @Test
    fun from_string_list_and_to_string_list_roundtrip_with_empty_list() {
        val originalList = emptyList<String>()
        val json = converters.fromStringList(originalList)
        val finalList = converters.toStringList(json)
        assertEquals(originalList, finalList)
    }

    @Test
    fun from_attachment_list_with_null_returns_empty_json_array() {
        val result = converters.fromAttachmentList(null)
        assertEquals("[]", result)
    }

    @Test
    fun from_attachment_list_with_empty_list_returns_empty_json_array() {
        val list = emptyList<com.fiveis.xend.data.model.Attachment>()
        val result = converters.fromAttachmentList(list)
        assertEquals("[]", result)
    }

    @Test
    fun from_attachment_list_with_attachments_returns_correct_json() {
        val list = listOf(
            com.fiveis.xend.data.model.Attachment("att1", "file1.pdf", "application/pdf", 1024L),
            com.fiveis.xend.data.model.Attachment("att2", "file2.txt", "text/plain", 512L)
        )
        val result = converters.fromAttachmentList(list)
        assertTrue(result.contains("file1.pdf"))
        assertTrue(result.contains("application/pdf"))
    }

    @Test
    fun to_attachment_list_with_blank_string_returns_empty_list() {
        val result = converters.toAttachmentList("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun to_attachment_list_with_empty_json_array_returns_empty_list() {
        val result = converters.toAttachmentList("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun to_attachment_list_with_valid_json_returns_correct_list() {
        val json = """[{"attachment_id":"att1","filename":"file1.pdf","mime_type":"application/pdf","size":1024}]"""
        val result = converters.toAttachmentList(json)
        assertEquals(1, result.size)
        assertEquals("file1.pdf", result[0].filename)
        assertEquals("application/pdf", result[0].mimeType)
        assertEquals(1024L, result[0].size)
    }

    @Test
    fun to_attachment_list_with_null_json_returns_empty_list() {
        val result = converters.toAttachmentList("null")
        assertTrue(result.isEmpty())
    }

    @Test
    fun from_attachment_list_and_to_attachment_list_roundtrip() {
        val originalList = listOf(
            com.fiveis.xend.data.model.Attachment("att1", "test.pdf", "application/pdf", 2048L)
        )
        val json = converters.fromAttachmentList(originalList)
        val finalList = converters.toAttachmentList(json)
        assertEquals(originalList.size, finalList.size)
        assertEquals(originalList[0].filename, finalList[0].filename)
        assertEquals(originalList[0].mimeType, finalList[0].mimeType)
        assertEquals(originalList[0].size, finalList[0].size)
    }
}
