package com.fiveis.xend

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 메일 데이터 모델
data class EmailItem(
    val id: String,
    val sender: String,
    val subject: String,
    val content: String,
    val timestamp: String,
    val unread: Boolean
)

// 상단 탭 라벨
enum class InboxTab(val label: String) {
    All("전체"),
    Sent("보낸 메일")
}

// 메일함 화면
@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
    onEmailClick: (EmailItem) -> Unit = {}
) {
    // 선택된 탭 상태(전체 메일로 임시 고정)
    var selectedTab by remember { mutableStateOf(InboxTab.All) }

    // 실제 앱에서는 ViewModel에서 Flow로 이메일 리스트 받아오기
    val emails by remember(selectedTab) {
        mutableStateOf(sampleEmailsFor(selectedTab))
    }

    Scaffold(
        topBar = {
            InboxTopBar(
                onSearch = onOpenSearch,
                onProfile = onOpenProfile
            )
        },
        bottomBar = {
            BottomBar(
                selected = "inbox",
                onSelect = onBottomNavChange
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                InboxTabRow(selectedTab = selectedTab, onSelect = { selectedTab = it })
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                EmailList(emails = emails, onEmailClick = onEmailClick)
                // 리스트가 바텀바/FAB와 겹치지 않도록 여백
                Spacer(Modifier.height(72.dp))
            }

            FloatingActionButton(
                onClick = onFabClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            ) {
                Icon(Icons.Filled.Create, contentDescription = "새 메일 작성")
            }
        }
    }
}

// ---------- 상단 바 ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxTopBar(
    onSearch: () -> Unit,
    onProfile: () -> Unit
) {
    // 캡처 기준: 좌측 프로필(원형 아이콘) + 우측 검색
    TopAppBar(
        title = { "Title goes here" },
        navigationIcon = {
            IconButton(onClick = onProfile) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "유저 프로필"
                )
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "메일 검색"
                )
            }
        }
    )
}

// ---------- 탭 영역 ----------

@Composable
private fun InboxTabRow(
    selectedTab: InboxTab,
    onSelect: (InboxTab) -> Unit
) {
    val tabs = InboxTab.values()
    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        divider = {}
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == tabs.indexOf(selectedTab)
            Tab(
                selected = selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ---------- 리스트 ----------

@Composable
private fun EmailList(
    emails: List<EmailItem>,
    onEmailClick: (EmailItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // 바텀 영역과 겹치지 않게
    ) {
        items(
            count = emails.size,
            key = { idx -> emails[idx].id }
        ) { idx ->
            val item = emails[idx]
            EmailRow(
                item = item,
                onClick = { onEmailClick(item) }
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }
}

// ---------- 개별 셀 ----------

@Composable
private fun EmailRow(
    item: EmailItem,
    onClick: () -> Unit
) {
    // 셀 전체 패딩/정렬
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "메일 항목: ${item.subject}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측 읽지않음 점(빨간 점) - 캡처의 작은 빨간 원
        if (item.unread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)) // 약간 톤다운된 레드
            )
        } else {
            // 읽음일 땐 공간 정렬을 위해 투명 스페이서
            Spacer(Modifier.size(8.dp))
        }

        Spacer(Modifier.width(8.dp))

        // 본문(보낸이/시간/제목/스니펫)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.sender,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = item.subject,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------- 하단 바텀 네비 ----------

@Composable
private fun BottomBar(
    selected: String,
    onSelect: (String) -> Unit
) {
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

// ---------- 샘플 데이터 ----------

private fun sampleEmailsFor(tab: InboxTab): List<EmailItem> = when (tab) {
    InboxTab.All -> listOf(
        EmailItem(
            id = "1",
            sender = "김대표 (대표이사)",
            subject = "Subject",
            content = "Preview",
            timestamp = "방금 전",
            unread = true
        ),
        EmailItem(
            id = "2",
            sender = "박팀장 (개발팀)",
            subject = "Subject",
            content = "Preview",
            timestamp = "방금 전",
            unread = true
        ),
        EmailItem(
            id = "3",
            sender = "이부장 (마케팅)",
            subject = "Subject",
            content = "Preview",
            timestamp = "방금 전",
            unread = false
        ),
        EmailItem(
            id = "4",
            sender = "최고객 (VIP 고객)",
            subject = "Subject",
            content = "Preview",
            timestamp = "방금 전",
            unread = false
        ),
        EmailItem(
            id = "5",
            sender = "정동료 (같은팀)",
            subject = "Subject",
            content = "Preview",
            timestamp = "방금 전",
            unread = false
        ),
        EmailItem(
            id = "6",
            sender = "홍보님 (인사팀)",
            subject = "Subject",
            content = "Preview",
            timestamp = "방금 전",
            unread = false
        )
    )
    InboxTab.All -> sampleEmailsFor(InboxTab.All)
    InboxTab.Sent -> emptyList()
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 800)
@Composable
private fun InboxScreenPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        InboxScreen()
    }
}
