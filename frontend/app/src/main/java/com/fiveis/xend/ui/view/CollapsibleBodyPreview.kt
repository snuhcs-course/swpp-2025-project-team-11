package com.fiveis.xend.ui.view

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fiveis.xend.ui.theme.BackgroundWhite
import com.fiveis.xend.ui.theme.Gray500
import com.fiveis.xend.ui.theme.Green50
import com.fiveis.xend.ui.theme.GreenBorder
import com.fiveis.xend.ui.theme.MailDetailBodyBg

/**
 * 원본 메시지를 파싱하여 구조화된 정보 추출
 */
private data class ParsedOriginalMessage(
    val from: String?,
    val to: String?,
    val sent: String?,
    val subject: String?,
    val body: String
)

private fun parseOriginalMessage(rawMessage: String): ParsedOriginalMessage {
    // 디버깅: 원본 메시지 출력
    android.util.Log.d("CollapsibleBodyPreview", "=== RAW MESSAGE ===")
    android.util.Log.d("CollapsibleBodyPreview", rawMessage)
    android.util.Log.d("CollapsibleBodyPreview", "==================")

    // 마커 제거
    var content = rawMessage
        .replace(Regex("--+\\s*original message\\s*--+", RegexOption.IGNORE_CASE), "")
        .replace(Regex("--+원본 메시지--+"), "")
        .trim()

    android.util.Log.d("CollapsibleBodyPreview", "After marker removal: $content")

    // 메타데이터 추출
    val fromMatch = Regex("From:\\s*(.+?)(?=<br>|To:|Sent:|Subject:|$)", RegexOption.IGNORE_CASE).find(content)
    val toMatch = Regex("To:\\s*(.+?)(?=<br>|Cc:|Sent:|Subject:|$)", RegexOption.IGNORE_CASE).find(content)
    val sentMatch = Regex("Sent:\\s*(.+?)(?=<br>|Subject:|$)", RegexOption.IGNORE_CASE).find(content)

    // Subject는 "Subject:" 바로 다음만 추출 (한글/영문 제목 부분만)
    val subjectMatch = Regex("Subject:\\s*([^가-힣a-zA-Z]*[가-힣a-zA-Z\\s]+)", RegexOption.IGNORE_CASE).find(content)

    android.util.Log.d("CollapsibleBodyPreview", "Subject match: ${subjectMatch?.groupValues?.get(1)?.trim()}")

    // 본문 추출: Subject 라인을 찾아서 "Subject: 제목" 부분 이후를 본문으로
    var body = ""

    // Subject: 로 시작하는 위치를 찾아서
    val subjectIndex = content.indexOf("Subject:", ignoreCase = true)
    if (subjectIndex != -1) {
        // Subject: 이후의 모든 텍스트
        val afterSubject = content.substring(subjectIndex + 8).trim() // "Subject:" = 8글자
        android.util.Log.d("CollapsibleBodyPreview", "After Subject: '$afterSubject'")

        // Subject 값(제목)을 찾아서 제거
        if (subjectMatch != null) {
            val subjectValue = subjectMatch.groupValues[1].trim()
            android.util.Log.d("CollapsibleBodyPreview", "Subject value: '$subjectValue'")
            // Subject 값 이후의 텍스트가 본문
            val bodyStartIndex = afterSubject.indexOf(subjectValue)
            if (bodyStartIndex != -1) {
                body = afterSubject.substring(bodyStartIndex + subjectValue.length)
                    .trim()
                    .removePrefix("<br>")
                    .removePrefix("<br>")
                    .trim()
                android.util.Log.d("CollapsibleBodyPreview", "Extracted body: '$body'")
            }
        }
    }

    return ParsedOriginalMessage(
        from = fromMatch?.groupValues?.get(1)?.trim(),
        to = toMatch?.groupValues?.get(1)?.trim(),
        sent = sentMatch?.groupValues?.get(1)?.trim(),
        subject = subjectMatch?.groupValues?.get(1)?.trim(),
        body = body
    )
}

/**
 * 파싱된 원본 메시지를 HTML로 변환
 */
private fun formatOriginalMessageHtml(parsed: ParsedOriginalMessage): String {
    val metadata = buildString {
        parsed.from?.let { append("<div style='margin-bottom: 4px;'><strong>From:</strong> $it</div>") }
        parsed.to?.let { append("<div style='margin-bottom: 4px;'><strong>To:</strong> $it</div>") }
        parsed.sent?.let { append("<div style='margin-bottom: 4px;'><strong>Sent:</strong> $it</div>") }
        parsed.subject?.let { append("<div style='margin-bottom: 8px;'><strong>Subject:</strong> $it</div>") }
    }

    // 일반 줄바꿈을 <br>로 변환
    val processedBody = if (!parsed.body.contains("<br>", ignoreCase = true)) {
        parsed.body.replace("\n", "<br>")
    } else {
        parsed.body
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    margin: 16px;
                    padding: 0;
                    font-family: sans-serif;
                    font-size: 12px;
                    line-height: 1.5;
                    color: #6B7280;
                    background-color: transparent;
                }
                strong {
                    color: #4B5563;
                    font-weight: 600;
                }
                .metadata {
                    padding-bottom: 8px;
                    margin-bottom: 8px;
                    border-bottom: 1px solid #E5E7EB;
                }
                .body-content {
                    color: #6B7280;
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
            <div class="metadata">
                $metadata
            </div>
            <div class="body-content">
                $processedBody
            </div>
        </body>
        </html>
    """.trimIndent()
}

/**
 * HTML을 WebView로 렌더링하기 위한 헬퍼 함수
 */
private fun createHtmlContent(htmlBody: String, fontSize: Int = 14, textColor: String = "#202124"): String {
    // 일반 줄바꿈을 <br>로 변환 (이미 <br>이 있으면 유지)
    val processedBody = if (!htmlBody.contains("<br>", ignoreCase = true)) {
        htmlBody.replace("\n", "<br>")
    } else {
        htmlBody
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    margin: 16px;
                    padding: 0;
                    font-family: sans-serif;
                    font-size: ${fontSize}px;
                    line-height: 1.5;
                    color: $textColor;
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
            $processedBody
        </body>
        </html>
    """.trimIndent()
}

/**
 * 원본 메시지 접기 기능이 있는 본문 미리보기 컴포저블
 *
 * 원본 메시지 마커를 자동으로 감지하고, 답장 본문과 원본 메시지를 분리하여 표시합니다.
 * 원본 메시지는 기본적으로 접혀있고, 클릭하면 펼칠 수 있습니다.
 */
@Composable
fun CollapsibleBodyPreview(
    bodyPreview: String,
    modifier: Modifier = Modifier,
    headerText: String = "본문 미리보기",
    showHeader: Boolean = true,
    backgroundColor: Color = BackgroundWhite,
    borderColor: Color = GreenBorder,
    textColor: Color = Green50
) {
    // 원본 메시지 마커 찾기
    val markers = listOf(
        "-- original message --",
        "--original message--",
        "-----Original Message-----",
        "-----원본 메시지-----",
        "<br><br>From:",
        "<br><br>from:"
    )

    var splitIndex = -1
    for (marker in markers) {
        val index = bodyPreview.indexOf(marker, ignoreCase = true)
        if (index != -1) {
            splitIndex = index
            break
        }
    }

    // 원본 메시지가 없으면 그냥 전체 표시 (WebView 사용)
    if (splitIndex == -1) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (showHeader) {
                Text(
                    text = headerText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = false
                                loadWithOverviewMode = true
                                useWideViewPort = false
                                setSupportZoom(false)
                            }
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            createHtmlContent(bodyPreview),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 800.dp)
                )
            }
        }
        return
    }

    // 답장 본문과 원본 메시지 분리
    val replyBody = bodyPreview.substring(0, splitIndex).trim()
    val originalMessage = bodyPreview.substring(splitIndex).trim()

    var isOriginalExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (showHeader) {
            Text(
                text = headerText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray500,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 답장 본문 (WebView)
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = false
                                loadWithOverviewMode = true
                                useWideViewPort = false
                                setSupportZoom(false)
                            }
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            createHtmlContent(replyBody),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 600.dp)
                )

                // 원본 메시지 접기/펼치기
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isOriginalExpanded = !isOriginalExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "원본 메시지",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray500
                    )
                    Icon(
                        imageVector = if (isOriginalExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isOriginalExpanded) "접기" else "펼치기",
                        tint = Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 원본 메시지 내용 (접기 가능, WebView)
                AnimatedVisibility(visible = isOriginalExpanded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MailDetailBodyBg
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = false
                                        setSupportZoom(false)
                                    }
                                }
                            },
                            update = { webView ->
                                // 원본 메시지를 파싱하여 구조화된 형태로 표시
                                val parsed = parseOriginalMessage(originalMessage)
                                webView.loadDataWithBaseURL(
                                    null,
                                    formatOriginalMessageHtml(parsed),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 400.dp)
                        )
                    }
                }
            }
        }
    }
}
