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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    groups: List<Group> = emptyList(),
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onRelationshipRoleChange: (String?) -> Unit,
    onPersonalPromptChange: (String?) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onGmailContactsSync: () -> Unit,
    onBottomNavChange: (String) -> Unit = {},
    onGroupChange: (Group?) -> Unit = {}
) {
    var isManualOpen by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    val recipientRoleOptions = listOf("업무 동료", "개인 관계", "학술 관계")
    var isRecipientRoleExpanded by remember { mutableStateOf(false) }
    var recipientRole by rememberSaveable { mutableStateOf<String?>(null) }
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
                    ExposedDropdownMenuBox(
                        expanded = isRecipientRoleExpanded,
                        onExpandedChange = { isRecipientRoleExpanded = !isRecipientRoleExpanded }
                    ) {
                        OutlinedTextField(
                            value = recipientRole ?: "관계 선택",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = ToolbarIconTint
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRecipientRoleExpanded)
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
                                focusedBorderColor = Purple60,
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Gray600,
                                cursorColor = Purple60
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isRecipientRoleExpanded,
                            onDismissRequest = { isRecipientRoleExpanded = false }
                        ) {
                            recipientRoleOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        recipientRole = option
                                        isRecipientRoleExpanded = false
                                        onRelationshipRoleChange(option)
                                    }
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
                                text = "상대방과의 관계를 설명해주세요.",
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

                FormBlock(label = "그룹(선택사항)") {
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
