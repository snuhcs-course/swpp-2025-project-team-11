package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.ui.theme.Gray400
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    themeColor: Color,
    uiState: GroupDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMemberClick: (Contact) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onClearRenameError: () -> Unit,
    onRefreshPromptOptions: () -> Unit,
    onSavePromptOptions: (List<Long>) -> Unit,
    onAddPromptOption: AddPromptOptionHandler,
    onClearPromptError: () -> Unit
) {
    val group = uiState.group
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameField by rememberSaveable { mutableStateOf("") }
    var renameDescriptionField by rememberSaveable { mutableStateOf("") }
    var renameSubmitted by rememberSaveable { mutableStateOf(false) }

    var showPromptSheet by rememberSaveable { mutableStateOf(false) }
    var selectedToneIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var selectedFormatIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var originalToneIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var originalFormatIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var promptSubmitted by rememberSaveable { mutableStateOf(false) }
    var pendingPromptKey by rememberSaveable { mutableStateOf<String?>(null) }
    var newPromptName by rememberSaveable { mutableStateOf("") }
    var newPromptDescription by rememberSaveable { mutableStateOf("") }
    var addPromptError by rememberSaveable { mutableStateOf<String?>(null) }
    var isAddingPrompt by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isRenaming, uiState.renameError, showRenameDialog) {
        if (!showRenameDialog) return@LaunchedEffect
        if (renameSubmitted && !uiState.isRenaming) {
            if (uiState.renameError == null) {
                showRenameDialog = false
                renameSubmitted = false
                onClearRenameError()
            } else {
                renameSubmitted = false
            }
        }
    }

    LaunchedEffect(uiState.isPromptSaving, uiState.promptOptionsError, showPromptSheet) {
        if (!showPromptSheet) return@LaunchedEffect
        if (promptSubmitted && !uiState.isPromptSaving) {
            if (uiState.promptOptionsError == null) {
                showPromptSheet = false
                promptSubmitted = false
                onClearPromptError()
            } else {
                promptSubmitted = false
            }
        }
    }

    val navigationBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
            },
            title = { Text("그룹 정보", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        )

        if (group == null) {
            // 에러/로딩 카드
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(uiState.error ?: "불러오는 중...", color = Color.Gray)
                }
            }
            return
        }

        val currentDescription = group.description.orEmpty()
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val innerCardPadding = 32.dp // card content padding (16dp each side)
        val outerListPadding = 32.dp // LazyColumn horizontal padding (16dp each side)
        val memberPages = remember(group.members) { group.members.chunked(4) }
        val pageWidth = remember(configuration.screenWidthDp) {
            (screenWidth - innerCardPadding - outerListPadding).coerceAtLeast(0.dp)
        }
        val memberPagerState = rememberLazyListState()
        val memberPagerFling = rememberSnapFlingBehavior(memberPagerState)
        var isDescriptionExpanded by rememberSaveable(group.id) { mutableStateOf(false) }
        var canExpandDescription by remember(currentDescription) { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + navigationBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(themeColor)
                            )
                            Spacer(Modifier.size(12.dp))
                            Text(
                                group.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    renameField = group.name
                                    renameDescriptionField = group.description.orEmpty()
                                    renameSubmitted = false
                                    onClearRenameError()
                                    showRenameDialog = true
                                }
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "그룹 정보 수정")
                            }
                        }

                        if (currentDescription.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                currentDescription,
                                color = Color.DarkGray,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { result ->
                                    if (!isDescriptionExpanded) {
                                        val hasOverflow = result.hasVisualOverflow
                                        if (hasOverflow != canExpandDescription) {
                                            canExpandDescription = hasOverflow
                                        }
                                    }
                                }
                            )
                            if (canExpandDescription || isDescriptionExpanded) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    TextButton(
                                        onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                                        contentPadding = PaddingValues(horizontal = 0.dp)
                                    ) {
                                        Text(if (isDescriptionExpanded) "간단히 보기" else "더보기")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        Text("멤버 ${group.members.size}명", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            state = memberPagerState,
                            flingBehavior = memberPagerFling,
                            userScrollEnabled = memberPages.size > 1,
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(memberPages.size, key = { it }) { pageIndex ->
                                val chunk = memberPages[pageIndex]
                                Column(
                                    modifier = Modifier.width(pageWidth),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunk.forEach { c ->
                                        MemberRow(
                                            member = c,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { onMemberClick(c) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                PromptOptionsCard(
                    modifier = Modifier.fillMaxWidth(),
                    options = group.options,
                    onEditClick = {
                        val toneIds =
                            group.options.filter { it.key.equals("tone", ignoreCase = true) }.map { it.id }
                        val formatIds =
                            group.options.filter { it.key.equals("format", ignoreCase = true) }.map { it.id }
                        selectedToneIds = toneIds
                        selectedFormatIds = formatIds
                        originalToneIds = toneIds
                        originalFormatIds = formatIds
                        promptSubmitted = false
                        onClearPromptError()
                        onRefreshPromptOptions()
                        showPromptSheet = true
                    }
                )
            }
        }
    }

    if (showRenameDialog) {
        val trimmedName = renameField.trim()
        val trimmedDescription = renameDescriptionField.trim()
        val originalName = group?.name
        val originalDescription = group?.description.orEmpty().trim()
        val hasChanges = trimmedName != originalName || trimmedDescription != originalDescription

        RenameGroupDialog(
            value = renameField,
            description = renameDescriptionField,
            errorMessage = uiState.renameError,
            isProcessing = uiState.isRenaming,
            isConfirmEnabled = trimmedName.isNotBlank(),
            onValueChange = {
                renameField = it
                if (uiState.renameError != null) onClearRenameError()
            },
            onDescriptionChange = { renameDescriptionField = it },
            onDismiss = {
                showRenameDialog = false
                renameSubmitted = false
                renameDescriptionField = group?.description.orEmpty()
                onClearRenameError()
            },
            onConfirm = {
                val targetName = renameField.trim()
                val targetDescription = renameDescriptionField.trim()
                if (targetName.isBlank()) return@RenameGroupDialog
                renameSubmitted = true
                onRenameGroup(targetName, targetDescription)
            }
        )
    }

    if (showPromptSheet) {
        PromptOptionsBottomSheet(
            toneOptions = uiState.tonePromptOptions,
            formatOptions = uiState.formatPromptOptions,
            selectedToneIds = selectedToneIds,
            selectedFormatIds = selectedFormatIds,
            originalToneIds = originalToneIds,
            originalFormatIds = originalFormatIds,
            isSaving = uiState.isPromptSaving,
            errorMessage = uiState.promptOptionsError,
            onToggleTone = { id -> selectedToneIds = selectedToneIds.toggle(id) },
            onToggleFormat = { id -> selectedFormatIds = selectedFormatIds.toggle(id) },
            onReset = {
                selectedToneIds = originalToneIds
                selectedFormatIds = originalFormatIds
            },
            onDismiss = {
                if (!uiState.isPromptSaving) {
                    showPromptSheet = false
                    promptSubmitted = false
                    onClearPromptError()
                }
            },
            onSave = {
                promptSubmitted = true
                onSavePromptOptions((selectedToneIds + selectedFormatIds).distinct())
            },
            onRequestAddNew = { key ->
                pendingPromptKey = key
                newPromptName = ""
                newPromptDescription = ""
                addPromptError = null
            }
        )
    }

    val currentAddKey = pendingPromptKey
    if (currentAddKey != null) {
        AlertDialog(
            onDismissRequest = { if (!isAddingPrompt) pendingPromptKey = null },
            title = { Text("새 프롬프트 추가") },
            text = {
                Column {
                    Text(
                        text = "카테고리: ${if (currentAddKey == "tone") "문체 스타일" else "형식 가이드"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPromptName,
                        onValueChange = { newPromptName = it },
                        label = { Text("이름") },
                        singleLine = true,
                        enabled = !isAddingPrompt
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPromptDescription,
                        onValueChange = { newPromptDescription = it },
                        label = { Text("프롬프트 설명") },
                        minLines = 3,
                        enabled = !isAddingPrompt
                    )
                    if (!addPromptError.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(addPromptError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        addPromptError = null
                        isAddingPrompt = true
                        onAddPromptOption(
                            currentAddKey,
                            newPromptName.trim(),
                            newPromptDescription.trim(),
                            { created ->
                                if (currentAddKey == "tone") {
                                    selectedToneIds = (selectedToneIds + created.id).distinct()
                                } else {
                                    selectedFormatIds = (selectedFormatIds + created.id).distinct()
                                }
                                isAddingPrompt = false
                                pendingPromptKey = null
                                newPromptName = ""
                                newPromptDescription = ""
                            },
                            { msg ->
                                addPromptError = msg
                                isAddingPrompt = false
                            }
                        )
                    },
                    enabled = !isAddingPrompt && newPromptName.isNotBlank() && newPromptDescription.isNotBlank()
                ) {
                    if (isAddingPrompt) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("추가")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingPromptKey = null },
                    enabled = !isAddingPrompt
                ) { Text("취소") }
            }
        )
    }
}

@Composable
private fun MemberRow(member: Contact, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = member.name.firstOrNull()?.toString() ?: "?"
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StableColor.forId(member.id)),
                contentAlignment = Alignment.Center
            ) { Text(initial, color = Color.White, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.SemiBold)
                if (member.email.isNotBlank()) Text(member.email, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PromptOptionsCard(modifier: Modifier = Modifier, options: List<PromptOption>, onEditClick: () -> Unit) {
    val tone = options.filter { it.key.equals("tone", ignoreCase = true) }
    val format = options.filter { it.key.equals("format", ignoreCase = true) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI 프롬프트 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = "프롬프트 수정")
                }
            }

            if (tone.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("문체 스타일", color = TextSecondary, fontSize = 15.sp)
                ChipRow(tone)
            }

            if (format.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("형식 가이드", color = TextSecondary, fontSize = 15.sp)
                ChipRow(format)
            }

            if (tone.isEmpty() && format.isEmpty()) {
                Text(
                    "설정된 프롬프트가 없습니다.\n프롬프트를 설정해 더 나은 메일 생성을 경험하세요!",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ChipRow(list: List<PromptOption>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { TagChip(it.name.ifBlank { it.prompt }) }
    }
}

@Composable
private fun TagChip(text: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptOptionsBottomSheet(
    toneOptions: List<PromptOption>,
    formatOptions: List<PromptOption>,
    selectedToneIds: List<Long>,
    selectedFormatIds: List<Long>,
    originalToneIds: List<Long>,
    originalFormatIds: List<Long>,
    isSaving: Boolean,
    errorMessage: String?,
    onToggleTone: (Long) -> Unit,
    onToggleFormat: (Long) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRequestAddNew: (String) -> Unit
) {
    val thresholdFraction = 0.20f

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 드래그 중 시트의 현재 오프셋 추적
    var lastOffsetPx by rememberSaveable { mutableStateOf(0f) }

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

    // ▶ 변경 여부(저장 버튼 활성화 판단)
    val hasChanges =
        selectedToneIds.toSet() != originalToneIds.toSet() ||
            selectedFormatIds.toSet() != originalFormatIds.toSet()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더 영역
            item {
                Text(
                    "AI 프롬프트 설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    "그룹의 커뮤니케이션 스타일을 설정합니다",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            item { HorizontalDivider() }

            // 문체 스타일 섹션
            item {
                PromptOptionsSection(
                    title = "문체 스타일 프롬프트",
                    description = "메일의 말투와 문체를 설정합니다",
                    options = toneOptions,
                    selectedIds = selectedToneIds,
                    onToggle = onToggleTone,
                    onAddNew = { onRequestAddNew("tone") }
                )
            }

            item { HorizontalDivider() }

            // 형식 가이드 섹션
            item {
                PromptOptionsSection(
                    title = "형식 가이드 프롬프트",
                    description = "메일의 구조와 포맷을 설정합니다",
                    options = formatOptions,
                    selectedIds = selectedFormatIds,
                    onToggle = onToggleFormat,
                    onAddNew = { onRequestAddNew("format") }
                )
            }

            // 에러 메시지
            if (!errorMessage.isNullOrBlank()) {
                item {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            // 액션 버튼 영역(리스트 맨 아래에 고정되어 자연스럽게 스크롤됨)
            item {
                PromptSheetActionButtons(
                    hasChanges = hasChanges,
                    isSaving = isSaving,
                    onReset = onReset,
                    onSave = onSave
                )
            }

            // (선택) 네비게이션 바와 겹치지 않도록 아주 살짝 여백
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PromptOptionsSection(
    title: String,
    description: String,
    options: List<PromptOption>,
    selectedIds: List<Long>,
    onToggle: (Long) -> Unit,
    onAddNew: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        if (options.isEmpty()) {
            Text("아직 등록된 프롬프트가 없습니다", color = TextSecondary, fontSize = 12.sp)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { option ->
                    val selected = selectedIds.contains(option.id)
                    FilterChip(
                        selected = selected,
                        onClick = { onToggle(option.id) },
                        label = { Text(option.name.ifBlank { option.prompt }) },
                        leadingIcon = {
                            if (selected) {
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

                // 새 프롬프트 추가 버튼
                FilterChip(
                    selected = false,
                    onClick = onAddNew,
                    label = { Text("새 프롬프트 추가") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun PromptSheetActionButtons(hasChanges: Boolean, isSaving: Boolean, onReset: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            enabled = hasChanges && !isSaving
        ) { Text("초기화") }

        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.weight(1f)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("저장")
        }
    }
}

private fun List<Long>.toggle(id: Long): List<Long> = if (contains(id)) filterNot { it == id } else this + id

@Composable
private fun RenameGroupDialog(
    value: String,
    description: String,
    errorMessage: String?,
    isProcessing: Boolean,
    isConfirmEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("그룹 정보 수정", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormBlock(label = "그룹 이름") {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = {
                            Text(
                                text = "이름을 입력하세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                color = Gray400
                            )
                        },
                        singleLine = true,
                        enabled = !isProcessing
                    )
                }

                FormBlock(label = "그룹 설명") {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        placeholder = {
                            Text(
                                text = "그룹을 소개해 주세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                color = Gray400
                            )
                        },
                        enabled = !isProcessing,
                        minLines = 4
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled && !isProcessing
            ) { Text("저장") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) { Text("취소") }
        }
    )
}

// 그룹 정보 수정 시 dialog 팝업에서 이용
@Composable
private fun FormBlock(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}
