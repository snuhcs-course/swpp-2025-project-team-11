package com.fiveis.xend.ui.inbox

import android.util.Log
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.fiveis.xend.ui.compose.Banner
import com.fiveis.xend.ui.compose.BannerType
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.Blue80
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun InboxScreen(
    uiState: InboxUiState,
    onEmailClick: (EmailItem) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onBottomNavChange: (String) -> Unit = {},
    onAddContactClick: (EmailItem) -> Unit = {},
    onDismissSuccessBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 스크롤 방향 감지
    var showBottomBar by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            // 맨 위에 있을 때는 항상 표시
            if (currentIndex == 0 && currentOffset == 0) {
                showBottomBar = true
            } else {
                // 스크롤 방향 감지
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
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                BottomNavBar(selected = "inbox", onSelect = onBottomNavChange)
            }
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(onSearch = onOpenSearch, onProfile = onOpenProfile)

                // Success Banner
                AnimatedVisibility(
                    visible = uiState.addContactSuccess,
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

                if (uiState.isRefreshing && uiState.emails.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null && uiState.emails.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${uiState.error}")
                    }
                } else {
                    EmailList(
                        emails = uiState.emails,
                        onEmailClick = onEmailClick,
                        onAddContactClick = onAddContactClick,
                        onRefresh = onRefresh,
                        onLoadMore = onLoadMore,
                        isRefreshing = uiState.isRefreshing,
                        isLoadingMore = uiState.isLoading,
                        listState = listState,
                        contactEmails = uiState.contactEmails,
                        contactsByEmail = uiState.contactsByEmail
                    )
                }
            }

            val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier
                        .offset(y = 28.dp) // 하단바와 겹치도록 아래로 이동
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
private fun ScreenHeader(onSearch: () -> Unit, onProfile: () -> Unit) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
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
        HorizontalDivider(
            modifier = Modifier,
            color = Color(0xFFE8EAED),
            thickness = 1.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailList(
    emails: List<EmailItem>,
    onEmailClick: (EmailItem) -> Unit,
    onAddContactClick: (EmailItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contactEmails: Set<String>,
    contactsByEmail: Map<String, String> = emptyMap()
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(items = emails, key = { it.id }) { item ->
                EmailRow(
                    item = item,
                    onClick = { onEmailClick(item) },
                    onAddContactClick = onAddContactClick,
                    contactEmails = contactEmails,
                    contactsByEmail = contactsByEmail
                )
                HorizontalDivider(
                    modifier = Modifier,
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }

            // Loading indicator at the bottom
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Blue80,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Trigger load more when scrolled near bottom
        LaunchedEffect(listState) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val lastVisibleItem = visibleItems.lastOrNull()
                val totalItems = layoutInfo.totalItemsCount

                if (totalItems == 0 || lastVisibleItem == null) {
                    false
                } else {
                    // Trigger when last visible item is within 3 items from the end
                    val shouldLoad = lastVisibleItem.index >= totalItems - 4
                    if (shouldLoad) {
                        Log.d("InboxScreen", "Near bottom: lastVisible=${lastVisibleItem.index}, total=$totalItems")
                    }
                    shouldLoad
                }
            }.collect { shouldLoadMore ->
                Log.d(
                    "InboxScreen",
                    "shouldLoadMore=$shouldLoadMore, isRefreshing=$isRefreshing, isLoadingMore=$isLoadingMore"
                )
                if (shouldLoadMore && !isLoadingMore) {
                    Log.d("InboxScreen", "Triggering loadMore")
                    onLoadMore()
                } else {
                    Log.d("InboxScreen", "Not triggering loadMore - conditions not met")
                }
            }
        }
    }
}

// Extracted EmailListContent for use in MailScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailListContent(
    emails: List<EmailItem>,
    onEmailClick: (EmailItem) -> Unit,
    onAddContactClick: (EmailItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    onScrollChange: (Int, Int) -> Unit = { _, _ -> },
    contactEmails: Set<String>,
    contactsByEmail: Map<String, String> = emptyMap()
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onScrollChange(index, offset)
        }
    }

    if (isRefreshing && emails.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (error != null && emails.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Error: $error")
        }
    } else {
        EmailList(
            emails = emails,
            onEmailClick = onEmailClick,
            onAddContactClick = onAddContactClick,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            isRefreshing = isRefreshing,
            isLoadingMore = isLoadingMore,
            listState = listState,
            contactEmails = contactEmails,
            contactsByEmail = contactsByEmail
        )
    }
}

@Composable
private fun EmailRow(
    item: EmailItem,
    onClick: () -> Unit,
    onAddContactClick: (EmailItem) -> Unit,
    contactEmails: Set<String>,
    contactsByEmail: Map<String, String> = emptyMap()
) {
    // Extract email from "Name <email>" format
    val senderEmail = extractSenderEmailFromRow(item.fromEmail)
    val isContact = senderEmail.lowercase() in contactEmails

    // Use contact name if available, otherwise use original sender name
    val displayName = contactsByEmail[senderEmail.lowercase()] ?: extractSenderName(item.fromEmail)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (item.isUnread) Color.White else Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .semantics { contentDescription = "메일 항목: ${item.subject}" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isUnread) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp, end = 8.dp)
                        .size(6.dp)
                        .background(Color(0xFFEA4335), CircleShape)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            color = if (item.isUnread) Color(0xFF202124) else Color(0xFF5F6368),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Add Contact Button - only show if not already a contact
                        if (!isContact) {
                            IconButton(
                                onClick = { onAddContactClick(item) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = "연락처 추가",
                                    tint = Blue80,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
// 날짜 포매팅 해야함
                    Text(
                        text = formatDisplayDate(item.date),
                        color = Color(0xFF5F6368),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = item.subject,
                    color = if (item.isUnread) Color(0xFF202124) else Color(0xFF5F6368),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.snippet,
                    color = if (item.isUnread) Color(0xFF5F6368) else Color(0xFF80868B),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                modifier = Modifier.clickable { onSelect("inbox") }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "받은메일함",
                    tint = if (selected == "inbox") Blue60 else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "받은메일",
                    color = if (selected == "inbox") Blue60 else Color(0xFF1E293B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onSelect("sent") }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "보낸메일함",
                    tint = if (selected == "sent") Blue60 else Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "보낸메일",
                    color = if (selected == "sent") Blue60 else Color(0xFF1E293B),
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

private fun formatDisplayDate(isoDate: String): String {
    return try {
        // 1. ISO 날짜 문자열을 사용자 시간대에 맞춰 파싱
        val parsedDateTime = java.time.OffsetDateTime.parse(isoDate)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())

        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        val emailDate = parsedDateTime.toLocalDate()

        // 2. 조건에 따라 다른 형식으로 변환
        when {
            // 오늘 받은 메일이면 시간만 표시
            emailDate.isEqual(today) -> {
                java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                    .format(parsedDateTime)
            }
            // 올해 받은 메일이면 월/일만 표시
            emailDate.year == today.year -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("M월 d일", java.util.Locale.KOREAN)
                formatter.format(parsedDateTime)
            }
            // 작년 또는 그 이전 메일이면 연/월/일 표시
            else -> {
                java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT)
                    .format(parsedDateTime)
            }
        }
    } catch (e: Exception) {
        // 변환 실패 시 날짜 부분만 표시
        isoDate.substringBefore("T")
    }
}

// Helper function to extract sender name from "Name <email>" format
private fun extractSenderName(fromEmail: String): String {
    val nameRegex = "(.+?)\\s*<".toRegex()
    val matchResult = nameRegex.find(fromEmail)
    val name = matchResult?.groupValues?.get(1)?.trim() ?: fromEmail.substringBefore("<").trim().ifEmpty { fromEmail }
    // Remove surrounding quotes if present
    return name.trim('"', '\'')
}

// Helper function to extract email address from "Name <email>" format
private fun extractSenderEmailFromRow(fromEmail: String): String {
    val emailRegex = "<(.+?)>".toRegex()
    val matchResult = emailRegex.find(fromEmail)
    return matchResult?.groupValues?.get(1)?.trim() ?: fromEmail
}
