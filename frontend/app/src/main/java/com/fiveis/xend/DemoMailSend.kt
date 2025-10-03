package com.fiveis.xend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Contact(
    val name: String,
    val email: String,
    val color: Color = Color(0xFF5A7DFF) // 아바타 배경색 (임시)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailComposeScreen(
    modifier: Modifier = Modifier,
    contactsInitial: List<Contact> = listOf(
        Contact("김대표", "kimjjang@naver.com")
    ),
    onBack: () -> Unit = {},
    onSend: () -> Unit = {},
    onUndo: () -> Unit = {},
    onAiComplete: () -> Unit = {}
) {
    // -------- UI 상태 (샘플) --------
    var contacts by rememberSaveable {
        mutableStateOf(contactsInitial)
    }
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable {
        mutableStateOf(
            "안녕하세요, 대표님.\n\nQ4 실적 보고서를 검토했습니다.\n\n전반적으로 매출 증가율이 목표치를 상회하는 우수한 성과라고 판단됩니다."
        )
    }
    var aiRealtime by rememberSaveable { mutableStateOf(true) }

    // 새 수신자 입력
    var newContact by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("메일 작성", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 그리드 아이콘(의미 없는 데코) */ }) {
                        Icon(Icons.Default.GridView, contentDescription = "템플릿")
                    }
                    IconButton(onClick = { /* 첨부 */ }) {
                        Icon(Icons.Default.Attachment, contentDescription = "첨부")
                    }
                    IconButton(onClick = onSend) {
                        Icon(Icons.Default.Send, contentDescription = "보내기")
                    }
                }
            )
        },
        modifier = modifier
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // 상단 액션 라인: 실행취소 / AI완성
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    shape = RoundedCornerShape(20.dp)
                ) { Text("실행취소") }

                FilledTonalButton(
                    onClick = onAiComplete,
                    shape = RoundedCornerShape(20.dp)
                ) { Text("AI 완성") }
            }

            // 섹션: 받는 사람
            SectionHeader(text = "받는 사람")
            Column(Modifier.padding(horizontal = 16.dp)) {
                // 선택된 수신자 칩
                LazyRow(
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(contacts, key = { it.email }) { r ->
                        ContactChip(
                            contact = r,
                            onRemove = {
                                contacts = contacts.filterNot { it.email == r.email }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    item {
                        AssistChip(
                            onClick = {
                                // 입력된 문자열이 이메일 형식인지 등 검증은 필요 시 확장
                                val t = newContact.text.trim()
                                if (t.isNotEmpty()) {
                                    contacts = contacts + Contact(t, t)
                                    newContact = TextFieldValue("")
                                }
                            },
                            label = { Text("+") }
                        )
                    }
                }

                // 새 수신자 입력
                OutlinedTextField(
                    value = newContact,
                    onValueChange = { newContact = it },
                    placeholder = { Text("이메일 입력 후 Enter") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            // 섹션: 제목
            SectionHeader(text = "제목")
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("제목") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 섹션: 본문 + 실시간 AI 토글
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("본문", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("실시간 AI", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    Switch(checked = aiRealtime, onCheckedChange = { aiRealtime = it })
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("내용을 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 화면 남는 공간을 본문이 차지
                    .padding(horizontal = 16.dp),
                minLines = 10
            )

            // 하단 고정: 탭 완료(=임시 저장/확정 느낌)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(onClick = { /* 탭 완료 액션 */ }) {
                    Text("탭 완료")
                }
            }
        }
    }
}

/**
 * 섹션 타이틀을 통일감 있게 표시
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * 수신자 칩 – 아바타(이니셜) + 이름(이메일) + X 버튼
 * Coil 로 프로필 이미지를 불러오는 확장 가능 지점.
 */
@Composable
fun ContactChip(
    contact: Contact,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아바타: 이니셜
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(contact.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${contact.name} (${contact.email})",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "×",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onRemove() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailComposePreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        EmailComposeScreen()
    }
}
