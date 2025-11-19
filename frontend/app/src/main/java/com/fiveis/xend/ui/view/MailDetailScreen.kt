package com.fiveis.xend.ui.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.fiveis.xend.data.model.Attachment
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.ui.theme.AttachmentExcelBg
import com.fiveis.xend.ui.theme.AttachmentHeaderText
import com.fiveis.xend.ui.theme.AttachmentImageBg
import com.fiveis.xend.ui.theme.BackgroundWhite
import com.fiveis.xend.ui.theme.Blue40
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.MailDetailBodyBg
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint
import com.fiveis.xend.utils.formatFileSize
import com.fiveis.xend.utils.shortenFilename
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailDetailScreen(
    uiState: MailDetailUiState,
    knownContactsByEmail: Map<String, Contact> = emptyMap(),
    onBack: () -> Unit,
    onReply: () -> Unit = {},
    onDownloadAttachment: (Attachment) -> Unit = {},
    onAnalyzeAttachment: (Attachment) -> Unit = {},
    onDismissAnalysis: () -> Unit = {},
    onClearDownloadResult: () -> Unit = {},
    onPreviewAttachment: (Attachment) -> Unit = {},
    onDismissPreview: () -> Unit = {},
    onOpenAttachmentExternally: (Attachment) -> Unit = {},
    onConsumeExternalOpen: () -> Unit = {},
    onClearExternalOpenError: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var attachmentToDownload by remember { mutableStateOf<Attachment?>(null) }

    LaunchedEffect(uiState.downloadSuccessMessage) {
        uiState.downloadSuccessMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onClearDownloadResult()
        }
    }

    LaunchedEffect(uiState.downloadErrorMessage) {
        uiState.downloadErrorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onClearDownloadResult()
        }
    }

    LaunchedEffect(uiState.externalOpenErrorMessage) {
        uiState.externalOpenErrorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onClearExternalOpenError()
        }
    }

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
                        mail = uiState.mail,
                        knownContactsByEmail = knownContactsByEmail,
                        onAttachmentClick = { selected -> attachmentToDownload = selected },
                        onAnalyzeAttachment = onAnalyzeAttachment,
                        onPreviewAttachment = onPreviewAttachment,
                        onOpenAttachmentExternally = onOpenAttachmentExternally
                    )
                }
            }
        }
    }

    attachmentToDownload?.let { attachment ->
        AttachmentDownloadDialog(
            attachment = attachment,
            onDismiss = { attachmentToDownload = null },
            onConfirm = {
                onDownloadAttachment(attachment)
                attachmentToDownload = null
            }
        )
    }

    if (uiState.isDownloadingAttachment) {
        DownloadingDialog()
    }

    if (uiState.isExternalOpenLoading) {
        ExternalOpenLoadingDialog()
    }

    if (uiState.showAnalysisPopup && uiState.analysisTarget != null) {
        AttachmentAnalysisPopup(
            attachment = uiState.analysisTarget,
            isLoading = uiState.isAnalyzingAttachment,
            result = uiState.analysisResult,
            errorMessage = uiState.analysisErrorMessage,
            onDismiss = onDismissAnalysis,
            onCopyGuide = { text ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "답장 가이드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (uiState.showPreviewDialog && uiState.previewTarget != null) {
        AttachmentPreviewDialog(
            attachment = uiState.previewTarget,
            isLoading = uiState.isPreviewLoading,
            content = uiState.previewContent,
            errorMessage = uiState.previewErrorMessage,
            onDismiss = onDismissPreview,
            onRetry = { onPreviewAttachment(uiState.previewTarget) }
        )
    }

    LaunchedEffect(uiState.externalOpenContent) {
        val content = uiState.externalOpenContent ?: return@LaunchedEffect
        val file = File(content.filePath)
        val mime = content.mimeType.ifBlank { "*/*" }
        if (!file.exists()) {
            Toast.makeText(context, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            onConsumeExternalOpen()
            return@LaunchedEffect
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "앱 선택").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        } finally {
            onConsumeExternalOpen()
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
private fun MailDetailContent(
    mail: EmailItem,
    knownContactsByEmail: Map<String, Contact>,
    onAttachmentClick: (Attachment) -> Unit,
    onAnalyzeAttachment: (Attachment) -> Unit,
    onPreviewAttachment: (Attachment) -> Unit,
    onOpenAttachmentExternally: (Attachment) -> Unit
) {
    val scrollState = rememberScrollState()

    val isSentMail = mail.labelIds.any { label ->
        label.contains("SENT", ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(BackgroundWhite)
    ) {
        // A. 발신자 정보 섹션
        SenderInfoSection(
            senderEmail = mail.fromEmail,
            recipientEmail = mail.toEmail,
            isSentMail = isSentMail,
            date = mail.date,
            knownContactsByEmail = knownContactsByEmail
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = ComposeOutline
        )

        // B. 메일 제목
        SubjectSection(subject = mail.subject)

        // C. 메일 본문
        BodySection(body = mail.body)

        // D. 첨부파일 섹션
        if (mail.attachments.isNotEmpty()) {
            AttachmentSection(
                attachments = mail.attachments,
                onAttachmentClick = onAttachmentClick,
                onAnalyzeAttachment = onAnalyzeAttachment,
                onPreviewAttachment = onPreviewAttachment,
                onOpenAttachmentExternally = onOpenAttachmentExternally
            )
        }
    }
}

@Composable
private fun SenderInfoSection(
    senderEmail: String,
    recipientEmail: String,
    isSentMail: Boolean,
    date: String,
    knownContactsByEmail: Map<String, Contact>
) {
    val (displayName, displayEmail) = if (isSentMail) {
        parseSenderEmail(recipientEmail)
    } else {
        parseSenderEmail(senderEmail)
    }
    val normalized = displayEmail.trim().lowercase()
    val savedContact = knownContactsByEmail[normalized]
    val resolvedDisplayName = savedContact?.name?.takeIf { it.isNotBlank() } ?: displayName

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (isSentMail) "To. $resolvedDisplayName" else resolvedDisplayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "<$displayEmail>",
            fontSize = 13.sp,
            color = TextSecondary
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
    // 디버깅: 메일 본문 확인
    android.util.Log.d("MailDetailScreen", "=== BODY ===")
    android.util.Log.d("MailDetailScreen", "Body length: ${body.length}")
    android.util.Log.d("MailDetailScreen", body.take(200)) // 처음 200자만
    android.util.Log.d("MailDetailScreen", "============")

    // body가 비어있으면 안내 메시지 표시
    if (body.isBlank()) {
        Text(
            text = "메일 본문이 없습니다.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = TextSecondary
        )
        return
    }

    // 원본 메시지 분리
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
        val index = body.indexOf(marker, ignoreCase = true)
        if (index != -1) {
            splitIndex = index
            break
        }
    }

    if (splitIndex == -1) {
        // 원본 메시지가 없으면 기존 방식대로 표시
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
                            javaScriptEnabled = false
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
    } else {
        // 원본 메시지가 있으면 CollapsibleBodyPreview 사용
        val mainBody = body.substring(0, splitIndex).trim()
        CollapsibleBodyPreview(
            bodyPreview = body,
            modifier = Modifier.padding(horizontal = 20.dp),
            showHeader = false,
            backgroundColor = MailDetailBodyBg,
            borderColor = MailDetailBodyBg,
            textColor = androidx.compose.ui.graphics.Color(0xFF202124)
        )
    }
}

@Composable
private fun AttachmentSection(
    attachments: List<Attachment>,
    onAttachmentClick: (Attachment) -> Unit,
    onAnalyzeAttachment: (Attachment) -> Unit,
    onPreviewAttachment: (Attachment) -> Unit,
    onOpenAttachmentExternally: (Attachment) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "첨부파일 (${attachments.size}개)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AttachmentHeaderText
            )
            attachments.forEach { attachment ->
                val previewType = attachment.previewType()
                val supportsPreview = previewType != AttachmentPreviewType.UNSUPPORTED
                AttachmentItem(
                    attachment = attachment,
                    onClick = { onAttachmentClick(attachment) },
                    onAnalyze = { onAnalyzeAttachment(attachment) },
                    onPreview = { onPreviewAttachment(attachment) },
                    onOpenExternal = { onOpenAttachmentExternally(attachment) },
                    supportsPreview = supportsPreview
                )
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    attachment: Attachment,
    onClick: () -> Unit,
    onAnalyze: () -> Unit,
    onPreview: () -> Unit,
    onOpenExternal: () -> Unit,
    supportsPreview: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = BackgroundWhite,
        border = BorderStroke(1.dp, ComposeOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentFileIcon(filename = attachment.filename)
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = shortenFilename(attachment.filename, 25),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(attachment.size),
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiAnalysisBadge(onClick = onAnalyze)
                PreviewButton(
                    onClick = {
                        if (supportsPreview) {
                            onPreview()
                        } else {
                            onOpenExternal()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AttachmentFileIcon(filename: String) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(6.dp),
        color = attachmentBadgeColor(filename)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.InsertDriveFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AiAnalysisBadge(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEFF6FF),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = "AI 분석",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Purple60,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PreviewButton(onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Purple60
    ) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "미리보기",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "보기",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AttachmentDownloadDialog(attachment: Attachment, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "첨부파일 다운로드") },
        text = {
            Text(
                text = "'${attachment.filename}' 파일을 저장할까요?",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "파일 저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}

@Composable
private fun DownloadingDialog() {
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BackgroundWhite,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(
                    text = "파일 저장 중입니다...",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ExternalOpenLoadingDialog() {
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BackgroundWhite,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(
                    text = "파일을 준비하는 중입니다...",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun AttachmentAnalysisPopup(
    attachment: Attachment,
    isLoading: Boolean,
    result: AttachmentAnalysisResponse?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCopyGuide: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(min = 400.dp, max = 640.dp),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 12.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AnalysisHeader(attachment = attachment, onDismiss = onDismiss)
                    AnalysisContent(
                        isLoading = isLoading,
                        result = result,
                        errorMessage = errorMessage,
                        onCopyGuide = onCopyGuide
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisHeader(attachment: Attachment, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttachmentFileIcon(filename = attachment.filename)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = attachment.filename,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "파일 분석 결과",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFFF1F5F9),
            modifier = Modifier.clickable(onClick = onDismiss)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = TextSecondary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun AnalysisContent(
    isLoading: Boolean,
    result: AttachmentAnalysisResponse?,
    errorMessage: String?,
    onCopyGuide: (String) -> Unit
) {
    when {
        isLoading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "AI가 파일을 분석 중입니다...",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
        errorMessage != null -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "잠시 후 다시 시도해주세요.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        result == null -> {
            Text(
                text = "분석 결과를 불러올 수 없습니다.",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AnalysisSection(
                    title = "주요 내용 요약",
                    backgroundColor = Color(0xFFF8FAFC),
                    borderColor = Color(0xFFE2E8F0),
                    contentLines = result.summary.lines().map { it.trim() }.filter { it.isNotEmpty() }
                )
                AnalysisSection(
                    title = "핵심 시사점",
                    backgroundColor = Color(0xFFFFF7ED),
                    borderColor = Color(0xFFFED7AA),
                    contentLines = result.insights.lines().map { it.trim() }.filter { it.isNotEmpty() }
                )
                AnalysisSection(
                    title = "답장 작성 가이드",
                    backgroundColor = Color(0xFFFAF5FF),
                    borderColor = Color(0xFFDDD6FE),
                    contentLines = result.mailGuide.lines().map { it.trim() }.filter { it.isNotEmpty() }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val guideText = result.mailGuide
                    val copyEnabled = guideText.isNotBlank()
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable(
                            enabled = copyEnabled,
                            onClick = { if (copyEnabled) onCopyGuide(guideText) }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "답장 가이드 복사",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "답장 가이드 복사",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisSection(title: String, backgroundColor: Color, borderColor: Color, contentLines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (contentLines.isEmpty()) {
                    Text(
                        text = "표시할 내용이 없습니다.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                } else {
                    contentLines.forEach { line ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 9.dp)
                                    .size(6.dp)
                                    .background(color = Purple60, shape = CircleShape)
                            )
                            Text(
                                text = line,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewDialog(
    attachment: Attachment,
    isLoading: Boolean,
    content: AttachmentPreviewContent?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .heightIn(min = 420.dp, max = 780.dp),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 12.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AttachmentPreviewHeader(
                        attachment = attachment,
                        onDismiss = onDismiss
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "파일을 불러오는 중입니다...",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            errorMessage != null -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = errorMessage,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(onClick = onRetry) {
                                        Text("다시 시도")
                                    }
                                }
                            }
                            content is AttachmentPreviewContent.Text -> {
                                TextPreviewContent(
                                    text = content.text,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            content is AttachmentPreviewContent.Pdf -> {
                                PdfPreviewContent(
                                    filePath = content.filePath,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Text(
                                    text = "미리볼 수 있는 내용이 없습니다.",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewHeader(attachment: Attachment, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AttachmentFileIcon(filename = attachment.filename)
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = attachment.filename,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "파일 크기 ${formatFileSize(attachment.size)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun TextPreviewContent(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MailDetailBodyBg, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun PdfPreviewContent(filePath: String, modifier: Modifier = Modifier) {
    val holder = rememberPdfRenderer(filePath)
    when {
        holder == null -> {
            Box(
                modifier = modifier
                    .background(MailDetailBodyBg, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PDF 파일을 열 수 없습니다.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        holder.renderer.pageCount == 0 -> {
            Box(
                modifier = modifier
                    .background(MailDetailBodyBg, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "표시할 페이지가 없습니다.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .background(MailDetailBodyBg, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(holder.renderer.pageCount) { pageIndex ->
                    PdfPagePreview(
                        renderer = holder.renderer,
                        pageIndex = pageIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPagePreview(renderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember(pageIndex, renderer) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, pageIndex) {
        bitmap?.recycle()
        bitmap = null
        val renderedBitmap = withContext(Dispatchers.IO) {
            renderer.openPage(pageIndex).use { page ->
                val rendered = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                rendered
            }
        }
        bitmap = renderedBitmap
    }

    DisposableEffect(renderer, pageIndex) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, ComposeOutline), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "페이지 ${pageIndex + 1}",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            contentAlignment = Alignment.Center
        ) {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun rememberPdfRenderer(filePath: String): PdfRendererHolder? {
    val holder = remember(filePath) {
        runCatching {
            val file = File(filePath)
            if (!file.exists()) {
                null
            } else {
                val descriptor = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                PdfRendererHolder(descriptor, PdfRenderer(descriptor))
            }
        }.getOrNull()
    }
    DisposableEffect(holder) {
        onDispose {
            holder?.renderer?.close()
            holder?.descriptor?.close()
        }
    }
    return holder
}

private data class PdfRendererHolder(
    val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
)

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

private fun attachmentBadgeColor(filename: String): Color {
    val extension = filename.substringAfterLast('.', "").lowercase(Locale.getDefault())
    return when (extension) {
        "xls", "xlsx", "csv" -> AttachmentExcelBg
        "png", "jpg", "jpeg", "gif", "bmp", "webp" -> AttachmentImageBg
        else -> Blue40
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
    val sampleMail = EmailItem(
        id = "1",
        threadId = "thread_1",
        subject = "Re: Q4 실적 보고서 검토 부탁드립니다",
        fromEmail = "김대표 (대표이사) <kim@company.com>",
        snippet = "첨부된 Q4 실적 보고서를 검토해 주시고...",
        date = "2024.12.19 오전 9:30",
        dateRaw = "2024-12-19T09:30:00Z",
        isUnread = false,
        labelIds = listOf("INBOX"),
        body = """
            첨부된 Q4 실적 보고서를 검토해 주시고, 내일 오전 10시 정영진 회의에서 발표할 예정이니 오늘 오후 6시까지 피드백 부탁드립니다.

            주요 내용:
            • 매출 실적 분석
            • 비용 구조 개선안
            • 내년도 목표 설정

            감사합니다.
        """.trimIndent(),
        attachments = listOf(
            Attachment(
                attachmentId = "att1",
                filename = "Q4_실적보고서_최종.xlsx",
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                size = 2_400_000
            ),
            Attachment(
                attachmentId = "att2",
                filename = "매출_그래프_비교분석.png",
                mimeType = "image/png",
                size = 856_000
            )
        )
    )

    val uiState = MailDetailUiState(
        mail = sampleMail,
        isLoading = false,
        error = null
    )

    MaterialTheme {
        MailDetailScreen(
            uiState = uiState,
            knownContactsByEmail = emptyMap(),
            onBack = {},
            onReply = {},
            onDownloadAttachment = {},
            onAnalyzeAttachment = {},
            onDismissAnalysis = {},
            onClearDownloadResult = {},
            onPreviewAttachment = {},
            onDismissPreview = {}
        )
    }
}
