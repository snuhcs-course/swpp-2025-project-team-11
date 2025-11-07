package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.ui.theme.Gray400
import com.fiveis.xend.ui.theme.StableColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    themeColor: Color,
    uiState: GroupDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMemberClick: (Contact) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onClearRenameError: () -> Unit
) {
    val group = uiState.group
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameField by rememberSaveable { mutableStateOf("") }
    var renameDescriptionField by rememberSaveable { mutableStateOf("") }
    var renameSubmitted by rememberSaveable { mutableStateOf(false) }

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

        // 헤더 카드
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
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
                    Column(Modifier.weight(1f)) {
                        Text(
                            group.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            modifier = Modifier.clickable {
                                renameField = group.name
                                renameDescriptionField = group.description.orEmpty()
                                renameSubmitted = false
                                onClearRenameError()
                                showRenameDialog = true
                            }
                        )
                        if (group.description?.isNotBlank() ?: false) {
                            Text(
                                group.description,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    renameField = group.name
                                    renameDescriptionField = group.description.orEmpty()
                                    renameSubmitted = false
                                    onClearRenameError()
                                    showRenameDialog = true
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // 멤버 목록
                Text("멤버 ${group.members.size}명", fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 1.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(group.members) { c ->
                        MemberRow(c) { onMemberClick(c) }
                    }
                }
            }
        }

        PromptOptionsCard(options = group.options)
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
}

@Composable
private fun MemberRow(member: Contact, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
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
private fun PromptOptionsCard(options: List<PromptOption>) {
    val tone = options.filter { it.key.equals("tone", ignoreCase = true) }
    val format = options.filter { it.key.equals("format", ignoreCase = true) }

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI 프롬프트 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (tone.isNotEmpty()) {
                Text("문체 스타일", color = Color.Gray, fontSize = 12.sp)
                ChipRow(tone)
            }

            if (format.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("형식 가이드", color = Color.Gray, fontSize = 12.sp)
                ChipRow(format)
            }

            if (tone.isEmpty() && format.isEmpty()) {
                Text("설정된 프롬프트가 없습니다", color = Color.Gray)
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
            style = MaterialTheme.typography.bodySmall
        )
    }
}

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
