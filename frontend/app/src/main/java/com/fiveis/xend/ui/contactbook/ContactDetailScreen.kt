package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.ui.profile.LanguageDialog
import com.fiveis.xend.ui.profile.languageDisplayText
import com.fiveis.xend.ui.theme.BackgroundLight
import com.fiveis.xend.ui.theme.BorderGray
import com.fiveis.xend.ui.theme.Gray200
import com.fiveis.xend.ui.theme.Gray400
import com.fiveis.xend.ui.theme.Gray600
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.Slate900
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.ToolbarIconTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    themeColor: Color,
    uiState: ContactDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenGroup: (Long) -> Unit,
    onComposeMail: (Contact) -> Unit,
    onUpdateContact: (String, String, String?, String?, String?, Long?, String?) -> Unit,
    onClearEditError: () -> Unit
) {
    val contact = uiState.contact
    val groups = uiState.groups
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var editNameField by rememberSaveable { mutableStateOf("") }
    var editEmailField by rememberSaveable { mutableStateOf("") }
    var editSubmitted by rememberSaveable { mutableStateOf(false) }
    var editSenderMode by rememberSaveable { mutableStateOf(RoleInputMode.PRESET) }
    var editSenderPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var editSenderManual by rememberSaveable { mutableStateOf("") }
    var editRecipientMode by rememberSaveable { mutableStateOf(RoleInputMode.PRESET) }
    var editRecipientPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var editRecipientManual by rememberSaveable { mutableStateOf("") }
    var editPersonalPromptField by rememberSaveable { mutableStateOf("") }
    var editSelectedGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editLanguagePreference by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isUpdating, uiState.updateError, showEditDialog) {
        if (!showEditDialog) return@LaunchedEffect
        if (editSubmitted && !uiState.isUpdating) {
            if (uiState.updateError == null) {
                showEditDialog = false
                editSubmitted = false
                onClearEditError()
            } else {
                editSubmitted = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                title = { Text("연락처 정보", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(
                        onClick = { contact?.let(onComposeMail) },
                        enabled = contact != null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                            Text("메일 쓰기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(innerPadding)
            .navigationBarsPadding()

        if (contact == null) {
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
            }
            return@Scaffold
        }

        Column(modifier = contentModifier) {
            // 헤더
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(themeColor)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "연락처",
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                contact.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (contact.email.isNotBlank()) {
                                Text(
                                    contact.email,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                editNameField = contact.name
                                editEmailField = contact.email
                                val senderValue = contact.context?.senderRole.orEmpty()
                                if (senderValue.isBlank()) {
                                    editSenderMode = RoleInputMode.PRESET
                                    editSenderPreset = null
                                    editSenderManual = ""
                                } else if (senderRoleOptionExamples.contains(senderValue)) {
                                    editSenderMode = RoleInputMode.PRESET
                                    editSenderPreset = senderValue
                                    editSenderManual = ""
                                } else {
                                    editSenderMode = RoleInputMode.MANUAL
                                    editSenderPreset = null
                                    editSenderManual = senderValue
                                }
                                val recipientValue = contact.context?.recipientRole.orEmpty()
                                if (recipientValue.isBlank()) {
                                    editRecipientMode = RoleInputMode.PRESET
                                    editRecipientPreset = null
                                    editRecipientManual = ""
                                } else if (recipientRoleOptionExamples.contains(recipientValue)) {
                                    editRecipientMode = RoleInputMode.PRESET
                                    editRecipientPreset = recipientValue
                                    editRecipientManual = ""
                                } else {
                                    editRecipientMode = RoleInputMode.MANUAL
                                    editRecipientPreset = null
                                    editRecipientManual = recipientValue
                                }
                                editPersonalPromptField = contact.context?.personalPrompt.orEmpty()
                                editSelectedGroupId = contact.group?.id
                                editLanguagePreference = contact.context?.languagePreference.orEmpty()
                                editSubmitted = false
                                onClearEditError()
                                showEditDialog = true
                            }
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "연락처 정보 수정")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        InfoRow(
                            contact.name + " 님께 나는",
                            contact.context?.senderRole ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                        InfoRow(
                            "나에게 " + contact.name + " 님은",
                            contact.context?.recipientRole ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                    InfoRow("개인 프롬프트", contact.context?.personalPrompt.orEmpty())

                    HorizontalDivider()
                    Text("소속 그룹", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    uiState.contact.group?.let { group ->
                        val groupColor = StableColor.forId(group.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onOpenGroup(group.id) }),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = groupColor.copy(alpha = 0.08f)),
                            border = BorderStroke(2.dp, groupColor)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(groupColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    group.emoji?.let {
                                        Text(text = it, fontSize = 20.sp)
                                    }
                                }
                                Spacer(Modifier.size(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = groupColor
                                    )
                                    if (group.description?.isNotBlank() ?: false) {
                                        Text(
                                            group.description,
                                            color = Color.DarkGray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.contact.group == null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Gray600.copy(alpha = 0.08f)),
                            border = BorderStroke(2.dp, Gray600)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("소속된 그룹이 없습니다.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()

                    contact.context?.languagePreference
                        ?.takeIf { it.isNotBlank() }
                        ?.let { language ->
                            Column {
                                Text(
                                    text = "메일 작성 언어",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = languageDisplayText(language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }

                    if (contact.context?.languagePreference.isNullOrBlank()) {
                        Column {
                            Text(
                                text = "메일 작성 언어",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "프로필 기본값",
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
//            uiState.contact.group?.let { g ->
//                GroupBriefCard(
//                    group = g,
//                    onClick = { onOpenGroup(g.id) }
//                )
//            }
//
//            if (uiState.contact.group == null) {
//                Card(
//                    modifier = Modifier
//                        .padding(horizontal = 16.dp)
//                        .fillMaxWidth(),
//                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(containerColor = Gray600.copy(alpha = 0.08f))
//                ) {
//                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                        Text("소속 그룹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
//                        Spacer(Modifier.height(4.dp))
//
//                        Card(
//                            modifier = Modifier
//                                .fillMaxWidth(),
//                            shape = RoundedCornerShape(16.dp),
//                            colors = CardDefaults.cardColors(containerColor = Gray600.copy(alpha = 0.08f)),
//                            border = BorderStroke(2.dp, Gray600)
//                        ) {
//                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
//                                Text("소속된 그룹이 없습니다.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
//                            }
//                        }
//                    }
//                }
//            }
        }
    }

    if (showEditDialog) {
        val trimmedName = editNameField.trim()
        val trimmedEmail = editEmailField.trim()
        val currentLanguagePreference = editLanguagePreference.trim()
        val finalSenderRole = when (editSenderMode) {
            RoleInputMode.PRESET -> editSenderPreset?.trim()
            RoleInputMode.MANUAL -> editSenderManual.trim()
        } ?: ""
        val finalRecipientRole = when (editRecipientMode) {
            RoleInputMode.PRESET -> editRecipientPreset?.trim()
            RoleInputMode.MANUAL -> editRecipientManual.trim()
        } ?: ""
        val selectedGroup = groups.firstOrNull { it.id == editSelectedGroupId }
            ?: contact?.group?.takeIf { it.id == editSelectedGroupId }
        val selectedGroupName = selectedGroup?.name ?: "그룹 없음"
        val isConfirmEnabled = trimmedName.isNotBlank() &&
            trimmedEmail.contains("@")

        EditContactDialog(
            name = editNameField,
            email = editEmailField,
            senderMode = editSenderMode,
            senderPreset = editSenderPreset,
            senderManual = editSenderManual,
            recipientMode = editRecipientMode,
            recipientPreset = editRecipientPreset,
            recipientManual = editRecipientManual,
            personalPrompt = editPersonalPromptField,
            selectedGroupName = selectedGroupName,
            groups = groups,
            errorMessage = uiState.updateError,
            isProcessing = uiState.isUpdating,
            isConfirmEnabled = isConfirmEnabled,
            languagePreference = editLanguagePreference,
            onNameChange = {
                editNameField = it
                if (uiState.updateError != null) onClearEditError()
            },
            onEmailChange = {
                editEmailField = it
                if (uiState.updateError != null) onClearEditError()
            },
            onSenderModeChange = { mode ->
                editSenderMode = mode
                if (mode == RoleInputMode.PRESET) {
                    editSenderManual = ""
                } else {
                    editSenderPreset = null
                }
            },
            onSenderPresetChange = { editSenderPreset = it },
            onSenderManualChange = { editSenderManual = it },
            onRecipientModeChange = { mode ->
                editRecipientMode = mode
                if (mode == RoleInputMode.PRESET) {
                    editRecipientManual = ""
                } else {
                    editRecipientPreset = null
                }
            },
            onRecipientPresetChange = { editRecipientPreset = it },
            onRecipientManualChange = { editRecipientManual = it },
            onPersonalPromptChange = { editPersonalPromptField = it },
            onGroupSelected = { editSelectedGroupId = it },
            onLanguagePreferenceChange = { editLanguagePreference = it },
            onDismiss = {
                if (!uiState.isUpdating) {
                    showEditDialog = false
                    editSubmitted = false
                    onClearEditError()
                }
            },
            onConfirm = {
                editSubmitted = true
                onUpdateContact(
                    trimmedName,
                    trimmedEmail,
                    finalSenderRole,
                    finalRecipientRole,
                    editPersonalPromptField.trim(),
                    editSelectedGroupId,
                    currentLanguagePreference
                )
            }
        )
    }
}

@Composable
private fun GroupBriefCard(group: Group, onClick: () -> Unit) {
    val themeColor = StableColor.forId(group.id)

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("소속 그룹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = BorderStroke(2.dp, themeColor)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(themeColor)
                    )
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            group.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        if (group.description?.isNotBlank() ?: false) {
                            Text(
                                group.description,
                                color = Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (group.options.isNotEmpty()) {
                Divider(Modifier.padding(vertical = 4.dp))
                Text("그룹 프롬프트 정보", style = MaterialTheme.typography.titleMedium)
                val tone = group.options.filter { it.key.equals("tone", true) }
                val format = group.options.filter { it.key.equals("format", true) }
                if (tone.isNotEmpty()) {
                    Text("문체 스타일", color = Color.Gray)
                    ChipRow(tone)
                }
                if (format.isNotEmpty()) {
                    Text("형식 가이드", color = Color.Gray)
                    ChipRow(format)
                }
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(value, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditContactDialog(
    name: String,
    email: String,
    senderMode: RoleInputMode,
    senderPreset: String?,
    senderManual: String,
    recipientMode: RoleInputMode,
    recipientPreset: String?,
    recipientManual: String,
    personalPrompt: String,
    selectedGroupName: String,
    groups: List<Group>,
    languagePreference: String,
    errorMessage: String?,
    isProcessing: Boolean,
    isConfirmEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSenderModeChange: (RoleInputMode) -> Unit,
    onSenderPresetChange: (String?) -> Unit,
    onSenderManualChange: (String) -> Unit,
    onRecipientModeChange: (RoleInputMode) -> Unit,
    onRecipientPresetChange: (String?) -> Unit,
    onRecipientManualChange: (String) -> Unit,
    onPersonalPromptChange: (String) -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onLanguagePreferenceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val directInputLabel = "직접 입력"
    val senderRoleOptions = senderRoleOptionExamples + directInputLabel
    val recipientRoleOptions = recipientRoleOptionExamples + directInputLabel
    val scrollState = rememberScrollState()
    var isSenderExpanded by remember { mutableStateOf(false) }
    var isRecipientExpanded by remember { mutableStateOf(false) }
    var isGroupExpanded by remember { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(20.dp)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "연락처 정보 수정",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = Gray600
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(scrollState)
                ) {
                    Text("이름", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = {
                            Text(
                                text = "이름을 입력하세요",
                                style = LocalTextStyle.current.copy(fontSize = 12.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("이메일", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = {
                            Text(
                                text = "example@email.com",
                                style = LocalTextStyle.current.copy(fontSize = 12.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("관계 - 나", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = isSenderExpanded,
                                onExpandedChange = { if (!isProcessing) isSenderExpanded = !isSenderExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = when (senderMode) {
                                        RoleInputMode.PRESET -> senderPreset ?: "나"
                                        RoleInputMode.MANUAL -> directInputLabel
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Group,
                                            contentDescription = null,
                                            tint = ToolbarIconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSenderExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .height(48.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isProcessing,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = BackgroundLight,
                                        unfocusedContainerColor = BackgroundLight,
                                        unfocusedBorderColor = Gray200,
                                        focusedBorderColor = Purple60,
                                        focusedTextColor = Slate900,
                                        unfocusedTextColor = Gray600
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isSenderExpanded,
                                    onDismissRequest = { isSenderExpanded = false }
                                ) {
                                    senderRoleOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 13.sp) },
                                            onClick = {
                                                isSenderExpanded = false
                                                if (option == directInputLabel) {
                                                    onSenderModeChange(RoleInputMode.MANUAL)
                                                    onSenderPresetChange(null)
                                                } else {
                                                    onSenderModeChange(RoleInputMode.PRESET)
                                                    onSenderPresetChange(option)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("관계 - 상대방", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = isRecipientExpanded,
                                onExpandedChange = { if (!isProcessing) isRecipientExpanded = !isRecipientExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = when (recipientMode) {
                                        RoleInputMode.PRESET -> recipientPreset ?: "상대방"
                                        RoleInputMode.MANUAL -> directInputLabel
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Group,
                                            contentDescription = null,
                                            tint = ToolbarIconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRecipientExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .height(48.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isProcessing,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = BackgroundLight,
                                        unfocusedContainerColor = BackgroundLight,
                                        unfocusedBorderColor = Gray200,
                                        focusedBorderColor = Purple60,
                                        focusedTextColor = Slate900,
                                        unfocusedTextColor = Gray600
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isRecipientExpanded,
                                    onDismissRequest = { isRecipientExpanded = false }
                                ) {
                                    recipientRoleOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 13.sp) },
                                            onClick = {
                                                isRecipientExpanded = false
                                                if (option == directInputLabel) {
                                                    onRecipientModeChange(RoleInputMode.MANUAL)
                                                    onRecipientPresetChange(null)
                                                } else {
                                                    onRecipientModeChange(RoleInputMode.PRESET)
                                                    onRecipientPresetChange(option)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (senderMode == RoleInputMode.MANUAL || recipientMode == RoleInputMode.MANUAL) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            if (senderMode == RoleInputMode.MANUAL) {
                                OutlinedTextField(
                                    value = senderManual,
                                    onValueChange = onSenderManualChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            text = "나의 역할",
                                            style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                            color = Gray400
                                        )
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    singleLine = true,
                                    enabled = !isProcessing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderGray,
                                        focusedBorderColor = Purple60
                                    )
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            Spacer(Modifier.width(8.dp))

                            if (recipientMode == RoleInputMode.MANUAL) {
                                OutlinedTextField(
                                    value = recipientManual,
                                    onValueChange = onRecipientManualChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            text = "상대방 역할",
                                            style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                            color = Gray400
                                        )
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    singleLine = true,
                                    enabled = !isProcessing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderGray,
                                        focusedBorderColor = Purple60
                                    )
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("관계 프롬프팅(선택사항)", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = personalPrompt,
                        onValueChange = onPersonalPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        placeholder = {
                            Text(
                                text = "상대방과의 관계를 설명해 주세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = Gray200,
                            focusedBorderColor = Purple60
                        ),
                        maxLines = 3
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("그룹 선택", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = isGroupExpanded,
                        onExpandedChange = { if (!isProcessing) isGroupExpanded = !isGroupExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedGroupName,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = Gray600,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .height(48.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isProcessing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundLight,
                                unfocusedContainerColor = BackgroundLight,
                                unfocusedBorderColor = Gray200,
                                focusedBorderColor = Purple60
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isGroupExpanded,
                            onDismissRequest = { isGroupExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("그룹 없음", fontSize = 13.sp) },
                                onClick = {
                                    onGroupSelected(null)
                                    isGroupExpanded = false
                                }
                            )
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(StableColor.forId(g.id))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                g.name,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        onGroupSelected(g.id)
                                        isGroupExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("메일 작성 언어", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isProcessing) { if (!isProcessing) showLanguageDialog = true }
                    ) {
                        OutlinedTextField(
                            value = languageDisplayText(languagePreference),
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            placeholder = {
                                Text(
                                    text = "프로필 기본값",
                                    style = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    color = Gray400
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            singleLine = true,
                            readOnly = true,
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TextPrimary,
                                disabledBorderColor = BorderGray,
                                focusedBorderColor = Purple60,
                                unfocusedBorderColor = BorderGray,
                                disabledContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            )
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소", fontSize = 14.sp, color = Gray600)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        enabled = isConfirmEnabled && !isProcessing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple60,
                            disabledContainerColor = Gray200
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("저장", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = if (languagePreference == "KOR") "Korean" else languagePreference,
            onLanguageSelected = { selected -> onLanguagePreferenceChange(selected) },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
