package com.fiveis.xend.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.ui.theme.AddButtonBackground
import com.fiveis.xend.ui.theme.AttachmentExcelBg
import com.fiveis.xend.ui.theme.AttachmentHeaderText
import com.fiveis.xend.ui.theme.AttachmentImageBg
import com.fiveis.xend.ui.theme.BackgroundWhite
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.Gray200
import com.fiveis.xend.ui.theme.Gray500
import com.fiveis.xend.ui.theme.Gray600
import com.fiveis.xend.ui.theme.Green50
import com.fiveis.xend.ui.theme.Green60
import com.fiveis.xend.ui.theme.GreenSurface
import com.fiveis.xend.ui.theme.MailDetailBodyBg
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.Slate50
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint
import kotlinx.coroutines.launch

data class AttachmentFile(
    val id: String,
    val name: String,
    val size: String,
    // "excel" or "image"
    val type: String
)

data class ReplyOption(
    val id: String,
    val title: String,
    val subject: String,
    val bodyPreview: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyComposeScreen(
    senderEmail: String,
    date: String,
    subject: String,
    body: String,
    attachments: List<AttachmentFile> = emptyList(),
    replyOptions: List<ReplyOptionState> = emptyList(),
    isLoadingOptions: Boolean = false,
    isStreamingOptions: Boolean = false,
    onBack: () -> Unit = {},
    onTemplate: () -> Unit = {},
    onAttach: () -> Unit = {},
    onSend: () -> Unit = {},
    onDirectCompose: () -> Unit = {},
    onGenerateMore: () -> Unit = {},
    onUseOption: (ReplyOptionState) -> Unit = {}
) {
    android.util.Log.d(
        "ReplyComposeScreen",
        "렌더링: isLoading=$isLoadingOptions, isStreaming=$isStreamingOptions, options=${replyOptions.size}"
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundWhite,
        topBar = {
            Column {
                ReplyComposeTopBar(
                    onBack = onBack,
                    onTemplate = onTemplate,
                    onAttach = onAttach,
                    onSend = onSend
                )
                HorizontalDivider(thickness = 1.dp, color = ComposeOutline)
            }
        }
    ) { paddingValues ->
        ReplyComposeContent(
            modifier = Modifier.padding(paddingValues),
            senderEmail = senderEmail,
            date = date,
            subject = subject,
            body = body,
            attachments = attachments,
            replyOptions = replyOptions,
            isLoadingOptions = isLoadingOptions,
            isStreamingOptions = isStreamingOptions,
            onDirectCompose = onDirectCompose,
            onGenerateMore = onGenerateMore,
            onUseOption = onUseOption
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplyComposeTopBar(onBack: () -> Unit, onTemplate: () -> Unit, onAttach: () -> Unit, onSend: () -> Unit) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = statusBarPadding.calculateTopPadding(),
                start = 16.dp,
                end = 16.dp
            )
            .height(56.dp),
        title = {
            Text(
                "답장 작성",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        navigationIcon = {
            ToolbarIconButton(
                onClick = onBack,
                border = null,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Blue60
                )
            }
        },
        actions = {
            // 아이콘 제거 - 답장 옵션 추천 화면에서는 필요없음
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeBackground,
            scrolledContainerColor = ComposeBackground
        ),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    border: BorderStroke? = BorderStroke(1.dp, ComposeOutline),
    contentTint: Color = TextSecondary,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
        border = border
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentTint
            )
        ) {
            content()
        }
    }
}

@Composable
private fun ReplyComposeContent(
    modifier: Modifier = Modifier,
    senderEmail: String,
    date: String,
    subject: String,
    body: String,
    attachments: List<AttachmentFile>,
    replyOptions: List<ReplyOptionState>,
    isLoadingOptions: Boolean,
    isStreamingOptions: Boolean,
    onDirectCompose: () -> Unit,
    onGenerateMore: () -> Unit,
    onUseOption: (ReplyOptionState) -> Unit
) {
    android.util.Log.d("ReplyComposeContent", "렌더링: isLoading=$isLoadingOptions, options=${replyOptions.size}")
    val scrollState = rememberScrollState()
    var isMailContentExpanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<ReplyOptionState?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(BackgroundWhite)
    ) {
        // 발신자 정보 섹션 (접기/펼치기 가능)
        CollapsibleSenderInfoSection(
            senderEmail = senderEmail,
            date = date,
            isExpanded = isMailContentExpanded,
            onToggle = { isMailContentExpanded = !isMailContentExpanded }
        )
        HorizontalDivider(thickness = 1.dp, color = ComposeOutline)

        // 메일 제목 + 본문 (조건부 표시)
        AnimatedVisibility(visible = isMailContentExpanded) {
            Column {
                SubjectSection(subject = subject)
                CollapsibleBodySection(body = body)
            }
        }

        // 첨부파일 섹션 (조건부 렌더링)
        if (attachments.isNotEmpty()) {
            AttachmentSection(attachments = attachments)
        }

        // 답장 옵션 추천 섹션
        android.util.Log.d(
            "ReplyComposeContent",
            "ReplyOptionsSection 호출 직전: isLoading=$isLoadingOptions, options=${replyOptions.size}"
        )
        val totalBodyLength = replyOptions.sumOf { it.body.length }
        android.util.Log.d("ReplyComposeContent", "totalBodyLength=$totalBodyLength")
        key(replyOptions.size, isLoadingOptions, totalBodyLength) {
            ReplyOptionsSection(
                replyOptions = replyOptions,
                isLoading = isLoadingOptions,
                isStreaming = isStreamingOptions,
                onGenerateMore = onGenerateMore,
                onCurrentOptionChange = { selectedOption = it }
            )
        }

        // 하단 버튼들
        BottomActionButtons(
            onDirectCompose = onDirectCompose,
            onUseSelectedOption = {
                selectedOption?.let { option -> onUseOption(option) }
            },
            isUseOptionEnabled = selectedOption != null && !isStreamingOptions
        )
    }
}

@Composable
private fun CollapsibleSenderInfoSection(senderEmail: String, date: String, isExpanded: Boolean, onToggle: () -> Unit) {
    val (senderName, email) = parseSenderEmail(senderEmail)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = senderName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "접기" else "펼치기",
            tint = ToolbarIconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SubjectSection(subject: String) {
    Text(
        text = subject,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Purple60,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun CollapsibleBodySection(body: String) {
    // CollapsibleBodyPreview 사용으로 원본 메시지 파싱 지원
    CollapsibleBodyPreview(
        bodyPreview = body,
        modifier = Modifier.padding(horizontal = 20.dp),
        showHeader = false,
        backgroundColor = MailDetailBodyBg,
        borderColor = MailDetailBodyBg,
        textColor = androidx.compose.ui.graphics.Color(0xFF202124)
    )
}

@Composable
private fun AttachmentSection(attachments: List<AttachmentFile>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "첨부파일 (${attachments.size}개)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AttachmentHeaderText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        attachments.forEach { attachment ->
            AttachmentItem(attachment = attachment)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AttachmentItem(attachment: AttachmentFile) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = BackgroundWhite,
        border = BorderStroke(1.dp, ComposeOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 파일 아이콘
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (attachment.type == "excel") AttachmentExcelBg else AttachmentImageBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = attachment.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = attachment.size,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI 분석 버튼
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AddButtonBackground
                ) {
                    Text(
                        text = "AI 분석",
                        fontSize = 12.sp,
                        color = Purple60,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // 보기 버튼
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Purple60
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "보기",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyOptionsSection(
    replyOptions: List<ReplyOptionState>,
    isLoading: Boolean,
    isStreaming: Boolean,
    onGenerateMore: () -> Unit,
    onCurrentOptionChange: (ReplyOptionState?) -> Unit
) {
    // 상태 로깅
    android.util.Log.d(
        "ReplyOptionsSection",
        "isLoading=$isLoading, isStreaming=$isStreaming, options=${replyOptions.size}"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Text(
            text = "답장 옵션 추천",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // 로딩 중일 때 표시
        if (isLoading && replyOptions.isEmpty()) {
            android.util.Log.d("ReplyOptionsSection", "로딩 스피너 표시 중")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Green60
                    )
                    Text(
                        text = "답장 옵션 생성 중...",
                        fontSize = 14.sp,
                        color = Gray500
                    )
                }
            }
            return
        }

        // 옵션이 없으면 표시하지 않음
        if (replyOptions.isEmpty()) {
            return
        }

        // 페이지 인디케이터와 탭
        val pagerState = rememberPagerState(pageCount = { replyOptions.size })
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        // 페이지 인디케이터
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(replyOptions.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (pagerState.currentPage == index) Green60 else ComposeOutline,
                            shape = CircleShape
                        )
                )
                if (index < replyOptions.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        // 탭 버튼들
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(replyOptions) { option ->
                val isSelected = replyOptions.indexOf(option) == pagerState.currentPage
                OptionTab(
                    title = option.title,
                    isSelected = isSelected,
                    onClick = {
                        val index = replyOptions.indexOf(option)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }

        // 답장 내용 카드 (스와이프 가능)
        android.util.Log.d(
            "ReplyOptionsSection",
            "HorizontalPager 생성: pageCount=${replyOptions.size}, currentPage=${pagerState.currentPage}"
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            key = { page -> "${replyOptions[page].id}_${replyOptions[page].body.length}" }
        ) { page ->
            android.util.Log.d("ReplyOptionsSection", "HorizontalPager page=$page 렌더링 중")
            ReplyContentCard(
                replyOption = replyOptions[page],
                isStreaming = isStreaming,
                onGenerateMore = onGenerateMore
            )
        }

        androidx.compose.runtime.LaunchedEffect(pagerState.currentPage, replyOptions) {
            val current = replyOptions.getOrNull(pagerState.currentPage)
            onCurrentOptionChange(current)
        }
    }
}

@Composable
private fun OptionTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) GreenSurface else MailDetailBodyBg,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Green60 else Gray200
        )
    ) {
        Text(
            text = title,
            fontSize = if (isSelected) 13.sp else 11.sp,
            fontWeight = if (isSelected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            color = if (isSelected) Green50 else Gray600,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReplyContentCard(replyOption: ReplyOptionState, isStreaming: Boolean, onGenerateMore: () -> Unit) {
    android.util.Log.d(
        "ReplyContentCard",
        "id=${replyOption.id}, type=${replyOption.type}, " +
            "title=${replyOption.title}, body length=${replyOption.body.length}"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GreenSurface,
        border = BorderStroke(2.dp, Green60)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 상단: 추천 뱃지 + 새로 생성 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 추천 뱃지
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Green60
                ) {
                    Text(
                        text = "추천",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // 새로 생성 버튼
                Button(
                    onClick = onGenerateMore,
                    enabled = !isStreaming,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green60,
                        contentColor = Color.White,
                        disabledContainerColor = Gray200,
                        disabledContentColor = Gray500
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "새로 생성",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 옵션 타입 (예: 상세 보고형, 간결형)
            Text(
                text = replyOption.type,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Green50,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 본문 미리보기 섹션 (더 큰 높이 + 스크롤 가능 + 하얀색 배경)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                val bodyScrollState = rememberScrollState()
                Text(
                    text = replyOption.body,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(bodyScrollState)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomActionButtons(
    onDirectCompose: () -> Unit,
    onUseSelectedOption: (() -> Unit)?,
    isUseOptionEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 직접 작성 버튼
        OutlinedButton(
            onClick = onDirectCompose,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Slate50,
                contentColor = Gray600
            ),
            border = BorderStroke(1.dp, Gray200)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "직접 작성",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 이 옵션 사용 버튼
        Button(
            onClick = { if (isUseOptionEnabled) onUseSelectedOption?.invoke() },
            enabled = isUseOptionEnabled,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isUseOptionEnabled) Purple60 else Gray200,
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, if (isUseOptionEnabled) Purple60 else Gray200)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "이 옵션 사용",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun parseSenderEmail(senderEmail: String): Pair<String, String> {
    val regex = """^(.+?)\s*<(.+?)>$""".toRegex()
    val matchResult = regex.find(senderEmail.trim())

    return if (matchResult != null) {
        val name = matchResult.groupValues[1].trim()
        val email = matchResult.groupValues[2].trim()
        "$name ($email)" to email
    } else {
        senderEmail.trim() to senderEmail.trim()
    }
}

// Preview
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReplyComposeScreenPreview() {
    val sampleAttachments = listOf(
        AttachmentFile(
            id = "1",
            name = "Q4_실적보고서_최종.xlsx",
            size = "2.4MB",
            type = "excel"
        ),
        AttachmentFile(
            id = "2",
            name = "매출_그래프_비교분석.png",
            size = "856KB",
            type = "image"
        )
    )

    val sampleReplyOptions = listOf(
        ReplyOptionState(
            id = 1,
            type = "상세 보고형",
            title = "Re: Q4 실적 보고서 검토 완료 - 상세 분석 포함",
            body = "안녕하세요, 대표님.\n\nQ4 실적 보고서를 상세히 검토하였습니다.\n\n📊 주요 성과:\n• 마케팅 비용 12% 초과 → Q1 전략 재검토 필요",
            isComplete = true
        ),
        ReplyOptionState(
            id = 2,
            type = "간결형",
            title = "Re: Q4 실적 보고서 검토 완료",
            body = "검토 완료했습니다. 내일 회의에서 피드백 드리겠습니다.",
            isComplete = true
        ),
        ReplyOptionState(
            id = 3,
            type = "긍정형",
            title = "Re: Q4 실적 보고서 검토 완료 - 훌륭합니다!",
            body = "안녕하세요! 보고서 잘 받았습니다. 전반적으로 매우 잘 작성되었습니다.",
            isComplete = true
        ),
        ReplyOptionState(
            id = 4,
            type = "직접작성",
            title = "",
            body = "",
            isComplete = true
        )
    )

    ReplyComposeScreen(
        senderEmail = "김대표 (대표이사) <kim@company.com>",
        date = "2024.12.19 오전 9:30",
        subject = "Re: Q4 실적 보고서 검토 부탁드립니다",
        body = "첨부된 Q4 실적 보고서를 검토해 주시고, 내일 오전 10시 정영진 회의에서 " +
            "발표할 예정이니 오늘 오후 6시까지 피드백 부탁드립니다.\n\n주요 내용:\n" +
            "• 매출 실적 분석\n• 비용 구조 개선안\n• 내년도 목표 설정\n\n감사합니다.",
        attachments = sampleAttachments,
        replyOptions = sampleReplyOptions
    )
}
