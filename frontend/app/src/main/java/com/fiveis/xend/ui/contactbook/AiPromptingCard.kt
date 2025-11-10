package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fiveis.xend.data.model.PromptOption
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 하드코딩된 예시 프롬프트 (클릭 시 백엔드에 추가됨)
 */
data class PromptExample(
    val key: String,
    val name: String,
    val prompt: String
)

private val TONE_EXAMPLES = listOf(
    PromptExample("tone", "존댓말", "존댓말을 사용하여 정중하게 작성하세요"),
    PromptExample("tone", "직설적", "직설적이고 간결하게 요점만 전달하세요"),
    PromptExample("tone", "결론우선", "결론을 먼저 제시하고 세부사항은 나중에 설명하세요"),
    PromptExample("tone", "격식적", "격식을 갖춘 공식적인 표현을 사용하세요"),
    PromptExample("tone", "친근함", "친근하고 부드러운 어조로 작성하세요"),
    PromptExample("tone", "신중함", "신중하고 조심스러운 표현을 사용하세요")
)

private val FORMAT_EXAMPLES = listOf(
    PromptExample("format", "3~5문장", "3~5문장 이내로 간결하게 작성하세요"),
    PromptExample("format", "핵심키워드", "핵심 키워드를 강조하여 작성하세요"),
    PromptExample("format", "구체적일정", "구체적인 날짜와 시간을 포함하여 작성하세요"),
    PromptExample("format", "인사말최소", "인사말을 최소화하고 본론 중심으로 작성하세요")
)

data class PromptingUiState(
    val selectedTone: Set<PromptOption> = emptySet(),
    val selectedFormat: Set<PromptOption> = emptySet()
)

typealias AddPromptOptionHandler = (
    key: String,
    name: String,
    prompt: String,
    onSuccess: (PromptOption) -> Unit,
    onError: (String) -> Unit
) -> Unit

/**
 * ===== 메인 카드 + "수정" 버튼 =====
 * “선택된 프롬프트 조합” 영역을 구성.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiPromptingCard(
    modifier: Modifier = Modifier,
    selectedState: PromptingUiState = PromptingUiState(),
    onValueChange: (PromptingUiState) -> Unit,
    allToneOptions: List<PromptOption> = emptyList(),
    allFormatOptions: List<PromptOption> = emptyList(),
    onAddPromptOption: AddPromptOptionHandler = { _, _, _, _, _ -> }
) {
    val thresholdFraction = 0.20f

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 드래그 중 시트의 현재 오프셋 추적
    var lastOffsetPx by rememberSaveable { mutableStateOf(0f) }

    var uiState by remember { mutableStateOf(selectedState) }

    LaunchedEffect(selectedState) {
        uiState = selectedState
    }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == androidx.compose.material3.SheetValue.Hidden) {
                lastOffsetPx >= screenHeightPx * thresholdFraction
            } else {
                true
            }
        }
    )

    LaunchedEffect(sheetState) {
        snapshotFlow { runCatching { sheetState.requireOffset() }.getOrDefault(0f) }
            .collectLatest { lastOffsetPx = it }
    }

    val scope = rememberCoroutineScope()

    var addDialogForKey: String? by remember { mutableStateOf(null) }
    var newName by remember { mutableStateOf("") }
    var newPrompt by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("선택된 프롬프트 조합", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            // 선택된 요약 칩들 (최대 5개 + “+n개”)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = buildList {
                    addAll(uiState.selectedTone)
                    addAll(uiState.selectedFormat)
                }
                val shown = options.take(5)
                val remain = (options.size - shown.size).coerceAtLeast(0)

                shown.forEach { label ->
                    SummaryChip(label = label.name)
                }
                if (remain > 0) {
                    SummaryChip(label = "+${remain}개")
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showSheet = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("수정") }
        }
    }

    if (showSheet) {
        PromptingBottomSheet(
            allToneOptions = allToneOptions,
            allFormatOptions = allFormatOptions,
            sheetState = sheetState,
            initial = uiState,
            onReset = {
                uiState = PromptingUiState() // 초기화
            },
            onSave = { newState ->
                uiState = newState
                onValueChange(newState)
                scope.launch {
                    sheetState.hide()
                    showSheet = false
                }
            },
            onDismiss = {
                showSheet = false
            },
            onRequestAddNew = { key ->
                addDialogForKey = key
                newName = ""
                newPrompt = ""
                addError = null
            },
            onSelectionChange = { newState ->
                uiState = newState
                onValueChange(newState)
            },
            onAddPromptOption = onAddPromptOption
        )
    }

    val currentKey = addDialogForKey
    if (currentKey != null) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) addDialogForKey = null },
            title = { Text("새 프롬프트 추가") },
            text = {
                Column {
                    Text("카테고리: ${if (currentKey == "tone") "문체 스타일" else "형식 가이드"}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPrompt,
                        onValueChange = { newPrompt = it },
                        label = { Text("프롬프트 설명") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                    if (addError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(addError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isAdding && newName.isNotBlank() && newPrompt.isNotBlank(),
                    onClick = {
                        addError = null
                        isAdding = true
                        onAddPromptOption(
                            currentKey,
                            newName.trim(),
                            newPrompt.trim(),
                            { created ->
                                val updated = if (currentKey == "tone") {
                                    uiState.copy(selectedTone = uiState.selectedTone + created)
                                } else {
                                    uiState.copy(selectedFormat = uiState.selectedFormat + created)
                                }

                                uiState = updated
                                onValueChange(updated)

                                isAdding = false
                                addDialogForKey = null
                            },
                            { msg ->
                                addError = msg
                                isAdding = false
                            }
                        )
                    }
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("추가")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isAdding) addDialogForKey = null }
                ) { Text("취소") }
            }
        )
    }
}

/** 선택 요약 칩(노란 배지처럼 보이는 간략 칩) */
@Composable
private fun SummaryChip(label: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

/**
 * ===== 바텀시트 =====
 * - 카테고리별 섹션 + 칩 토글
 * - 하단 "초기화" / "설정 저장"
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PromptingBottomSheet(
    allToneOptions: List<PromptOption> = emptyList(),
    allFormatOptions: List<PromptOption> = emptyList(),
    sheetState: SheetState,
    initial: PromptingUiState,
    onReset: () -> Unit,
    onSave: (PromptingUiState) -> Unit,
    onDismiss: () -> Unit,
    onRequestAddNew: (key: String) -> Unit,
    onSelectionChange: (PromptingUiState) -> Unit,
    onAddPromptOption: AddPromptOptionHandler = { _, _, _, _, _ -> }
) {
    var selectedTone by remember { mutableStateOf(initial.selectedTone) }
    var selectedFormat by remember { mutableStateOf(initial.selectedFormat) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AI 프롬프트 설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "그룹의 커뮤니케이션 스타일을 설정합니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { HorizontalDivider() }

            item {
                Section(
                    title = "문체 스타일 프롬프트",
                    description = "메일의 말투와 문체를 설정합니다",
                    options = allToneOptions,
                    selected = selectedTone,
                    onToggle = { option ->
                        selectedTone = selectedTone.toggle(option)
                        onSelectionChange(
                            PromptingUiState(
                                selectedTone = selectedTone,
                                selectedFormat = selectedFormat
                            )
                        )
                    },
                    onAddNew = { onRequestAddNew("tone") },
                    onAddPromptOption = onAddPromptOption
                )
            }

            item { HorizontalDivider() }

            item {
                Section(
                    title = "형식 가이드 프롬프트",
                    description = "메일의 구조와 포맷을 설정합니다",
                    options = allFormatOptions,
                    selected = selectedFormat,
                    onToggle = { option ->
                        selectedFormat = selectedFormat.toggle(option)
                        onSelectionChange(
                            PromptingUiState(
                                selectedTone = selectedTone,
                                selectedFormat = selectedFormat
                            )
                        )
                    },
                    onAddNew = { onRequestAddNew("format") },
                    onAddPromptOption = onAddPromptOption
                )
            }

            item {
                RowActionButtons(
                    onReset = {
                        onReset()
                        selectedTone = PromptingUiState().selectedTone
                        selectedFormat = PromptingUiState().selectedFormat
                        onSelectionChange(PromptingUiState())
                    },
                    onSave = {
                        onSave(
                            PromptingUiState(
                                selectedTone = selectedTone,
                                selectedFormat = selectedFormat
                            )
                        )
                    }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    title: String,
    description: String,
    options: List<PromptOption>,
    selected: Set<PromptOption>,
    onToggle: (PromptOption) -> Unit,
    onAddNew: () -> Unit,
    onAddPromptOption: AddPromptOptionHandler
) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))

    // 예시 리스트 결정
    val examples = if (title.contains("문체")) TONE_EXAMPLES else FORMAT_EXAMPLES
    // DB에 없는 예시만 필터링
    val availableExamples = examples.filter { example ->
        !options.any { it.name == example.name }
    }

    var addingExample by remember { mutableStateOf<String?>(null) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 이미 DB에 있는 옵션들
        options.forEach { opt ->
            val isSelected = selected.contains(opt)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(opt) },
                label = { Text(opt.name) },
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckBox,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }

        // 하드코딩된 예시들 (아직 DB에 없는 것)
        availableExamples.forEach { example ->
            val isAdding = addingExample == example.name
            FilterChip(
                selected = false,
                enabled = !isAdding,
                onClick = {
                    addingExample = example.name
                    onAddPromptOption(
                        example.key,
                        example.name,
                        example.prompt,
                        { created ->
                            // 성공 시 자동으로 선택
                            onToggle(created)
                            addingExample = null
                        },
                        { _ ->
                            addingExample = null
                        }
                    )
                },
                label = {
                    if (isAdding) {
                        Text("추가 중...")
                    } else {
                        Text(example.name)
                    }
                },
                leadingIcon = {
                    if (isAdding) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }

        // 새 프롬프트 추가 버튼
        FilterChip(
            selected = false,
            onClick = onAddNew,
            label = { Text("새 프롬프트 추가") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
        )
    }
}

@Composable
private fun RowActionButtons(onReset: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onReset) { Text("초기화") }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f)
        ) { Text("저장") }
    }
}

/** Set 토글 helper */
private fun Set<PromptOption>.toggle(name: PromptOption): Set<PromptOption> =
    if (contains(name)) this - name else this + name
