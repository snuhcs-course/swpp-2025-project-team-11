package com.fiveis.xend.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import com.fiveis.xend.ui.compose.MailComposeViewModel
import com.fiveis.xend.ui.compose.SendMailViewModel
import com.fiveis.xend.ui.compose.TemplateSelectionScreen
import com.fiveis.xend.ui.compose.common.rememberXendRichEditorState
import com.fiveis.xend.ui.mail.MailActivity
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.utils.EmailUtils
import org.json.JSONArray
import org.json.JSONObject

class ReplyDirectComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent에서 메일 정보 가져오기
        val recipientEmailRaw = intent.getStringExtra("recipient_email") ?: ""
        val recipientName = intent.getStringExtra("recipient_name") ?: ""
        val initialSubject = intent.getStringExtra("subject") ?: ""
        val groups = intent.getStringArrayListExtra("groups") ?: emptyList()
        val senderEmail = intent.getStringExtra("sender_email") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val originalBody = intent.getStringExtra("original_body") ?: ""
        val generatedBody = intent.getStringExtra("generated_body") ?: ""

        // 이메일 주소 추출 ("이름 <email@example.com>" 형식에서 이메일만 추출)
        val recipientEmail = EmailUtils.extractEmailAddress(recipientEmailRaw)

        setContent {
            XendTheme {
                // AI Compose ViewModel 초기화
                val composeVm: MailComposeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MailComposeViewModel(
                                api = MailComposeSseClient(
                                    application.applicationContext,
                                    endpointUrl = BuildConfig.BASE_URL + "/api/ai/mail/generate/stream/"
                                ),
                                wsClient = MailComposeWebSocketClient(
                                    context = application.applicationContext,
                                    wsUrl = BuildConfig.WS_URL
                                )
                            ) as T
                        }
                    }
                )
                val composeUi by composeVm.ui.collectAsState()

                // SendMailViewModel 초기화
                val sendVm: SendMailViewModel = viewModel(
                    factory = SendMailViewModel.Factory(application)
                )
                val sendUiState by sendVm.ui.collectAsState()

                // 템플릿 화면 상태
                var showTemplateScreen by remember { mutableStateOf(false) }
                var aiRealtime by rememberSaveable { mutableStateOf(true) }

                // subject와 body 상태 관리
                var currentSubject by rememberSaveable { mutableStateOf(initialSubject) }
                val editorState = rememberXendRichEditorState()

                // Enable/disable realtime mode
                LaunchedEffect(aiRealtime) {
                    composeVm.enableRealtimeMode(aiRealtime)
                }

                // Monitor text changes for realtime suggestions
                LaunchedEffect(editorState.editor) {
                    editorState.editor?.setTextChangedListener { html ->
                        if (aiRealtime) {
                            composeVm.onTextChanged(html)
                        }
                    }
                }

                // Sync AI generated content
                LaunchedEffect(composeUi.subject) {
                    if (composeUi.subject.isNotBlank()) currentSubject = composeUi.subject
                }
                LaunchedEffect(composeUi.bodyRendered) {
                    if (composeUi.bodyRendered.isNotEmpty()) {
                        editorState.setHtml(composeUi.bodyRendered)
                    }
                }

                // AI가 생성한 본문이 있으면 초기값으로 설정
                LaunchedEffect(generatedBody) {
                    if (generatedBody.isNotEmpty()) {
                        editorState.setHtml(generatedBody)
                    }
                }

                // Banner state for send results
                var bannerState by remember { mutableStateOf<com.fiveis.xend.ui.compose.BannerState?>(null) }

                LaunchedEffect(sendUiState.lastSuccessMsg) {
                    sendUiState.lastSuccessMsg?.let {
                        bannerState = com.fiveis.xend.ui.compose.BannerState(
                            message = "메일 전송에 성공했습니다.",
                            type = com.fiveis.xend.ui.compose.BannerType.SUCCESS,
                            actionText = "홈 화면 이동하기",
                            onActionClick = {
                                // Navigate to MailActivity
                                val intent = Intent(this@ReplyDirectComposeActivity, MailActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }

                LaunchedEffect(sendUiState.error) {
                    sendUiState.error?.let {
                        bannerState = com.fiveis.xend.ui.compose.BannerState(
                            message = "메일 전송에 실패했습니다. 다시 시도해주세요.",
                            type = com.fiveis.xend.ui.compose.BannerType.ERROR
                        )
                    }
                }

                if (showTemplateScreen) {
                    // 템플릿 선택 화면
                    TemplateSelectionScreen(
                        onBack = { showTemplateScreen = false },
                        onTemplateSelected = { template ->
                            currentSubject = template.subject
                            editorState.setHtml(template.body)
                            showTemplateScreen = false
                        }
                    )
                } else {
                    // 답장 작성 화면
                    ReplyDirectComposeScreen(
                        recipientEmail = recipientEmail,
                        recipientName = recipientName,
                        subject = currentSubject,
                        groups = groups,
                        onBack = { finish() },
                        onSend = { bodyText ->
                            // 이메일 전송
                            sendVm.sendEmail(
                                to = listOf(recipientEmail),
                                subject = currentSubject,
                                body = bodyText
                            )
                        },
                        onTemplateClick = { showTemplateScreen = true },
                        onSubjectChange = { currentSubject = it },
                        editorState = editorState,
                        senderEmail = senderEmail,
                        date = date,
                        originalBody = originalBody,
                        sendUiState = sendUiState,
                        // AI 관련 파라미터
                        isStreaming = composeUi.isStreaming,
                        suggestionText = composeUi.suggestionText,
                        aiRealtime = aiRealtime,
                        onUndo = {
                            composeVm.undo()?.let { snapshot ->
                                currentSubject = snapshot.subject
                                editorState.setHtml(snapshot.bodyHtml)
                            }
                        },
                        onAiComplete = {
                            // Save current state before AI generation
                            composeVm.saveUndoSnapshot(
                                subject = currentSubject,
                                bodyHtml = editorState.getHtml()
                            )

                            val payload = JSONObject().apply {
                                put("subject", currentSubject.ifBlank { "제목 생성" })
                                put("body", editorState.getHtml().ifBlank { "간단한 답장 내용" })
                                put("to_emails", JSONArray(listOf(recipientEmail)))
                                // 답장이므로 원본 메일 정보 포함
                                if (originalBody.isNotEmpty()) {
                                    put("reply_body", originalBody)
                                }
                            }
                            composeVm.startStreaming(payload)
                        },
                        onStopStreaming = { composeVm.stopStreaming() },
                        onAcceptSuggestion = {
                            // AI suggestion accepted - clear and request new one
                            composeVm.acceptSuggestion()
                            // Request new suggestion immediately
                            composeVm.requestImmediateSuggestion(editorState.getHtml())
                        },
                        onAiRealtimeToggle = { aiRealtime = it },
                        bannerState = bannerState,
                        onDismissBanner = { bannerState = null }
                    )
                }
            }
        }
    }
}
