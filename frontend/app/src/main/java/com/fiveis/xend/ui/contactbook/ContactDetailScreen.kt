package com.fiveis.xend.ui.contactbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    themeColor: Color,
    uiState: ContactDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenGroup: (Long) -> Unit,
    onComposeMail: (Contact) -> Unit
) {
    val contact = uiState.contact

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                title = { Text("연락처 정보", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { contact?.let(onComposeMail) },
                        enabled = contact != null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                            Icon(Icons.Filled.Email, contentDescription = "메일 쓰기")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(innerPadding)
            .navigationBarsPadding()

        if (contact == null) {
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(uiState.error ?: "불러오는 중...", color = Color.Gray)
                    }
                }
            }
            return@Scaffold
        }

        Column(modifier = contentModifier) {
            // 헤더
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(themeColor)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (contact.email.isNotBlank()) Text(contact.email, color = Color.Gray)
                        }
                    }
                }
            }

            uiState.contact.group?.let { g ->
                GroupBriefCard(
                    group = g,
                    onClick = { onOpenGroup(g.id) }
                )
            }

            // 세부 정보
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        InfoRow(
                            contact.name + " 님께 나는",
                            contact.context?.senderRole ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                        InfoRow(
                            "나에게 " + contact.name + " 님은",
                            contact.context?.recipientRole ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                    InfoRow("개인 프롬프트", contact.context?.personalPrompt.orEmpty())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GroupBriefCard(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("소속 그룹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(group.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (group.description?.isNotBlank() == true) Text(group.description, color = Color.Gray)

            if (group.options.isNotEmpty()) {
                Divider(Modifier.padding(vertical = 4.dp))
                Text("그룹 프롬프트 정보", style = MaterialTheme.typography.titleMedium)
                val tone = group.options.filter { it.key.equals("tone", true) }
                val format = group.options.filter { it.key.equals("format", true) }
                if (tone.isNotEmpty()) {
                    Text("문체 스타일", color = Color.Gray)
                    ChipRow(tone)
                }
                if (format.isNotEmpty()) {
                    Text("형식 가이드", color = Color.Gray)
                    ChipRow(format)
                }
            }
        }
    }
}

@Composable
private fun ChipRow(list: List<PromptOption>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { TagChip(it.name.ifBlank { it.prompt }) }
    }
}

@Composable
private fun TagChip(text: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
