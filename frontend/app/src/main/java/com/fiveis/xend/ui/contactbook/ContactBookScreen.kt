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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactBookScreen(
    modifier: Modifier = Modifier,
    uiState: ContactBookUiState,
    onTabSelected: (ContactBookTab) -> Unit,
    onGroupClick: (Group) -> Unit = {},
    onContactClick: (Contact) -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf("그룹별") }

    if(selectedTab == "그룹별") {
        Scaffold(
            bottomBar = { BottomNavBar(selected = "contacts", onSelect = onBottomNavChange) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F8))
            ) {
                // 헤더
                TopAppBar(
                    title = { Text("연락처", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { /* 연락처 검색 */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* 연락처 추가 */ }) {
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
                    TabChip("그룹별", selectedTab == "그룹별") {
                        selectedTab = "그룹별"
                        onTabSelected(ContactBookTab.Groups)
                    }
                    TabChip("전체", selectedTab == "전체") {
                        selectedTab = "전체"
                        onTabSelected(ContactBookTab.Contacts)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.groups.size) { index ->
                        GroupCard(
                            group = uiState.groups[index],
                            onClick = onGroupClick
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        QuickActions()
                    }
                }
            }
        }
    }
    else {
        Scaffold(
            bottomBar = { BottomNavBar(selected = "contacts", onSelect = onBottomNavChange) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F8))
            ) {
                // 헤더
                TopAppBar(
                    title = { Text("연락처", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { /* 연락처 검색 */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* 연락처 추가 */ }) {
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
                    TabChip("그룹별", selectedTab == "그룹별") {
                        selectedTab = "그룹별"
                        onTabSelected(ContactBookTab.Groups)
                    }
                    TabChip("전체", selectedTab == "전체") {
                        selectedTab = "전체"
                        onTabSelected(ContactBookTab.Contacts)
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
                            color = uiState.contacts[index].color,
                            onClick = onContactClick
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
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
fun GroupCard(group: Group, onClick: (Group) -> Unit) {
    Surface(
        color = group.color.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, group.color),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(group) }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(group.color)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(group.name, fontWeight = FontWeight.Bold, color = group.color, fontSize = 18.sp)
                    Text(group.description, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Text("${group.members.size}명", color = group.color, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.members.take(3).forEach {
                    MemberCircle(it.name.first().toString(), group.color)
                }
                if (group.members.size > 3) {
                    MemberCircle("+${group.members.size - 3}", Color.LightGray)
                }
            }
        }
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
fun QuickActions() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { /* 새 그룹 추가 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+ 새 그룹") }
        }
    }
}

@Composable
fun BottomNavBar(selected: String, onSelect: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == "inbox",
            onClick = { onSelect("inbox") },
            icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            label = { Text("받은 메일") }
        )
        NavigationBarItem(
            selected = selected == "contacts",
            onClick = { onSelect("contacts") },
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            label = { Text("연락처") }
        )
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    subtitle: String,
    color: Color,
    onClick: (Contact) -> Unit
) {
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
        Group("1", "VIP", "중요한 고객과 상급자들", listOf(Contact(id="1", name="김철수", email="john.c.calhoun@examplepetstore.com", groupId="1"), Contact(id="2", name="최철수", email="choi@snu.ac.kr", groupId="1")), Color(0xFFFF5C5C)),
        Group("2", "업무 동료", "같은 회사 팀원들과 협업 파트너", listOf(Contact(id="1", name="김철수", email="john.c.calhoun@examplepetstore.com", groupId="2"), Contact(id="2", name="최철수", email="choi@snu.ac.kr", groupId="2")), Color(0xFFFFA500))
    )
    ContactBookScreen(
        uiState = ContactBookUiState(groups = sampleGroups),
        onTabSelected = {},
        onGroupClick = {},
        onContactClick = {}
    )
}

