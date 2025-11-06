package com.fiveis.xend.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TemplateModelTest {

    @Before
    fun setup() {
        TemplateData.templates.clear()
        TemplateData.templates.addAll(listOf(
            EmailTemplate(
                id = "1",
                category = TemplateCategory.WORK,
                title = "Test Work Template",
                description = "Work description",
                subject = "Work Subject",
                body = "Work Body"
            ),
            EmailTemplate(
                id = "2",
                category = TemplateCategory.SCHOOL,
                title = "Test School Template",
                description = "School description",
                subject = "School Subject",
                body = "School Body"
            ),
            EmailTemplate(
                id = "3",
                category = TemplateCategory.PERSONAL,
                title = "Test Personal Template",
                description = "Personal description",
                subject = "Personal Subject",
                body = "Personal Body"
            )
        ))
    }

    @Test
    fun template_category_display_names_are_correct() {
        assertEquals("전체", TemplateCategory.ALL.displayName)
        assertEquals("업무", TemplateCategory.WORK.displayName)
        assertEquals("학업", TemplateCategory.SCHOOL.displayName)
        assertEquals("개인", TemplateCategory.PERSONAL.displayName)
    }

    @Test
    fun email_template_data_class_properties() {
        val template = EmailTemplate(
            id = "test123",
            category = TemplateCategory.WORK,
            title = "Test Title",
            description = "Test Description",
            subject = "Test Subject",
            body = "Test Body"
        )

        assertEquals("test123", template.id)
        assertEquals(TemplateCategory.WORK, template.category)
        assertEquals("Test Title", template.title)
        assertEquals("Test Description", template.description)
        assertEquals("Test Subject", template.subject)
        assertEquals("Test Body", template.body)
    }

    @Test
    fun get_templates_by_category_all_returns_all_templates() {
        val result = TemplateData.getTemplatesByCategory(TemplateCategory.ALL)

        assertEquals(3, result.size)
    }

    @Test
    fun get_templates_by_category_work_returns_only_work_templates() {
        val result = TemplateData.getTemplatesByCategory(TemplateCategory.WORK)

        assertEquals(1, result.size)
        assertEquals(TemplateCategory.WORK, result[0].category)
    }

    @Test
    fun get_templates_by_category_school_returns_only_school_templates() {
        val result = TemplateData.getTemplatesByCategory(TemplateCategory.SCHOOL)

        assertEquals(1, result.size)
        assertEquals(TemplateCategory.SCHOOL, result[0].category)
    }

    @Test
    fun get_templates_by_category_personal_returns_only_personal_templates() {
        val result = TemplateData.getTemplatesByCategory(TemplateCategory.PERSONAL)

        assertEquals(1, result.size)
        assertEquals(TemplateCategory.PERSONAL, result[0].category)
    }

    @Test
    fun add_template_adds_to_beginning_of_list() {
        val initialSize = TemplateData.templates.size

        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "New Template",
            description = "New Description",
            subject = "New Subject",
            body = "New Body"
        )

        assertEquals(initialSize + 1, TemplateData.templates.size)
        assertEquals("New Template", TemplateData.templates[0].title)
    }

    @Test
    fun add_template_generates_unique_id() {
        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "Template 1",
            description = "Description 1",
            subject = "Subject 1",
            body = "Body 1"
        )

        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "Template 2",
            description = "Description 2",
            subject = "Subject 2",
            body = "Body 2"
        )

        val id1 = TemplateData.templates[0].id
        val id2 = TemplateData.templates[1].id

        assertFalse(id1 == id2)
    }

    @Test
    fun delete_template_removes_template_by_id() {
        val initialSize = TemplateData.templates.size

        TemplateData.deleteTemplate("1")

        assertEquals(initialSize - 1, TemplateData.templates.size)
        assertFalse(TemplateData.templates.any { it.id == "1" })
    }

    @Test
    fun delete_template_with_non_existent_id_does_not_fail() {
        val initialSize = TemplateData.templates.size

        TemplateData.deleteTemplate("non-existent-id")

        assertEquals(initialSize, TemplateData.templates.size)
    }

    @Test
    fun update_template_updates_existing_template() {
        TemplateData.updateTemplate(
            templateId = "1",
            category = TemplateCategory.PERSONAL,
            title = "Updated Title",
            description = "Updated Description",
            subject = "Updated Subject",
            body = "Updated Body"
        )

        val template = TemplateData.templates.find { it.id == "1" }

        assertNotNull(template)
        assertEquals("Updated Title", template?.title)
        assertEquals(TemplateCategory.PERSONAL, template?.category)
        assertEquals("Updated Description", template?.description)
        assertEquals("Updated Subject", template?.subject)
        assertEquals("Updated Body", template?.body)
    }

    @Test
    fun update_template_preserves_template_id() {
        TemplateData.updateTemplate(
            templateId = "1",
            category = TemplateCategory.WORK,
            title = "Updated",
            description = "Updated",
            subject = "Updated",
            body = "Updated"
        )

        val template = TemplateData.templates.find { it.id == "1" }

        assertNotNull(template)
        assertEquals("1", template?.id)
    }

    @Test
    fun update_template_with_non_existent_id_does_not_modify_list() {
        val initialSize = TemplateData.templates.size

        TemplateData.updateTemplate(
            templateId = "non-existent",
            category = TemplateCategory.WORK,
            title = "Test",
            description = "Test",
            subject = "Test",
            body = "Test"
        )

        assertEquals(initialSize, TemplateData.templates.size)
    }

    @Test
    fun add_multiple_templates_maintains_correct_order() {
        TemplateData.addTemplate(
            TemplateCategory.WORK, "First", "Desc", "Subj", "Body"
        )
        TemplateData.addTemplate(
            TemplateCategory.WORK, "Second", "Desc", "Subj", "Body"
        )
        TemplateData.addTemplate(
            TemplateCategory.WORK, "Third", "Desc", "Subj", "Body"
        )

        assertEquals("Third", TemplateData.templates[0].title)
        assertEquals("Second", TemplateData.templates[1].title)
        assertEquals("First", TemplateData.templates[2].title)
    }

    @Test
    fun delete_all_templates_works_correctly() {
        TemplateData.templates.clear()

        assertEquals(0, TemplateData.templates.size)
        assertTrue(TemplateData.getTemplatesByCategory(TemplateCategory.ALL).isEmpty())
    }

    @Test
    fun add_template_with_empty_strings() {
        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "",
            description = "",
            subject = "",
            body = ""
        )

        val template = TemplateData.templates[0]
        assertEquals("", template.title)
        assertEquals("", template.description)
        assertEquals("", template.subject)
        assertEquals("", template.body)
    }

    @Test
    fun add_template_with_special_characters() {
        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "Test !@#$%^&*()",
            description = "<html>",
            subject = "Line1\nLine2",
            body = "Quote: \"Test\""
        )

        val template = TemplateData.templates[0]
        assertEquals("Test !@#$%^&*()", template.title)
        assertEquals("<html>", template.description)
        assertTrue(template.subject.contains("\n"))
    }

    @Test
    fun add_template_with_unicode_characters() {
        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "안녕하세요",
            description = "テスト",
            subject = "测试",
            body = "🎉 Test"
        )

        val template = TemplateData.templates[0]
        assertEquals("안녕하세요", template.title)
        assertEquals("テスト", template.description)
        assertEquals("测试", template.subject)
        assertTrue(template.body.contains("🎉"))
    }

    @Test
    fun update_template_changes_category() {
        TemplateData.updateTemplate(
            templateId = "1",
            category = TemplateCategory.SCHOOL,
            title = "Updated",
            description = "Updated",
            subject = "Updated",
            body = "Updated"
        )

        val workTemplates = TemplateData.getTemplatesByCategory(TemplateCategory.WORK)
        val schoolTemplates = TemplateData.getTemplatesByCategory(TemplateCategory.SCHOOL)

        assertEquals(0, workTemplates.filter { it.id == "1" }.size)
        assertEquals(1, schoolTemplates.filter { it.id == "1" }.size)
    }

    @Test
    fun get_templates_by_category_returns_empty_list_for_empty_category() {
        TemplateData.templates.clear()

        val result = TemplateData.getTemplatesByCategory(TemplateCategory.WORK)

        assertTrue(result.isEmpty())
    }

    @Test
    fun templates_list_is_mutable() {
        val initialSize = TemplateData.templates.size

        TemplateData.templates.add(
            EmailTemplate("new", TemplateCategory.WORK, "Test", "Test", "Test", "Test")
        )

        assertEquals(initialSize + 1, TemplateData.templates.size)
    }

    @Test
    fun add_template_with_long_content() {
        val longBody = "A".repeat(10000)

        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "Long Template",
            description = "Description",
            subject = "Subject",
            body = longBody
        )

        val template = TemplateData.templates[0]
        assertEquals(10000, template.body.length)
    }

    @Test
    fun delete_multiple_templates() {
        TemplateData.deleteTemplate("1")
        TemplateData.deleteTemplate("2")

        assertEquals(1, TemplateData.templates.size)
        assertEquals("3", TemplateData.templates[0].id)
    }

    @Test
    fun update_then_delete_template() {
        TemplateData.updateTemplate(
            templateId = "1",
            category = TemplateCategory.PERSONAL,
            title = "Updated",
            description = "Updated",
            subject = "Updated",
            body = "Updated"
        )

        TemplateData.deleteTemplate("1")

        assertFalse(TemplateData.templates.any { it.id == "1" })
    }

    @Test
    fun add_then_immediately_delete_template() {
        TemplateData.addTemplate(
            category = TemplateCategory.WORK,
            title = "Temporary",
            description = "Temp",
            subject = "Temp",
            body = "Temp"
        )

        val newId = TemplateData.templates[0].id
        TemplateData.deleteTemplate(newId)

        assertFalse(TemplateData.templates.any { it.id == newId })
    }

    @Test
    fun template_data_class_copy_works() {
        val original = EmailTemplate(
            id = "1",
            category = TemplateCategory.WORK,
            title = "Original",
            description = "Desc",
            subject = "Subj",
            body = "Body"
        )

        val copied = original.copy(title = "Updated")

        assertEquals("1", copied.id)
        assertEquals("Updated", copied.title)
        assertEquals(original.description, copied.description)
    }

    @Test
    fun template_data_class_equals_works() {
        val template1 = EmailTemplate(
            id = "1",
            category = TemplateCategory.WORK,
            title = "Test",
            description = "Desc",
            subject = "Subj",
            body = "Body"
        )

        val template2 = EmailTemplate(
            id = "1",
            category = TemplateCategory.WORK,
            title = "Test",
            description = "Desc",
            subject = "Subj",
            body = "Body"
        )

        assertEquals(template1, template2)
    }

    @Test
    fun get_templates_by_category_returns_copy_or_reference() {
        val result1 = TemplateData.getTemplatesByCategory(TemplateCategory.WORK)
        val result2 = TemplateData.getTemplatesByCategory(TemplateCategory.WORK)

        assertEquals(result1.size, result2.size)
    }

    @Test
    fun template_category_has_correct_number_of_values() {
        val categories = TemplateCategory.entries

        assertEquals(4, categories.size)
    }

    @Test
    fun all_template_categories_have_display_names() {
        TemplateCategory.entries.forEach { category ->
            assertNotNull(category.displayName)
            assertFalse(category.displayName.isEmpty())
        }
    }
}
