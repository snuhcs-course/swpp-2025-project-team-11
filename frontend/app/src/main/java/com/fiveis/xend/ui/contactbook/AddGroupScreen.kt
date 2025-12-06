package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.ui.theme.BackgroundLight
import com.fiveis.xend.ui.theme.BorderGray
import com.fiveis.xend.ui.theme.Gray400
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    uiState: AddGroupUiState,
    promptingState: PromptingUiState = PromptingUiState(),
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onGroupNameChange: (String) -> Unit,
    onGroupDescriptionChange: (String) -> Unit,
    onGroupEmojiChange: (String) -> Unit = {},
    onPromptOptionsChange: (PromptingUiState) -> Unit,
    onAddPromptOption: AddPromptOptionHandler = { _, _, _, _, _ -> },
    onUpdatePromptOption: UpdatePromptOptionHandler = { _, _, _, _, _ -> },
    onDeletePromptOption: DeletePromptOptionHandler = { _, _, _ -> },
    members: List<Contact> = emptyList(),
    onAddMember: () -> Unit = {},
    onMemberClick: () -> Unit = {},
    onBottomNavChange: (String) -> Unit = {}
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    var groupDescription by rememberSaveable { mutableStateOf("") }
    var groupEmoji by rememberSaveable { mutableStateOf("") }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    // 등록된 연락처 "+N명 더보기" 토글 상태
    var isMembersExpanded by rememberSaveable { mutableStateOf(false) }
    val savable = groupName.isNotBlank()

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
                        "그룹 추가",
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
        }
//        bottomBar = { BottomNavBar(selected = "contacts", onSelect = onBottomNavChange) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FormBlock(label = "그룹 이름") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = {
                                groupName = it
                                onGroupNameChange(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("groupNameInput"),
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

                        // 이모티콘 선택 버튼
                        Surface(
                            onClick = { showEmojiPicker = true },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (groupEmoji.isNotEmpty()) {
                                Purple60.copy(
                                    alpha = 0.1f
                                )
                            } else {
                                Gray400.copy(alpha = 0.1f)
                            },
                            border = BorderStroke(1.dp, if (groupEmoji.isNotEmpty()) Purple60 else BorderGray)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (groupEmoji.isNotEmpty()) {
                                    Text(
                                        text = groupEmoji,
                                        fontSize = 24.sp
                                    )
                                } else {
                                    Text(
                                        text = "😀",
                                        fontSize = 20.sp,
                                        color = Gray400
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                FormBlock(label = "그룹 설명") {
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = {
                            groupDescription = it
                            onGroupDescriptionChange(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = "그룹을 소개해 주세요",
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

            item {
                Text(
                    text = "AI 프롬프트 설정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            item {
                AiPromptingCard(
                    modifier = Modifier.fillMaxWidth(),
                    selectedState = promptingState,
                    onValueChange = onPromptOptionsChange,
                    allToneOptions = uiState.tonePromptOptions,
                    allFormatOptions = uiState.formatPromptOptions,
                    onAddPromptOption = onAddPromptOption,
                    onUpdatePromptOption = onUpdatePromptOption,
                    onDeletePromptOption = onDeletePromptOption
                )
            }

            /*
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("예상 결과:", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "12월 20일 오전 10시 회의자료 검토 요청드립니다. 첨부파일 확인 후 피드백 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
             */

            // ===== 멤버 헤더 =====
            item {
                android.util.Log.d("AddGroupScreen", "Rendering members header: ${members.size} members")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "그룹 멤버 (${members.size}명)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onAddMember) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("추가")
                    }
                }
            }

            // ===== 멤버 리스트 (처음엔 3명만, 더보기 시 전체) =====
            val visible = if (isMembersExpanded) members else members.take(3)
            itemsIndexed(visible, key = { _, m -> m.name }) { _, member ->
                MemberRow(
                    member = member,
                    onClick = { onMemberClick() }
                )
                Spacer(Modifier.height(6.dp))
            }

            // ===== +N명 더보기 토글 =====
            if (!isMembersExpanded && members.size > 3) {
                val remain = members.size - 3
                item {
                    MoreRow(
                        text = "+${remain}명 더보기",
                        onClick = { isMembersExpanded = true }
                    )
                }
            }
        }
    }

    // 이모티콘 선택 다이얼로그
    if (showEmojiPicker) {
        EmojiPickerDialog(
            currentEmoji = groupEmoji.ifEmpty { null },
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { emoji ->
                groupEmoji = emoji ?: ""
                onGroupEmojiChange(groupEmoji)
                showEmojiPicker = false
            }
        )
    }
}

/* --------------------------------- UI 파츠 --------------------------------- */

@Composable
private fun MemberRow(member: Contact, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = member.name.firstOrNull()?.toString() ?: "?"
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(StableColor.forId(member.id)),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// +N명 더보기
@Composable
private fun MoreRow(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * 임시 Color 목록
 */
@Composable
private fun randomStableColorFor(seed: String): Color {
    val colors = listOf(
        Color(0xFF5A7DFF),
        Color(0xFF35C6A8),
        Color(0xFFF4A425),
        Color(0xFFEF6E6E),
        Color(0xFF7A6FF0),
        Color(0xFF3DB2FF)
    )
    val idx = (seed.firstOrNull()?.code ?: 0) % colors.size
    return colors[idx]
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
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectDialog(
    contacts: List<Contact>,
    selectedContacts: List<Contact>,
    onDismiss: () -> Unit,
    onConfirm: (List<Contact>) -> Unit
) {
    val sortedContacts = remember(contacts) { contacts.sortedBy { it.name } }
    // ID만 저장해서 Set 비교 문제 해결
    var tempSelectedIds by remember { mutableStateOf(selectedContacts.map { it.id }.toSet()) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "연락처 선택",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "그룹에 추가할 연락처를 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "등록된 연락처가 없습니다",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        itemsIndexed(sortedContacts) { _, contact ->
                            val isSelected = tempSelectedIds.contains(contact.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelectedIds = if (isSelected) {
                                            tempSelectedIds - contact.id
                                        } else {
                                            tempSelectedIds + contact.id
                                        }
                                    },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    Color.White
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(StableColor.forId(contact.id)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.firstOrNull()?.uppercase() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = contact.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "선택됨",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            val selected = contacts.filter { tempSelectedIds.contains(it.id) }
                            onConfirm(selected)
                        }
                    ) {
                        Text("확인 (${tempSelectedIds.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPickerDialog(currentEmoji: String?, onDismiss: () -> Unit, onEmojiSelected: (String?) -> Unit) {
    var selectedCategoryIndex by rememberSaveable(currentEmoji) {
        mutableStateOf(
            emojiCategories.indexOfFirst { currentEmoji != null && it.emojis.contains(currentEmoji) }
                .takeIf { it >= 0 } ?: 0
        )
    }
    val emojiList = emojiCategories.getOrNull(selectedCategoryIndex)?.emojis ?: emptyList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "심볼 이모지 선택",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentEmoji != null) {
                        TextButton(onClick = { onEmojiSelected(null) }) {
                            Text("제거", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emojiCategories"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(emojiCategories.size) { index ->
                        val category = emojiCategories[index]
                        FilterChip(
                            selected = index == selectedCategoryIndex,
                            onClick = { selectedCategoryIndex = index },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Purple60.copy(alpha = 0.15f),
                                selectedLabelColor = Purple60
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .testTag("emojiGrid"),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojiList) { emoji ->
                        Surface(
                            onClick = { onEmojiSelected(emoji) },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (emoji == currentEmoji) {
                                Purple60.copy(alpha = 0.2f)
                            } else {
                                Color.Transparent
                            },
                            border = if (emoji == currentEmoji) {
                                BorderStroke(2.dp, Purple60)
                            } else {
                                null
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기")
                    }
                }
            }
        }
    }
}

private data class EmojiCategory(val name: String, val emojis: List<String>)

private val faceAndEmotionEmojis = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
    "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
    "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
    "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "🤩", "🥸",
    "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️",
    "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤",
    "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱",
    "😨", "😰", "😥", "😓", "🤗", "🤔", "🤭", "🤫",
    "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦",
    "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵",
    "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕"
)

private val specialFaceEmojis = listOf(
    "🤑", "🤠", "😈", "👿", "👹", "👺", "🤡", "💩",
    "👻", "💀", "☠️", "👽", "👾", "🤖", "🎃", "😺",
    "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾"
)

private val handGestureEmojis = listOf(
    "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
    "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆",
    "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛",
    "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️"
)

private val bodyEmojis = listOf(
    "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃",
    "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅",
    "👄", "💋", "🩸"
)

private val peopleEmojis = listOf(
    "👶", "👧", "🧒", "👦", "👩", "🧑", "👨", "👩‍🦱",
    "🧑‍🦱", "👨‍🦱", "👩‍🦰", "🧑‍🦰", "👨‍🦰", "👱‍♀️", "👱", "👱‍♂️",
    "👩‍🦳", "🧑‍🦳", "👨‍🦳", "👩‍🦲", "🧑‍🦲", "👨‍🦲", "🧔", "🧔‍♀️",
    "🧔‍♂️", "👵", "🧓", "👴", "👲", "👳‍♀️", "👳", "👳‍♂️"
)

private val professionEmojis = listOf(
    "👮‍♀️", "👮", "👮‍♂️", "👷‍♀️", "👷", "👷‍♂️", "💂‍♀️", "💂",
    "💂‍♂️", "🕵️‍♀️", "🕵️", "🕵️‍♂️", "👩‍⚕️", "🧑‍⚕️", "👨‍⚕️", "👩‍🌾",
    "🧑‍🌾", "👨‍🌾", "👩‍🍳", "🧑‍🍳", "👨‍🍳", "👩‍🎓", "🧑‍🎓", "👨‍🎓",
    "👩‍🎤", "🧑‍🎤", "👨‍🎤", "👩‍🏫", "🧑‍🏫", "👨‍🏫", "👩‍🏭", "🧑‍🏭",
    "👨‍🏭", "👩‍💻", "🧑‍💻", "👨‍💻", "👩‍💼", "🧑‍💼", "👨‍💼", "👩‍🔧",
    "🧑‍🔧", "👨‍🔧", "👩‍🔬", "🧑‍🔬", "👨‍🔬", "👩‍🎨", "🧑‍🎨", "👨‍🎨",
    "👩‍🚒", "🧑‍🚒", "👨‍🚒", "👩‍✈️", "🧑‍✈️", "👨‍✈️", "👩‍🚀", "🧑‍🚀",
    "👨‍🚀", "👩‍⚖️", "🧑‍⚖️", "👨‍⚖️", "👰‍♀️", "👰", "👰‍♂️", "🤵‍♀️",
    "🤵", "🤵‍♂️", "👸", "🤴", "🥷", "🦸‍♀️", "🦸", "🦸‍♂️",
    "🦹‍♀️", "🦹", "🦹‍♂️", "🧙‍♀️", "🧙", "🧙‍♂️", "🧚‍♀️", "🧚",
    "🧚‍♂️", "🧛‍♀️", "🧛", "🧛‍♂️", "🧜‍♀️", "🧜", "🧜‍♂️", "🧝‍♀️",
    "🧝", "🧝‍♂️", "🧞‍♀️", "🧞", "🧞‍♂️", "🧟‍♀️", "🧟", "🧟‍♂️"
)

private val mammalEmojis = listOf(
    "🐶", "🐕", "🦮", "🐕‍🦺", "🐩", "🐺", "🦊", "🦝",
    "🐱", "🐈", "🐈‍⬛", "🦁", "🐯", "🐅", "🐆", "🐴",
    "🐎", "🦄", "🦓", "🦌", "🦬", "🐮", "🐂", "🐃",
    "🐄", "🐷", "🐖", "🐗", "🐽", "🐏", "🐑", "🐐",
    "🐪", "🐫", "🦙", "🦒", "🐘", "🦣", "🦏", "🦛",
    "🐭", "🐁", "🐀", "🐹", "🐰", "🐇", "🐿️", "🦫",
    "🦔", "🦇", "🐻", "🐻‍❄️", "🐨", "🐼", "🦥", "🦦",
    "🦨", "🦘", "🦡"
)

private val birdEmojis = listOf(
    "🐔", "🐓", "🐣", "🐤", "🐥", "🐦", "🐧", "🕊️",
    "🦅", "🦆", "🦢", "🦉", "🦤", "🪶", "🦩", "🦚",
    "🦜"
)

private val reptileAmphibianEmojis = listOf(
    "🐸", "🐊", "🐢", "🦎", "🐍", "🐲", "🐉", "🦕",
    "🦖"
)

private val seaEmojis = listOf(
    "🐳", "🐋", "🐬", "🦭", "🐟", "🐠", "🐡", "🦈",
    "🐙", "🐚", "🦀", "🦞", "🦐", "🦑", "🪸"
)

private val insectEmojis = listOf(
    "🐌", "🦋", "🐛", "🐜", "🐝", "🪲", "🐞", "🦗",
    "🪳", "🕷️", "🕸️", "🦂", "🦟", "🪰", "🪱", "🦠"
)

private val plantEmojis = listOf(
    "💐", "🌸", "💮", "🏵️", "🌹", "🥀", "🌺", "🌻",
    "🌼", "🌷", "🌱", "🪴", "🌲", "🌳", "🌴", "🌵",
    "🌾", "🌿", "☘️", "🍀", "🍁", "🍂", "🍃", "🪹",
    "🪺"
)

private val fruitEmojis = listOf(
    "🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🥭",
    "🍎", "🍏", "🍐", "🍑", "🍒", "🍓", "🫐", "🥝",
    "🍅", "🫒", "🥥"
)

private val vegetableEmojis = listOf(
    "🥑", "🍆", "🥔", "🥕", "🌽", "🌶️", "🫑", "🥒",
    "🥬", "🥦", "🧄", "🧅", "🍄", "🥜", "🫘", "🌰"
)

private val mealEmojis = listOf(
    "🍞", "🥐", "🥖", "🫓", "🥨", "🥯", "🥞", "🧇",
    "🧀", "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕",
    "🌭", "🥪", "🌮", "🌯", "🫔", "🥙", "🧆", "🥚",
    "🍳", "🥘", "🍲", "🫕", "🥣", "🥗", "🍿", "🧈",
    "🧂", "🥫"
)

private val asianMealEmojis = listOf(
    "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝", "🍠",
    "🍢", "🍣", "🍤", "🍥", "🥮", "🍡", "🥟", "🥠",
    "🥡"
)

private val dessertEmojis = listOf(
    "🦀", "🦞", "🦐", "🦑", "🦪", "🍦", "🍧", "🍨",
    "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬",
    "🍭", "🍮", "🍯"
)

private val drinkEmojis = listOf(
    "🍼", "🥛", "☕", "🫖", "🍵", "🍶", "🍾", "🍷",
    "🍸", "🍹", "🍺", "🍻", "🥂", "🥃", "🥤", "🧋",
    "🧃", "🧉", "🧊"
)

private val kitchenEmojis = listOf(
    "🥢",
    "🍽️",
    "🍴",
    "🥄",
    "🔪",
    "🏺"
)

private val sportsEmojis = listOf(
    "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉",
    "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍",
    "🏏", "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿",
    "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌",
    "🎿", "⛷️", "🏂", "🪂", "🏋️‍♀️", "🏋️", "🏋️‍♂️", "🤼‍♀️",
    "🤼", "🤼‍♂️", "🤸‍♀️", "🤸", "🤸‍♂️", "⛹️‍♀️", "⛹️", "⛹️‍♂️",
    "🤺", "🤾‍♀️", "🤾", "🤾‍♂️", "🏌️‍♀️", "🏌️", "🏌️‍♂️", "🏇",
    "🧘‍♀️", "🧘", "🧘‍♂️", "🏄‍♀️", "🏄", "🏄‍♂️", "🏊‍♀️", "🏊",
    "🏊‍♂️", "🤽‍♀️", "🤽", "🤽‍♂️", "🚣‍♀️", "🚣", "🚣‍♂️", "🧗‍♀️",
    "🧗", "🧗‍♂️", "🚵‍♀️", "🚵", "🚵‍♂️", "🚴‍♀️", "🚴", "🚴‍♂️",
    "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🏵️", "🎗️"
)

private val transportEmojis = listOf(
    "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑",
    "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🦯", "🦽",
    "🦼", "🛴", "🚲", "🛵", "🏍️", "🛺", "🚨", "🚔",
    "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃", "🚋",
    "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚆", "🚇",
    "🚊", "🚉", "✈️", "🛫", "🛬", "🛩️", "💺", "🛰️",
    "🚁", "🛶", "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚢",
    "⚓", "⛽", "🚧", "🚦", "🚥", "🚏", "🗺️", "🗿"
)

private val placeEmojis = listOf(
    "🗼", "🗽", "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋",
    "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅", "🌆",
    "🌇", "🌉", "♨️", "🎠", "🎡", "🎢", "💈", "🎪",
    "🚂", "🚃", "🚄", "🚅", "🚆", "🚇", "🚈", "🚉",
    "🚊", "🚝", "🚞", "🚋", "🚌", "🚍", "🚎", "🚐"
)

private val deviceEmojis = listOf(
    "⌚", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️",
    "🖲️", "🕹️", "🗜️", "💾", "💿", "📀", "📼", "📷",
    "📸", "📹", "🎥", "📽️", "🎞️", "📞", "☎️", "📟",
    "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "🧭", "⏱️",
    "⏲️", "⏰", "🕰️", "⌛", "⏳", "📡", "🔋", "🔌"
)

private val toolEmojis = listOf(
    "💡", "🔦", "🕯️", "🪔", "🧯", "🛢️", "💸", "💵",
    "💴", "💶", "💷", "🪙", "💰", "💳", "🪪", "💎",
    "⚖️", "🪜", "🧰", "🪛", "🔧", "🔨", "⚒️", "🛠️",
    "⛏️", "🪚", "🔩", "⚙️", "🪤", "🧱", "⛓️", "🧲",
    "🔫", "💣", "🧨", "🪓", "🔪", "🗡️", "⚔️", "🛡️",
    "🚬", "⚰️", "🪦", "⚱️", "🏺", "🔮", "📿", "🧿"
)

private val stationeryEmojis = listOf(
    "💈", "⚗️", "🔭", "🔬", "🕳️", "🩹", "🩺", "💊",
    "💉", "🩸", "🧬", "🦠", "🧫", "🧪", "🌡️", "🧹",
    "🪠", "🧺", "🧻", "🪣", "🧼", "🪥", "🧽", "🧴",
    "🛎️", "🔑", "🗝️", "🚪", "🪑", "🛋️", "🛏️", "🛌",
    "🧸", "🪆", "🖼️", "🪞", "🪟", "🛍️", "🛒", "🎁",
    "🎈", "🎏", "🎀", "🪄", "🪅", "🎊", "🎉", "🎎",
    "🏮", "🎐", "🧧", "✉️", "📩", "📨", "📧", "💌",
    "📥", "📤", "📦", "🏷️", "🪧", "📪", "📫", "📬",
    "📭", "📮", "📯", "📜", "📃", "📄", "📑", "🧾",
    "📊", "📈", "📉", "🗒️", "🗓️", "📆", "📅", "🗑️",
    "📇", "🗃️", "🗳️", "🗄️", "📋", "📁", "📂", "🗂️",
    "🗞️", "📰", "📓", "📔", "📒", "📕", "📗", "📘",
    "📙", "📚", "📖", "🔖", "🧷", "🔗", "📎", "🖇️",
    "📐", "📏", "🧮", "📌", "📍", "✂️", "🖊️", "🖋️",
    "✒️", "🖌️", "🖍️", "📝", "✏️", "🔍", "🔎", "🔏",
    "🔐", "🔒", "🔓"
)

private val natureWeatherEmojis = listOf(
    "🌍", "🌎", "🌏", "🌐", "🗺️", "🗾", "🧭", "🏔️",
    "⛰️", "🌋", "🗻", "🏕️", "🏖️", "🏜️", "🏝️", "🏞️",
    "🏟️", "🏛️", "🏗️", "🧱", "🪨", "🪵", "🛖", "🏘️",
    "🏚️", "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦",
    "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰",
    "💒", "🗼", "🗽", "⛪", "🕌", "🛕", "🕍", "⛩️",
    "🕋", "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅",
    "🌆", "🌇", "🌉", "♨️", "🎠", "🎡", "🎢", "💈",
    "🎪", "🌌", "🎇", "🎆", "🌠", "🌈", "☀️", "🌤️",
    "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️",
    "❄️", "☃️", "⛄", "🌬️", "💨", "💧", "💦", "☔",
    "☂️", "🌊", "🌫️", "⭐", "✨", "🌟", "💫", "⚡",
    "🔥", "☄️", "🌡️"
)

private val moonStarEmojis = listOf(
    "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔",
    "🌙", "🌚", "🌛", "🌜", "🌝"
)

private val musicEventEmojis = listOf(
    "🎃", "🎄", "🎆", "🎇", "🧨", "✨", "🎈", "🎉",
    "🎊", "🎋", "🎍", "🎎", "🎏", "🎐", "🎑", "🧧",
    "🎀", "🎁", "🎗️", "🎟️", "🎫", "🎖️", "🏆", "🏅",
    "🥇", "🥈", "🥉", "⚽", "⚾", "🥎", "🏀", "🏐",
    "🏈", "🏉", "🎾", "🥏", "🎳", "🏏", "🏑", "🏒",
    "🥍", "🏓", "🏸", "🥊", "🥋", "🥅", "⛳", "⛸️",
    "🎣", "🤿", "🎽", "🎿", "🛷", "🥌", "🎯", "🪀",
    "🪁", "🎱", "🔮", "🪄", "🧿", "🪬", "🎮", "🕹️",
    "🎰", "🎲", "🧩", "🧸", "🪅", "🪩", "🪆", "♠️",
    "♥️", "♦️", "♣️", "♟️", "🃏", "🀄", "🎴", "🎭",
    "🖼️", "🎨", "🧵", "🪡", "🧶", "🪢"
)

private val instrumentEmojis = listOf(
    "🎼", "🎵", "🎶", "🎙️", "🎚️", "🎛️", "🎤", "🎧",
    "📻", "🎷", "🪗", "🎸", "🎹", "🎺", "🎻", "🪕",
    "🥁", "🪘"
)

private val heartSymbolEmojis = listOf(
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
    "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓",
    "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️",
    "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐",
    "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎",
    "♏", "♐", "♑", "♒", "♓", "🆔", "⚛️", "🉑",
    "☢️", "☣️", "📴", "📳", "🈶", "🈚", "🈸", "🈺",
    "🈷️", "✴️", "🆚", "💮", "🉐", "㊙️", "㊗️", "🈴",
    "🈵", "🈹", "🈲", "🅰️", "🅱️", "🆎", "🆑", "🅾️",
    "🆘", "❌", "⭕", "🛑", "⛔", "📛", "🚫", "💯",
    "💢", "♨️", "🚷", "🚯", "🚳", "🚱", "🔞", "📵"
)

private val arrowSymbolEmojis = listOf(
    "🔃", "🔄", "🔙", "🔚", "🔛", "🔜", "🔝", "🛐",
    "⚛️", "🕉️", "✡️", "☸️", "☯️", "✝️", "☦️", "☪️",
    "☮️", "🕎", "🔯", "♈", "♉", "♊", "♋", "♌",
    "♍", "♎", "♏", "♐", "♑", "♒", "♓", "⛎",
    "🔀", "🔁", "🔂", "▶️", "⏩", "⏭️", "⏯️", "◀️",
    "⏪", "⏮️", "🔼", "⏫", "🔽", "⏬", "⏸️", "⏹️",
    "⏺️", "⏏️", "🎦", "🔅", "🔆", "📶", "📳", "📴",
    "♀️", "♂️", "⚧️", "✖️", "➕", "➖", "➗", "🟰",
    "♾️", "‼️", "⁉️", "❓", "❔", "❕", "❗", "〰️",
    "💱", "💲", "⚕️", "♻️", "⚜️", "🔱", "📛", "🔰",
    "⭕", "✅", "☑️", "✔️", "❌", "❎", "➰", "➿",
    "〽️", "✳️", "✴️", "❇️", "©️", "®️", "™️", "#️⃣",
    "*️⃣", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣",
    "7️⃣", "8️⃣", "9️⃣", "🔟", "🔠", "🔡", "🔢", "🔣",
    "🔤", "🅰️", "🆎", "🅱️", "🆑", "🆒", "🆓", "ℹ️",
    "🆔", "Ⓜ️", "🆕", "🆖", "🅾️", "🆗", "🅿️", "🆘",
    "🆙", "🆚", "🈁", "🈂️", "🈷️", "🈶", "🈯", "🉐",
    "🈹", "🈚", "🈲", "🉑", "🈸", "🈴", "🈳", "㊗️",
    "㊙️", "🈺", "🈵"
)

private val shapeEmojis = listOf(
    "▪️", "▫️", "◾", "◽", "◼️", "◻️", "⬛", "⬜",
    "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛",
    "⬜", "◼️", "◻️", "◾", "◽", "▪️", "▫️", "🔶",
    "🔷", "🔸", "🔹", "🔺", "🔻", "💠", "🔘", "🔳",
    "🔲"
)

private val baseEmojiCategories = listOf(
    EmojiCategory(
        "표정/사람",
        faceAndEmotionEmojis + specialFaceEmojis + handGestureEmojis + bodyEmojis + peopleEmojis + professionEmojis
    ),
    EmojiCategory("동물/자연", mammalEmojis + birdEmojis + reptileAmphibianEmojis + seaEmojis + insectEmojis + plantEmojis),
    EmojiCategory(
        "음식/음료",
        fruitEmojis + vegetableEmojis + mealEmojis + asianMealEmojis + dessertEmojis + drinkEmojis + kitchenEmojis
    ),
    EmojiCategory("여행/장소", transportEmojis + placeEmojis + natureWeatherEmojis + moonStarEmojis),
    EmojiCategory("활동/스포츠", sportsEmojis),
    EmojiCategory("사물", deviceEmojis + toolEmojis + stationeryEmojis + musicEventEmojis + instrumentEmojis),
    EmojiCategory("기호", heartSymbolEmojis + arrowSymbolEmojis + shapeEmojis)
)

private val emojiCategories: List<EmojiCategory> =
    listOf(EmojiCategory("전체", baseEmojiCategories.flatMap { it.emojis })) + baseEmojiCategories
