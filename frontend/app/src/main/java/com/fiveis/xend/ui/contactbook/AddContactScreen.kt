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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.Group

// colors는 전체적으로 수정 필요
private object XColors {
    val Bg = Color(0xFFFAFAFA) // 전체 배경
    val Slate900 = Color(0xFF1E293B) // 진한 제목
    val Indigo = Color(0xFF6366F1) // 주요 인디고(보더/아이콘/저장 텍스트)
    val IndigoText = Color(0xFF4338CA) // Gmail 카드 타이틀
    val IndigoSoftBg = Color(0xFFEFF6FF) // Gmail 카드 배경
    val Orange = Color(0xFFF59E0B) // 직접 입력 카드 보더/부제
    val OrangeText = Color(0xFFEA580C) // 직접 입력 카드 타이틀
    val OrangeSoftBg = Color(0xFFFFF7ED)
    val Gray600 = Color(0xFF64748B) // 필드 라벨
    val Gray400 = Color(0xFF94A3B8) // 플레이스홀더
    val Gray200 = Color(0xFFE2E8F0) // 아웃라인 보더
    val Gray50 = Color(0xFFF8FAFC) // 셀렉터 필드 배경
    val DividerTop = Color(0xFFE8EAED) // 바텀네비 상단 경계선
}

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
    val relationRoleOptions = listOf("업무 동료", "개인 관계", "가족", "대학 동기")
    var isRelationRoleExpanded by remember { mutableStateOf(false) }
    var relationRole by rememberSaveable { mutableStateOf<String?>(null) }
    var personalPrompt by rememberSaveable { mutableStateOf("") }
    var isGroupExpanded by remember { mutableStateOf(false) }
    var selectedGroup by rememberSaveable { mutableStateOf<Group?>(null) }
    val savable = name.isNotBlank() && email.contains("@")

    Scaffold(
        containerColor = XColors.Bg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = XColors.Indigo
                        )
                    }
                },
                title = {
                    Text(
                        "연락처 추가",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = XColors.Slate900
                    )
                },
                actions = {
                    TextButton(
                        onClick = onAdd,
                        enabled = savable,
                        colors = ButtonDefaults.textButtonColors(contentColor = XColors.Indigo)
                    ) { Text("저장", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            )
        },
        bottomBar = {
            Column {
                Divider(color = XColors.DividerTop, thickness = 1.dp)
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
                .background(XColors.Bg)
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
                    color = XColors.Slate900
                )
                Spacer(Modifier.height(16.dp))

                // Gmail 동기화 카드
                ActionCard(
                    bg = XColors.IndigoSoftBg,
                    border = XColors.Indigo,
                    titleColor = XColors.IndigoText,
                    subtitleColor = XColors.Indigo,
                    iconBg = XColors.Indigo,
                    title = "Gmail 연락처 동기화",
                    subtitle = "구글 계정의 모든 연락처를 가져옵니다",
                    trailingTint = XColors.Indigo,
                    onClick = onGmailContactsSync
                )

                Spacer(Modifier.height(12.dp))

                // 직접 입력 카드 (토글)
                ActionCard(
                    bg = XColors.OrangeSoftBg,
                    border = XColors.Orange,
                    titleColor = XColors.OrangeText,
                    subtitleColor = XColors.Orange,
                    iconBg = XColors.Orange,
                    title = "직접 입력",
                    subtitle = "연락처 정보를 직접 입력합니다",
                    trailingTint = XColors.Orange,
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
                        placeholder = { Text("이름을 입력하세요", color = XColors.Gray400) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = XColors.Gray200,
                            focusedBorderColor = XColors.Indigo
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
                        placeholder = { Text("example@gmail.com", color = XColors.Gray400) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = XColors.Gray200,
                            focusedBorderColor = XColors.Indigo
                        )
                    )
                }

                FormBlock(label = "관계") {
                    ExposedDropdownMenuBox(
                        expanded = isRelationRoleExpanded,
                        onExpandedChange = { isRelationRoleExpanded = !isRelationRoleExpanded }
                    ) {
                        OutlinedTextField(
                            value = relationRole ?: "관계 선택",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    tint = XColors.Gray600
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRelationRoleExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = XColors.Gray50,
                                unfocusedBorderColor = XColors.Gray200,
                                focusedBorderColor = XColors.Indigo,
                                focusedTextColor = XColors.Slate900,
                                unfocusedTextColor = XColors.Gray600,
                                cursorColor = XColors.Indigo
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isRelationRoleExpanded,
                            onDismissRequest = { isRelationRoleExpanded = false }
                        ) {
                            relationRoleOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        relationRole = option
                                        isRelationRoleExpanded = false
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
                        placeholder = { Text("상대방과의 관계를 설명해주세요.", color = XColors.Gray400) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = XColors.Gray200,
                            focusedBorderColor = XColors.Indigo
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
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = XColors.Gray600
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = XColors.Gray50,
                                unfocusedBorderColor = XColors.Gray200,
                                focusedBorderColor = XColors.Indigo
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
                                                    .background(g.color)
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
            color = XColors.Gray600,
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
        border = BorderStroke(if (isExpanded) 2.dp else 2.dp, border),
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
                Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.White)
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
