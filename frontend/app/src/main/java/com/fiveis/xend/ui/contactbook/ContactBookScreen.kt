package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactBookScreen(
    groups: List<Group>,
    onGroupClick: (Group) -> Unit = {},
    onBottomNavChange: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("그룹별") }

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
                    IconButton(onClick = { /* 검색 */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* 추가 */ }) {
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
                TabChip("그룹별", selectedTab == "그룹별") { selectedTab = "그룹별" }
                TabChip("전체", selectedTab == "전체") { selectedTab = "전체" }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups.size) { index ->
                    GroupCard(group = groups[index], onClick = onGroupClick)
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActions()
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
        Text("빠른 작업", fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { /* 새 그룹 추가 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+ 새 그룹") }

            Button(
                onClick = { /* 연락처 가져오기 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9F5E1))
            ) { Text("연락처 가져오기", color = Color(0xFF008000)) }
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

@Preview(showBackground = true)
@Composable
fun ContactScreenPreview() {
    val sampleGroups = listOf(
        Group("VIP", "중요한 고객과 상급자들", listOf(Contact(name="김철수", email="john.c.calhoun@examplepetstore.com"), Contact(name="최철수", email="choi@snu.ac.kr")), Color(0xFFFF5C5C)),
        Group("업무 동료", "같은 회사 팀원들과 협업 파트너", listOf(Contact(name="김철수", email="john.c.calhoun@examplepetstore.com"), Contact(name="최철수", email="choi@snu.ac.kr")), Color(0xFFFFA500)),
        Group("개인 관계", "친구, 가족, 지인들과 편안한 소통", listOf(Contact(name="김철수", email="john.c.calhoun@examplepetstore.com"), Contact(name="최철수", email="choi@snu.ac.kr")), Color(0xFF00B8D9)),
        Group("학술 관계", "교수님, 연구진과의 학문적 소통", listOf(Contact(name="김철수", email="john.c.calhoun@examplepetstore.com"), Contact(name="최철수", email="choi@snu.ac.kr"), Contact(name="김철수", email="john.c.calhoun@examplepetstore.com"), Contact(name="최철수", email="choi@snu.ac.kr"), Contact(name="김철수", email="john.c.calhoun@examplepetstore.com"), Contact(name="최철수", email="choi@snu.ac.kr")), Color(0xFF8A2BE2))
    )
    ContactBookScreen(groups = sampleGroups)
}
