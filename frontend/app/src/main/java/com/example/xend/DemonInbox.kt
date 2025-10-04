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

enum class InboxTab(val label: String) { All("전체"), Sent("보낸 메일") }

@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
    onEmailClick: (EmailItem) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(InboxTab.All) }
    val emails by remember(selectedTab) { mutableStateOf(sampleEmailsFor(selectedTab)) }

    Scaffold(
        topBar = { InboxTopBar(onSearch = onOpenSearch, onProfile = onOpenProfile) },
        bottomBar = { BottomBar(selected = "inbox", onSelect = onBottomNavChange) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                InboxTabRow(selectedTab = selectedTab, onSelect = { selectedTab = it })
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                EmailList(emails = emails, onEmailClick = onEmailClick)
                Spacer(Modifier.height(72.dp))
            }

            FloatingActionButton(
                onClick = onFabClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            ) { Icon(Icons.Filled.Create, contentDescription = "새 메일 작성") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxTopBar(onSearch: () -> Unit, onProfile: () -> Unit) {
    TopAppBar(
        title = { Text("받은편지함") },
        navigationIcon = {
            IconButton(onClick = onProfile) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "유저 프로필")
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "메일 검색")
            }
        }
    )
}

@Composable
private fun InboxTabRow(selectedTab: InboxTab, onSelect: (InboxTab) -> Unit) {
    val tabs = InboxTab.values()
    TabRow(selectedTabIndex = tabs.indexOf(selectedTab), divider = {}) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "메일 항목: ${item.subject}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.unread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            )
        } else {
            Spacer(Modifier.size(8.dp))
        }
        Spacer(Modifier.width(8.dp))

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

@Composable
private fun BottomBar(selected: String, onSelect: (String) -> Unit) {
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
        EmailItem("1", "김대표 (대표이사)", "Subject", "Preview", "방금 전", true),
        EmailItem("2", "박팀장 (개발팀)", "Subject", "Preview", "방금 전", true),
        EmailItem("3", "이부장 (마케팅)", "Subject", "Preview", "방금 전", false),
        EmailItem("4", "최고객 (VIP 고객)", "Subject", "Preview", "방금 전", false),
        EmailItem("5", "정동료 (같은팀)", "Subject", "Preview", "방금 전", false),
        EmailItem("6", "홍보님 (인사팀)", "Subject", "Preview", "방금 전", false)
    )
    InboxTab.Sent -> emptyList()
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 800)
@Composable
private fun InboxScreenPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        InboxScreen()
    }
}
