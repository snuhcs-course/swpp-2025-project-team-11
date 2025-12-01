package com.fiveis.xend.ui.contactbook

import com.fiveis.xend.data.model.PromptOption
import org.junit.Assert.*
import org.junit.Test

class AiPromptingCardLogicTest {

    @Test
    fun promptingUiState_default_values() {
        val state = PromptingUiState()

        assertTrue(state.selectedTone.isEmpty())
        assertTrue(state.selectedFormat.isEmpty())
    }

    @Test
    fun promptingUiState_with_tone_selection() {
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val state = PromptingUiState(selectedTone = setOf(toneOption))

        assertEquals(1, state.selectedTone.size)
        assertTrue(state.selectedTone.contains(toneOption))
        assertTrue(state.selectedFormat.isEmpty())
    }

    @Test
    fun promptingUiState_with_format_selection() {
        val formatOption = PromptOption(1, "format", "3~5문장", "간결하게")
        val state = PromptingUiState(selectedFormat = setOf(formatOption))

        assertTrue(state.selectedTone.isEmpty())
        assertEquals(1, state.selectedFormat.size)
        assertTrue(state.selectedFormat.contains(formatOption))
    }

    @Test
    fun promptingUiState_with_both_selections() {
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val formatOption = PromptOption(2, "format", "3~5문장", "간결하게")
        val state = PromptingUiState(
            selectedTone = setOf(toneOption),
            selectedFormat = setOf(formatOption)
        )

        assertEquals(1, state.selectedTone.size)
        assertEquals(1, state.selectedFormat.size)
    }

    @Test
    fun promptingUiState_with_multiple_tones() {
        val tone1 = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val tone2 = PromptOption(2, "tone", "직설적", "직설적으로")
        val state = PromptingUiState(selectedTone = setOf(tone1, tone2))

        assertEquals(2, state.selectedTone.size)
        assertTrue(state.selectedTone.contains(tone1))
        assertTrue(state.selectedTone.contains(tone2))
    }

    @Test
    fun promptingUiState_with_multiple_formats() {
        val format1 = PromptOption(1, "format", "3~5문장", "간결하게")
        val format2 = PromptOption(2, "format", "핵심키워드", "키워드 강조")
        val state = PromptingUiState(selectedFormat = setOf(format1, format2))

        assertEquals(2, state.selectedFormat.size)
        assertTrue(state.selectedFormat.contains(format1))
        assertTrue(state.selectedFormat.contains(format2))
    }

    @Test
    fun promptExample_creates_correctly() {
        val example = PromptExample("tone", "존댓말", "존댓말을 사용하여 정중하게 작성하세요")

        assertEquals("tone", example.key)
        assertEquals("존댓말", example.name)
        assertEquals("존댓말을 사용하여 정중하게 작성하세요", example.prompt)
    }

    @Test
    fun promptOption_tone_key() {
        val option = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        assertEquals("tone", option.key)
        assertEquals("존댓말", option.name)
    }

    @Test
    fun promptOption_format_key() {
        val option = PromptOption(1, "format", "3~5문장", "간결하게")

        assertEquals("format", option.key)
        assertEquals("3~5문장", option.name)
    }

    @Test
    fun promptOption_with_blank_name() {
        val option = PromptOption(1, "tone", "", "존댓말을 사용하세요")

        assertTrue(option.name.isBlank())
        assertFalse(option.prompt.isBlank())
    }

    @Test
    fun promptOption_with_blank_prompt() {
        val option = PromptOption(1, "tone", "존댓말", "")

        assertFalse(option.name.isBlank())
        assertTrue(option.prompt.isBlank())
    }

    @Test
    fun promptOption_equality_by_id() {
        val option1 = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val option2 = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        assertEquals(option1.id, option2.id)
    }

    @Test
    fun promptOption_different_ids() {
        val option1 = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val option2 = PromptOption(2, "tone", "직설적", "직설적으로")

        assertNotEquals(option1.id, option2.id)
    }

    @Test
    fun set_toggle_adds_new_option() {
        val option = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val emptySet = emptySet<PromptOption>()

        val newSet = if (emptySet.contains(option)) emptySet - option else emptySet + option

        assertEquals(1, newSet.size)
        assertTrue(newSet.contains(option))
    }

    @Test
    fun set_toggle_removes_existing_option() {
        val option = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val existingSet = setOf(option)

        val newSet = if (existingSet.contains(option)) existingSet - option else existingSet + option

        assertTrue(newSet.isEmpty())
    }

    @Test
    fun set_contains_check() {
        val option1 = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val option2 = PromptOption(2, "tone", "직설적", "직설적으로")
        val set = setOf(option1)

        assertTrue(set.contains(option1))
        assertFalse(set.contains(option2))
    }

    @Test
    fun promptingUiState_copy_with_new_tone() {
        val original = PromptingUiState()
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val updated = original.copy(selectedTone = setOf(toneOption))

        assertTrue(original.selectedTone.isEmpty())
        assertEquals(1, updated.selectedTone.size)
    }

    @Test
    fun promptingUiState_copy_with_new_format() {
        val original = PromptingUiState()
        val formatOption = PromptOption(1, "format", "3~5문장", "간결하게")
        val updated = original.copy(selectedFormat = setOf(formatOption))

        assertTrue(original.selectedFormat.isEmpty())
        assertEquals(1, updated.selectedFormat.size)
    }

    @Test
    fun promptOption_list_filter_by_key() {
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "prompt1"),
            PromptOption(2, "format", "3~5문장", "prompt2"),
            PromptOption(3, "tone", "직설적", "prompt3")
        )

        val toneOptions = options.filter { it.key == "tone" }

        assertEquals(2, toneOptions.size)
        assertTrue(toneOptions.all { it.key == "tone" })
    }

    @Test
    fun promptOption_list_filter_by_name() {
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "prompt1"),
            PromptOption(2, "tone", "직설적", "prompt2")
        )

        val filtered = options.filter { it.name == "존댓말" }

        assertEquals(1, filtered.size)
        assertEquals("존댓말", filtered[0].name)
    }

    @Test
    fun promptOption_find_by_id() {
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "prompt1"),
            PromptOption(2, "tone", "직설적", "prompt2")
        )

        val found = options.find { it.id == 2L }

        assertNotNull(found)
        assertEquals("직설적", found?.name)
    }

    @Test
    fun promptOption_find_by_id_not_found() {
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "prompt1")
        )

        val found = options.find { it.id == 999L }

        assertNull(found)
    }

    @Test
    fun set_add_multiple_options() {
        val option1 = PromptOption(1, "tone", "존댓말", "prompt1")
        val option2 = PromptOption(2, "tone", "직설적", "prompt2")
        val option3 = PromptOption(3, "tone", "결론우선", "prompt3")

        val set = setOf(option1, option2, option3)

        assertEquals(3, set.size)
        assertTrue(set.contains(option1))
        assertTrue(set.contains(option2))
        assertTrue(set.contains(option3))
    }

    @Test
    fun set_remove_option() {
        val option1 = PromptOption(1, "tone", "존댓말", "prompt1")
        val option2 = PromptOption(2, "tone", "직설적", "prompt2")
        val set = setOf(option1, option2)

        val newSet = set - option1

        assertEquals(1, newSet.size)
        assertFalse(newSet.contains(option1))
        assertTrue(newSet.contains(option2))
    }

    @Test
    fun set_filterNot_by_id() {
        val option1 = PromptOption(1, "tone", "존댓말", "prompt1")
        val option2 = PromptOption(2, "tone", "직설적", "prompt2")
        val set = setOf(option1, option2)

        val filtered = set.filterNot { it.id == 1L }.toSet()

        assertEquals(1, filtered.size)
        assertTrue(filtered.contains(option2))
    }

    @Test
    fun set_map_to_names() {
        val option1 = PromptOption(1, "tone", "존댓말", "prompt1")
        val option2 = PromptOption(2, "tone", "직설적", "prompt2")
        val set = setOf(option1, option2)

        val names = set.map { it.name }

        assertEquals(2, names.size)
        assertTrue(names.contains("존댓말"))
        assertTrue(names.contains("직설적"))
    }

    @Test
    fun set_replace_option_when_exists() {
        val original = PromptOption(1, "tone", "존댓말", "old prompt")
        val updated = PromptOption(1, "tone", "존댓말", "new prompt")
        val set = setOf(original)

        val replaced = set.map { if (it.id == updated.id) updated else it }.toSet()

        assertEquals(1, replaced.size)
        val result = replaced.first()
        assertEquals("new prompt", result.prompt)
    }

    @Test
    fun set_replace_option_when_not_exists() {
        val option1 = PromptOption(1, "tone", "존댓말", "prompt1")
        val option2 = PromptOption(2, "tone", "직설적", "prompt2")
        val set = setOf(option1)

        val replaced = set.map { if (it.id == option2.id) option2 else it }.toSet()

        assertEquals(1, replaced.size)
        assertTrue(replaced.contains(option1))
        assertFalse(replaced.contains(option2))
    }

    @Test
    fun promptOption_name_or_prompt_display() {
        val withName = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val withoutName = PromptOption(2, "tone", "", "직설적으로 작성하세요")

        val display1 = withName.name.ifBlank { withName.prompt }
        val display2 = withoutName.name.ifBlank { withoutName.prompt }

        assertEquals("존댓말", display1)
        assertEquals("직설적으로 작성하세요", display2)
    }

    @Test
    fun promptOption_lowercase_key_comparison() {
        val toneOption = PromptOption(1, "TONE", "존댓말", "prompt")

        assertEquals("tone", toneOption.key.lowercase())
    }

    @Test
    fun promptOption_key_case_insensitive_check() {
        val option = PromptOption(1, "Tone", "존댓말", "prompt")

        assertTrue(option.key.lowercase() == "tone")
    }

    @Test
    fun promptingUiState_empty_check() {
        val emptyState = PromptingUiState()

        assertTrue(emptyState.selectedTone.isEmpty())
        assertTrue(emptyState.selectedFormat.isEmpty())
    }

    @Test
    fun promptingUiState_non_empty_check() {
        val option = PromptOption(1, "tone", "존댓말", "prompt")
        val state = PromptingUiState(selectedTone = setOf(option))

        assertFalse(state.selectedTone.isEmpty())
    }

    @Test
    fun promptOption_size_count() {
        val option1 = PromptOption(1, "tone", "존댓말", "prompt1")
        val option2 = PromptOption(2, "tone", "직설적", "prompt2")
        val option3 = PromptOption(3, "tone", "결론우선", "prompt3")
        val set = setOf(option1, option2, option3)

        assertEquals(3, set.size)
    }

    @Test
    fun promptExample_tone_examples_exist() {
        // Simulating hardcoded examples
        val toneExamples = listOf(
            PromptExample("tone", "존댓말", "존댓말을 사용하여 정중하게 작성하세요"),
            PromptExample("tone", "직설적", "직설적이고 간결하게 요점만 전달하세요")
        )

        assertEquals(2, toneExamples.size)
        assertEquals("tone", toneExamples[0].key)
        assertEquals("tone", toneExamples[1].key)
    }

    @Test
    fun promptExample_format_examples_exist() {
        // Simulating hardcoded examples
        val formatExamples = listOf(
            PromptExample("format", "3~5문장", "3~5문장 이내로 간결하게 작성하세요"),
            PromptExample("format", "핵심키워드", "핵심 키워드를 강조하여 작성하세요")
        )

        assertEquals(2, formatExamples.size)
        assertEquals("format", formatExamples[0].key)
        assertEquals("format", formatExamples[1].key)
    }

    @Test
    fun promptExample_filter_by_name() {
        val examples = listOf(
            PromptExample("tone", "존댓말", "prompt1"),
            PromptExample("tone", "직설적", "prompt2")
        )

        val filtered = examples.filter { it.name != "존댓말" }

        assertEquals(1, filtered.size)
        assertEquals("직설적", filtered[0].name)
    }

    @Test
    fun promptOption_any_check() {
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "prompt1"),
            PromptOption(2, "tone", "직설적", "prompt2")
        )

        val hasExactMatch = options.any { it.name == "존댓말" }

        assertTrue(hasExactMatch)
    }

    @Test
    fun promptOption_any_check_negative() {
        val options = listOf(
            PromptOption(1, "tone", "존댓말", "prompt1")
        )

        val hasExactMatch = options.any { it.name == "직설적" }

        assertFalse(hasExactMatch)
    }
}
