package com.fiveis.xend.ui.view

import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.ui.theme.BackgroundWhite
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.MailDetailBodyBg
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailDetailScreen(uiState: MailDetailUiState, onBack: () -> Unit, onReply: () -> Unit = {}) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundWhite,
        topBar = {
            Column {
                MailDetailTopBar(
                    scrollBehavior = scrollBehavior,
                    onBack = onBack
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ComposeOutline
                )
            }
        },
        bottomBar = {
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
            Column(
                modifier = Modifier.padding(bottom = navBarPadding.calculateBottomPadding())
            ) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ComposeOutline
                )
                ReplyButton(onClick = onReply)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = "오류: ${uiState.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.mail != null -> {
                    MailDetailContent(
                        mail = uiState.mail
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MailDetailTopBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
                "메일 상세",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
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
                    tint = ToolbarIconTint
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeBackground,
            scrolledContainerColor = ComposeBackground
        ),
        windowInsets = WindowInsets(0, 0, 0, 0),
        scrollBehavior = scrollBehavior
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
private fun MailDetailContent(mail: com.fiveis.xend.data.model.MailDetailResponse) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(BackgroundWhite)
    ) {
        // A. 발신자 정보 섹션
        SenderInfoSection(
            senderEmail = mail.from_email,
            date = mail.date
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = ComposeOutline
        )

        // B. 메일 제목
        SubjectSection(subject = mail.subject)

        // C. 메일 본문
        BodySection(body = mail.body)

        // D. 첨부파일 섹션 (API에 데이터 없으므로 주석 처리)
        // AttachmentSection()
    }
}

@Composable
private fun SenderInfoSection(senderEmail: String, date: String) {
    // 이메일 파싱: "김대표 <kim@company.com>" 또는 "kim@company.com"
    val (senderName, email) = parseSenderEmail(senderEmail)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
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
private fun BodySection(body: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MailDetailBodyBg
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = false // 보안을 위해 JavaScript 비활성화
                        loadWithOverviewMode = true
                        useWideViewPort = false
                        setSupportZoom(false)
                    }
                }
            },
            update = { webView ->
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                margin: 16px;
                                padding: 0;
                                font-family: sans-serif;
                                font-size: 14px;
                                line-height: 1.5;
                                color: #202124;
                                background-color: transparent;
                            }
                            img {
                                max-width: 100%;
                                height: auto;
                            }
                            a {
                                color: #1A73E8;
                                text-decoration: none;
                            }
                        </style>
                    </head>
                    <body>
                        $body
                    </body>
                    </html>
                """.trimIndent()

                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 2000.dp)
        )
    }
}

@Composable
private fun ReplyButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Blue60,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "답장하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 발신자 이메일을 파싱하는 함수
 * 입력: "김대표 <kim@company.com>" 또는 "kim@company.com"
 * 출력: Pair("김대표 (kim@company.com)", "kim@company.com") 또는 Pair("kim@company.com", "kim@company.com")
 */
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

// ========================================================
// Preview
// ========================================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MailDetailScreenPreview() {
    val sampleMail = MailDetailResponse(
        id = "1",
        thread_id = "thread_1",
        subject = "Re: Q4 실적 보고서 검토 부탁드립니다",
        from_email = "김대표 (대표이사) <kim@company.com>",
        to = "recipient@example.com",
        date = "2024.12.19 오전 9:30",
        date_raw = "2024-12-19T09:30:00Z",
        body = """
            첨부된 Q4 실적 보고서를 검토해 주시고, 내일 오전 10시 정영진 회의에서 발표할 예정이니 오늘 오후 6시까지 피드백 부탁드립니다.

            주요 내용:
            • 매출 실적 분석
            • 비용 구조 개선안
            • 내년도 목표 설정

            감사합니다.
        """.trimIndent(),
        snippet = "첨부된 Q4 실적 보고서를 검토해 주시고...",
        is_unread = false,
        label_ids = listOf("INBOX")
    )

    val uiState = MailDetailUiState(
        mail = sampleMail,
        isLoading = false,
        error = null
    )

    MaterialTheme {
        MailDetailScreen(
            uiState = uiState,
            onBack = {},
            onReply = {}
        )
    }
}
