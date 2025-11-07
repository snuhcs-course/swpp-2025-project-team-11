package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookTab
import com.fiveis.xend.ui.theme.BackgroundLight
import com.fiveis.xend.ui.theme.Red60
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ContactBookScreen(
    modifier: Modifier = Modifier,
    uiState: ContactBookUiState,
    onRefresh: () -> Unit = {},
    onTabSelected: (ContactBookTab) -> Unit,
    onGroupClick: (Group) -> Unit = {},
    onContactClick: (Contact) -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
    onAddGroupClick: () -> Unit = {},
    onAddContactClick: () -> Unit = {},
    onEditGroupClick: (Group) -> Unit = {},
    onDeleteGroupClick: (Group) -> Unit = {},
    onEditContactClick: (Contact) -> Unit = {},
    onDeleteContactClick: (Contact) -> Unit = {}
) {
    val selectedTab = uiState.selectedTab

    val refreshing = uiState.isLoading
    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = onRefresh
    )

    Box(modifier.fillMaxSize()) {
        if (selectedTab == ContactBookTab.Groups) {
            Scaffold(
                bottomBar = { BottomNavBar(selected = "contacts", onSelect = onBottomNavChange) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(BackgroundLight)
                ) {
                    // 헤더
                    TopAppBar(
                        title = { Text("연락처", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { /* 연락처 검색 */ }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onAddContactClick) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    )

                    // 탭
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabChip("그룹별", selectedTab == ContactBookTab.Groups) {
                            if (selectedTab != ContactBookTab.Groups) {
                                onTabSelected(ContactBookTab.Groups)
                            }
                        }
                        TabChip("전체", selectedTab == ContactBookTab.Contacts) {
                            if (selectedTab != ContactBookTab.Contacts) {
                                onTabSelected(ContactBookTab.Contacts)
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.groups.size) { index ->
                            GroupCard(
                                group = uiState.groups[index],
                                onClick = onGroupClick,
                                onEdit = onEditGroupClick,
                                onDelete = onDeleteGroupClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            QuickActions(onAddGroupClick = onAddGroupClick)
                        }
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = { BottomNavBar(selected = "contacts", onSelect = onBottomNavChange) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(BackgroundLight)
                ) {
                    // 헤더
                    TopAppBar(
                        title = { Text("연락처", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { /* 연락처 검색 */ }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onAddContactClick) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    )

                    // 탭
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabChip("그룹별", selectedTab == ContactBookTab.Groups) {
                            if (selectedTab != ContactBookTab.Groups) {
                                onTabSelected(ContactBookTab.Groups)
                            }
                        }
                        TabChip("전체", selectedTab == ContactBookTab.Contacts) {
                            if (selectedTab != ContactBookTab.Contacts) {
                                onTabSelected(ContactBookTab.Contacts)
                            }
                        }
                    }

                    // 섹션 헤더 ("전체 연락처" / "N명")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("전체 연락처", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${uiState.contacts.size}명", color = Color.Gray)
                    }
                    Divider()

                    // 연락처 리스트
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(uiState.contacts.size) { index ->
                            ContactRow(
                                contact = uiState.contacts[index],
                                subtitle = uiState.contacts[index].email,
                                color = StableColor.forId(uiState.contacts[index].id),
                                onClick = onContactClick,
                                onEdit = onEditContactClick,
                                onDelete = onDeleteContactClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullState,
            modifier = Modifier
                .align(Alignment.TopCenter),
            contentColor = colorScheme.primary
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFDDE7FF) else Color(0xFFF0F0F0),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color(0xFF2B6DE5) else Color.Gray,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GroupCard(group: Group, onClick: (Group) -> Unit, onEdit: (Group) -> Unit = {}, onDelete: (Group) -> Unit = {}) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val groupColor = StableColor.forId(group.id)

    Surface(
        color = groupColor.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, groupColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(group) }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(groupColor)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(group.name, fontWeight = FontWeight.Bold, color = groupColor, fontSize = 18.sp)
                    Text(
                        group.description ?: "",
                        modifier = Modifier.width(240.dp),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("${group.members.size}명", color = groupColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.members.take(3).forEach {
                        MemberCircle(it.name.first().toString(), groupColor)
                    }
                    if (group.members.size > 3) {
                        MemberCircle("+${group.members.size - 3}", Color.LightGray)
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                ) {
                    // 우측 "..." 버튼
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "더보기"
                        )
                    }

                    // 오버플로우 메뉴
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            text = { Text("수정") },
                            onClick = {
                                menuExpanded = false
                                onEdit(group)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            text = { Text("삭제", color = Red60) },
                            onClick = {
                                menuExpanded = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 삭제 확인 dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("그룹 삭제") },
            text = { Text("\"${group.name}\" 그룹을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(group)
                }) { Text("삭제", color = Red60) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            },
            containerColor = BackgroundLight
        )
    }
}

@Composable
fun MemberCircle(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuickActions(onAddGroupClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onAddGroupClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+ 새 그룹") }
        }
    }
}

@Composable
fun BottomNavBar(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            modifier = Modifier,
            color = Color(0xFFE8EAED),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(67.dp)
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onSelect("mail") }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "메일함",
                    tint = if (selected == "mail") Color(0xFF1A73E8) else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "메일함",
                    color = if (selected == "mail") Color(0xFF1A73E8) else Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { /* 이미 연락처에 있음 */ }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "연락처",
                    tint = if (selected == "contacts") Color(0xFF1A73E8) else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "연락처",
                    color = if (selected == "contacts") Color(0xFF1A73E8) else Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// deprecated ContactRow
@Composable
private fun ContactRow(contact: Contact, subtitle: String, color: Color, onClick: (Contact) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(contact) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonogramAvatar(
            letter = contact.name.firstOrNull()?.toString() ?: "?",
            bg = color
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                subtitle,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center
        ) {
            Text("😎", fontSize = 14.sp)
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    subtitle: String,
    color: Color,
    onClick: (Contact) -> Unit,
    onEdit: (Contact) -> Unit = {},
    onDelete: (Contact) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(contact) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonogramAvatar(
            letter = contact.name.firstOrNull()?.toString() ?: "?",
            bg = color
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                subtitle,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
        ) {
            // 우측 "..." 버튼
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "더보기"
                )
            }

            // 오버플로우 메뉴
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    text = { Text("수정") },
                    onClick = {
                        menuExpanded = false
                        onEdit(contact)
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    text = { Text("삭제", color = Red60) },
                    onClick = {
                        menuExpanded = false
                        showDeleteConfirm = true
                    }
                )
            }
        }
    }

    // 삭제 확인 dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("연락처 삭제") },
            text = { Text("\"${contact.name}\" 님의 연락처를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(contact)
                }) { Text("삭제", color = Red60) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            },
            containerColor = BackgroundLight
        )
    }
}

@Composable
private fun MonogramAvatar(letter: String, bg: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun ContactScreenPreview() {
    val sampleGroups = listOf(
        Group(
            1,
            "VIP",
            "중요한 고객과 상급자들",
            emptyList(),
            listOf(
                Contact(0, null, name = "김철수", email = "kim@snu.ac.kr"),
                Contact(0, null, name = "최철수", email = "choi@snu.ac.kr")
            ),
            null,
            null
        ),
        Group(
            2,
            "업무 동료",
            "같은 회사 팀원들과 협업 파트너",
            emptyList(),
            listOf(
                Contact(0, null, name = "김철수", email = "kim@snu.ac.kr"),
                Contact(0, null, name = "최철수", email = "choi@snu.ac.kr")
            ),
            null,
            null
        )
    )
    ContactBookScreen(
        uiState = ContactBookUiState(groups = sampleGroups),
        onTabSelected = {},
        onGroupClick = {},
        onContactClick = {}
    )
}
