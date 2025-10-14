package com.fiveis.xend.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.InboxTab

@Composable
fun InboxScreen(
    uiState: InboxUiState,
    onTabSelected: (InboxTab) -> Unit,
    onEmailClick: (EmailItem) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        bottomBar = { BottomBar(selected = "inbox", onSelect = onBottomNavChange) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                FixedHeader(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected,
                    onSearch = onOpenSearch,
                    onProfile = onOpenProfile
                )
                SyncStatus()
                EmailList(emails = uiState.emails, onEmailClick = onEmailClick)
                Spacer(Modifier.height(72.dp))
            }

            FloatingActionButton(
                onClick = onFabClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(56.dp),
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Filled.Create,
                    contentDescription = "새 메일 작성",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FixedHeader(
    selectedTab: InboxTab,
    onTabSelected: (InboxTab) -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(Color.White)
    ) {
        // 상단 섹션 (44dp): 프로필 + 검색
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 아바타 (그라디언트)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF4285F4)
                            )
                        )
                    )
                    .clickable { onProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 검색 아이콘
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF5F6368),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 필터 섹션 (44dp): 전체 / 보낸메일
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab(
                    text = "전체",
                    isSelected = selectedTab == InboxTab.All,
                    onClick = { onTabSelected(InboxTab.All) }
                )
                FilterTab(
                    text = "보낸메일",
                    isSelected = selectedTab == InboxTab.Sent,
                    onClick = { onTabSelected(InboxTab.Sent) }
                )
            }
        }

        // 하단 구분선
        HorizontalDivider(
            color = Color(0xFFE8EAED),
            thickness = 1.dp
        )
    }
}

@Composable
private fun FilterTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .background(
                Color(0xFFF8F9FA),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF5F6368),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmailList(emails: List<EmailItem>, onEmailClick: (EmailItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(count = emails.size, key = { idx -> emails[idx].id }) { idx ->
            val item = emails[idx]
            EmailRow(item = item, onClick = { onEmailClick(item) })
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }
}

@Composable
private fun EmailRow(item: EmailItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.unread) Color.White else Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .semantics { contentDescription = "메일 항목: ${item.subject}" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.unread) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp, end = 8.dp)
                        .size(6.dp)
                        .background(Color(0xFFEA4335), CircleShape)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.sender,
                        color = if (item.unread) Color(0xFF202124) else Color(0xFF5F6368),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = item.timestamp,
                        color = Color(0xFF5F6368),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = item.subject,
                    color = if (item.unread) Color(0xFF202124) else Color(0xFF5F6368),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.content,
                    color = if (item.unread) Color(0xFF5F6368) else Color(0xFF80868B),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BottomBar(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(67.dp)
            .background(Color.White)
    ) {
        HorizontalDivider(
            color = Color(0xFFE8EAED),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 받은메일 탭
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "받은메일함",
                    tint = if (selected == "inbox") Color(0xFF1A73E8) else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "받은메일",
                    color = if (selected == "inbox") Color(0xFF1A73E8) else Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 연락처 탭
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
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

@Composable
private fun SyncStatus(message: String = "연락처가 추가되었습니다.") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                Color(0xFFF0F9FF),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                Color(0xFF0EA5E9),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Sync",
            tint = Color(0xFF0284C7),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = message,
            color = Color(0xFF0284C7),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
