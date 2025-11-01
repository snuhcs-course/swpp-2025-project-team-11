package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.ui.theme.AddButtonBackground
import com.fiveis.xend.ui.theme.BackgroundGray
import com.fiveis.xend.ui.theme.BackgroundLight
import com.fiveis.xend.ui.theme.BorderGray
import com.fiveis.xend.ui.theme.Gray200
import com.fiveis.xend.ui.theme.Gray400
import com.fiveis.xend.ui.theme.Gray600
import com.fiveis.xend.ui.theme.IndigoText
import com.fiveis.xend.ui.theme.Orange
import com.fiveis.xend.ui.theme.OrangeSoftBg
import com.fiveis.xend.ui.theme.OrangeText
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.Slate900
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.ToolbarIconTint

enum class RoleInputMode { PRESET, MANUAL }

val senderRoleOptionExamples = listOf(
    "학생",
    "조교",
    "교수",
    "직장 부하 직원",
    "직장 동료",
    "직장 상사",
    "친한 친구"
)

val recipientRoleOptionExamples = listOf(
    "학생",
    "조교",
    "교수",
    "직장 부하 직원",
    "직장 동료",
    "직장 상사",
    "친한 친구"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    groups: List<Group> = emptyList(),
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSenderRoleChange: (String?) -> Unit,
    onRecipientRoleChange: (String?) -> Unit,
    onPersonalPromptChange: (String?) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onGmailContactsSync: () -> Unit,
    onBottomNavChange: (String) -> Unit = {},
    onGroupChange: (Group?) -> Unit = {}
) {
    val directInputLabel = "직접 입력"

    var isManualOpen by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    val senderRoleOptions = senderRoleOptionExamples + directInputLabel
    val recipientRoleOptions = recipientRoleOptionExamples + directInputLabel

    var senderRole by rememberSaveable { mutableStateOf<String?>(null) }
    var senderMode by rememberSaveable { mutableStateOf(RoleInputMode.PRESET) }
    var senderPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var senderManual by rememberSaveable { mutableStateOf("") }
    var isSenderExpanded by remember { mutableStateOf(false) }

    var recipientRole by rememberSaveable { mutableStateOf<String?>(null) }
    var recipientMode by rememberSaveable { mutableStateOf(RoleInputMode.PRESET) }
    var recipientPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var recipientManual by rememberSaveable { mutableStateOf("") }
    var isRecipientExpanded by remember { mutableStateOf(false) }

    var personalPrompt by rememberSaveable { mutableStateOf("") }
    var isGroupExpanded by remember { mutableStateOf(false) }
    var selectedGroup by rememberSaveable { mutableStateOf<Group?>(null) }
    val savable = name.isNotBlank() && email.contains("@")

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Purple60
                        )
                    }
                },
                title = {
                    Text(
                        "연락처 추가",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                actions = {
                    TextButton(
                        onClick = onAdd,
                        enabled = savable,
                        colors = ButtonDefaults.textButtonColors(contentColor = Purple60)
                    ) { Text("저장", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            )
        },
        bottomBar = {
            Column {
                Divider(color = BackgroundGray, thickness = 1.dp)
                BottomNavBar(
                    selected = "contacts",
                    onSelect = onBottomNavChange
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(BackgroundLight)
        ) {
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "연락처 가져오기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))

                // Gmail 동기화 카드
                ActionCard(
                    bg = AddButtonBackground,
                    border = Purple60,
                    titleColor = IndigoText,
                    subtitleColor = Purple60,
                    icon = Icons.Outlined.Email,
                    iconBg = Purple60,
                    title = "Gmail 연락처 동기화",
                    subtitle = "구글 계정의 모든 연락처를 가져옵니다",
                    trailingTint = Purple60,
                    onClick = onGmailContactsSync
                )

                Spacer(Modifier.height(12.dp))

                // 직접 입력 카드 (토글)
                ActionCard(
                    bg = OrangeSoftBg,
                    border = Orange,
                    titleColor = OrangeText,
                    subtitleColor = Orange,
                    icon = Icons.Outlined.PersonAdd,
                    iconBg = Orange,
                    title = "직접 입력",
                    subtitle = "연락처 정보를 직접 입력합니다",
                    trailingTint = Orange,
                    onClick = { isManualOpen = !isManualOpen },
                    isExpanded = isManualOpen
                )
            }

            if (isManualOpen) {
                Spacer(Modifier.height(16.dp))
                FormBlock(label = "이름") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onNameChange(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = "이름을 입력하세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60
                        )
                    )
                }

                FormBlock(label = "이메일 주소") {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onEmailChange(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = "이메일 주소를 입력하세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60
                        )
                    )
                }

                FormBlock(label = "관계") {
                    // 드롭다운 2개(좌: sender / 우: recipient)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ─── Sender Role ───
                        ExposedDropdownMenuBox(
                            expanded = isSenderExpanded,
                            onExpandedChange = { isSenderExpanded = !isSenderExpanded },
                            modifier = Modifier.weight(1f)
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
                                        tint = ToolbarIconTint
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isSenderExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .height(48.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundLight,
                                    unfocusedBorderColor = Gray200,
                                    focusedBorderColor = Purple60,
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Gray600,
                                    cursorColor = Purple60
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = isSenderExpanded,
                                onDismissRequest = { isSenderExpanded = false }
                            ) {
                                senderRoleOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            isSenderExpanded = false
                                            if (option == directInputLabel) {
                                                senderMode = RoleInputMode.MANUAL
                                                // 아직 입력 전이므로 콜백엔 null 혹은 현재 manual값 전달
                                                onSenderRoleChange(
                                                    senderManual.ifBlank { null }
                                                )
                                            } else {
                                                senderMode = RoleInputMode.PRESET
                                                senderPreset = option
                                                senderManual = ""
                                                onSenderRoleChange(option)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Recipient Role
                        ExposedDropdownMenuBox(
                            expanded = isRecipientExpanded,
                            onExpandedChange = { isRecipientExpanded = !isRecipientExpanded },
                            modifier = Modifier.weight(1f)
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
                                        tint = ToolbarIconTint
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isRecipientExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .height(48.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundLight,
                                    unfocusedBorderColor = Gray200,
                                    focusedBorderColor = Purple60,
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Gray600,
                                    cursorColor = Purple60
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = isRecipientExpanded,
                                onDismissRequest = { isRecipientExpanded = false }
                            ) {
                                recipientRoleOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            isRecipientExpanded = false
                                            if (option == directInputLabel) {
                                                recipientMode = RoleInputMode.MANUAL
                                                onRecipientRoleChange(
                                                    recipientManual.ifBlank { null }
                                                )
                                            } else {
                                                recipientMode = RoleInputMode.PRESET
                                                recipientPreset = option
                                                recipientManual = ""
                                                onRecipientRoleChange(option)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 아래 한 줄: "직접 입력" 선택된 쪽만 텍스트 입력칸 노출
                    if (senderMode == RoleInputMode.MANUAL || recipientMode == RoleInputMode.MANUAL) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            if (senderMode == RoleInputMode.MANUAL) {
                                OutlinedTextField(
                                    value = senderManual,
                                    onValueChange = {
                                        senderManual = it
                                        onSenderRoleChange(it.ifBlank { null })
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            text = "나의 역할을 적어주세요",
                                            style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                            color = Gray400
                                        )
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderGray,
                                        focusedBorderColor = Purple60
                                    )
                                )
                            }

                            if (senderMode == RoleInputMode.MANUAL && recipientMode == RoleInputMode.MANUAL) {
                                Spacer(Modifier.width(12.dp))
                            }

                            if (recipientMode == RoleInputMode.MANUAL) {
                                OutlinedTextField(
                                    value = recipientManual,
                                    onValueChange = {
                                        recipientManual = it
                                        onRecipientRoleChange(it.ifBlank { null })
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            text = "상대방의 역할을 적어주세요",
                                            style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                            color = Gray400
                                        )
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderGray,
                                        focusedBorderColor = Purple60
                                    )
                                )
                            }
                        }
                    }
                }

                FormBlock(label = "관계 프롬프팅(선택사항)") {
                    OutlinedTextField(
                        value = personalPrompt,
                        onValueChange = {
                            personalPrompt = it
                            onPersonalPromptChange(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = {
                            Text(
                                text = "상대방과의 관계를 설명해 주세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Gray200,
                            focusedBorderColor = Purple60
                        ),
                        maxLines = 5
                    )
                }

                FormBlock(label = "그룹") {
                    ExposedDropdownMenuBox(
                        expanded = isGroupExpanded,
                        onExpandedChange = { isGroupExpanded = !isGroupExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedGroup?.name ?: "그룹 선택",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = Gray600
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .height(48.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 15.sp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundLight,
                                unfocusedBorderColor = Gray200,
                                focusedBorderColor = Purple60
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isGroupExpanded,
                            onDismissRequest = { isGroupExpanded = false }
                        ) {
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
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedGroup = g
                                        isGroupExpanded = false
                                        onGroupChange(g)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ===== 레이아웃 유틸 ===== */

@Composable
private fun FormBlock(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            label,
            color = Gray600,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ActionCard(
    bg: Color,
    border: Color,
    titleColor: Color,
    subtitleColor: Color,
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    trailingTint: Color,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isExpanded) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(subtitle, color = subtitleColor, fontSize = 12.sp)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = trailingTint
            )
        }
    }
}
