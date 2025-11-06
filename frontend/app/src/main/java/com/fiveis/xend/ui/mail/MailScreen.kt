package com.fiveis.xend.ui.mail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.ui.compose.Banner
import com.fiveis.xend.ui.compose.BannerType
import com.fiveis.xend.ui.inbox.InboxUiState
import com.fiveis.xend.ui.sent.SentUiState
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.Blue80

enum class MailTab {
    INBOX,
    SENT
}

@Composable
fun MailScreen(
    inboxUiState: InboxUiState,
    sentUiState: SentUiState,
    onEmailClick: (EmailItem) -> Unit,
    onAddContactClick: (EmailItem) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onInboxRefresh: () -> Unit = {},
    onInboxLoadMore: () -> Unit = {},
    onSentRefresh: () -> Unit = {},
    onSentLoadMore: () -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
    onDismissSuccessBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MailTab.INBOX) }
    var showBottomBar by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
            ) {
                BottomNavBar(selected = "mail", onSelect = onBottomNavChange)
            }
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(
                    selectedTab = selectedTab,
                    onTabChange = { selectedTab = it },
                    onSearch = onOpenSearch,
                    onProfile = onOpenProfile
                )

                // Success Banner
                AnimatedVisibility(
                    visible = inboxUiState.addContactSuccess,
                    enter = slideInVertically(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetY = { -it }
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        animationSpec = tween(durationMillis = 300),
                        targetOffsetY = { -it }
                    ) + fadeOut(animationSpec = tween(300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Banner(
                            message = "연락처가 추가되었습니다",
                            type = BannerType.INFO,
                            onDismiss = onDismissSuccessBanner,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                }

                when (selectedTab) {
                    MailTab.INBOX -> com.fiveis.xend.ui.inbox.EmailListContent(
                        emails = inboxUiState.emails,
                        onEmailClick = onEmailClick,
                        onAddContactClick = onAddContactClick,
                        onRefresh = onInboxRefresh,
                        onLoadMore = onInboxLoadMore,
                        isRefreshing = inboxUiState.isRefreshing,
                        isLoadingMore = inboxUiState.isLoading,
                        error = inboxUiState.error,
                        onScrollChange = { currentIndex, currentOffset ->
                            if (currentIndex == 0 && currentOffset == 0) {
                                showBottomBar = true
                            } else {
                                val isScrollingDown = if (currentIndex != previousIndex) {
                                    currentIndex > previousIndex
                                } else {
                                    currentOffset > previousScrollOffset
                                }
                                showBottomBar = !isScrollingDown
                            }
                            previousIndex = currentIndex
                            previousScrollOffset = currentOffset
                        },
                        contactEmails = inboxUiState.contactEmails,
                        contactsByEmail = inboxUiState.contactsByEmail
                    )
                    MailTab.SENT -> com.fiveis.xend.ui.sent.EmailListContent(
                        emails = sentUiState.emails,
                        onEmailClick = onEmailClick,
                        onRefresh = onSentRefresh,
                        onLoadMore = onSentLoadMore,
                        isRefreshing = sentUiState.isRefreshing,
                        isLoadingMore = sentUiState.isLoading,
                        error = sentUiState.error,
                        onScrollChange = { currentIndex, currentOffset ->
                            if (currentIndex == 0 && currentOffset == 0) {
                                showBottomBar = true
                            } else {
                                val isScrollingDown = if (currentIndex != previousIndex) {
                                    currentIndex > previousIndex
                                } else {
                                    currentOffset > previousScrollOffset
                                }
                                showBottomBar = !isScrollingDown
                            }
                            previousIndex = currentIndex
                            previousScrollOffset = currentOffset
                        }
                    )
                }
            }

            val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
            ) {
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier
                        .offset(y = 28.dp)
                        .padding(bottom = navBarPadding.calculateBottomPadding())
                        .size(56.dp),
                    containerColor = Blue80,
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
}

@Composable
private fun ScreenHeader(
    selectedTab: MailTab,
    onTabChange: (MailTab) -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Profile and Search row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF5F6368),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Inbox/Sent Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F3F5)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    TabButton(
                        text = "수신",
                        isSelected = selectedTab == MailTab.INBOX,
                        onClick = { onTabChange(MailTab.INBOX) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TabButton(
                        text = "발신",
                        isSelected = selectedTab == MailTab.SENT,
                        onClick = { onTabChange(MailTab.SENT) }
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier,
            color = Color(0xFFE8EAED),
            thickness = 1.dp
        )
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color.White else Color.Transparent
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Blue60 else Color(0xFF6B7280)
        )
    }
}

@Composable
private fun BottomNavBar(selected: String, onSelect: (String) -> Unit) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = navBarPadding.calculateBottomPadding())
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
                modifier = Modifier.clickable { /* 이미 메일함에 있음 */ }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "메일함",
                    tint = if (selected == "mail") Blue60 else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "메일함",
                    color = if (selected == "mail") Blue60 else Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onSelect("contacts") }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "연락처",
                    tint = if (selected == "contacts") Blue60 else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "연락처",
                    color = if (selected == "contacts") Blue60 else Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
