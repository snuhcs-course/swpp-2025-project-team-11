package com.fiveis.xend.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.ui.contactbook.RoleInputMode
import com.fiveis.xend.ui.contactbook.recipientRoleOptionExamples
import com.fiveis.xend.ui.contactbook.senderRoleOptionExamples
import com.fiveis.xend.ui.profile.LanguageDialog
import com.fiveis.xend.ui.profile.languageDisplayText
import com.fiveis.xend.ui.theme.BackgroundLight
import com.fiveis.xend.ui.theme.BorderGray
import com.fiveis.xend.ui.theme.Gray200
import com.fiveis.xend.ui.theme.Gray400
import com.fiveis.xend.ui.theme.Gray600
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.Slate900
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.ToolbarIconTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactDialog(
    senderName: String,
    senderEmail: String,
    groups: List<Group> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        email: String,
        senderRole: String?,
        recipientRole: String,
        personalPrompt: String?,
        groupId: Long?,
        languagePreference: String?
    ) -> Unit
) {
    val directInputLabel = "직접 입력"

    var name by rememberSaveable { mutableStateOf(senderName) }
    var email by rememberSaveable { mutableStateOf(senderEmail) }

    val senderRoleOptions = senderRoleOptionExamples + directInputLabel
    val recipientRoleOptions = recipientRoleOptionExamples + directInputLabel

    var senderMode by rememberSaveable { mutableStateOf(RoleInputMode.PRESET) }
    var senderPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var senderManual by rememberSaveable { mutableStateOf("") }
    var isSenderExpanded by remember { mutableStateOf(false) }

    var recipientMode by rememberSaveable { mutableStateOf(RoleInputMode.PRESET) }
    var recipientPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var recipientManual by rememberSaveable { mutableStateOf("") }
    var isRecipientExpanded by remember { mutableStateOf(false) }

    var personalPrompt by rememberSaveable { mutableStateOf("") }
    var isGroupExpanded by remember { mutableStateOf(false) }
    var selectedGroup by rememberSaveable { mutableStateOf<Group?>(null) }
    var languagePreference by rememberSaveable { mutableStateOf("") }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    val savable = name.isNotBlank() && email.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "연락처 추가",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = Gray600
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Content - Scrollable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 이름 + 이메일
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = senderName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = senderEmail,
                            fontSize = 13.sp,
                            color = Gray600
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // 이름
                    Text(
                        "이름",
                        color = Gray600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = {
                            Text(
                                text = senderName,
                                style = LocalTextStyle.current.copy(fontSize = 12.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // 이메일
                    Text(
                        "이메일",
                        color = Gray600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = {
                            Text(
                                text = senderEmail,
                                style = LocalTextStyle.current.copy(fontSize = 12.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "관계 - 나",
                                color = Gray600,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            // Sender Role
                            ExposedDropdownMenuBox(
                                expanded = isSenderExpanded,
                                onExpandedChange = { isSenderExpanded = !isSenderExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = when (senderMode) {
                                        RoleInputMode.PRESET -> senderPreset ?: "나"
                                        RoleInputMode.MANUAL -> directInputLabel
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Group,
                                            contentDescription = null,
                                            tint = ToolbarIconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isSenderExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .heightIn(min = 48.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = BackgroundLight,
                                        unfocusedContainerColor = BackgroundLight,
                                        unfocusedBorderColor = Gray200,
                                        focusedBorderColor = Purple60,
                                        focusedTextColor = Slate900,
                                        unfocusedTextColor = Gray600
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isSenderExpanded,
                                    onDismissRequest = { isSenderExpanded = false }
                                ) {
                                    senderRoleOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 13.sp) },
                                            onClick = {
                                                isSenderExpanded = false
                                                if (option == directInputLabel) {
                                                    senderMode = RoleInputMode.MANUAL
                                                } else {
                                                    senderMode = RoleInputMode.PRESET
                                                    senderPreset = option
                                                    senderManual = ""
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "관계 - 상대방",
                                color = Gray600,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            // Recipient Role
                            ExposedDropdownMenuBox(
                                expanded = isRecipientExpanded,
                                onExpandedChange = { isRecipientExpanded = !isRecipientExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = when (recipientMode) {
                                        RoleInputMode.PRESET -> recipientPreset ?: "상대방"
                                        RoleInputMode.MANUAL -> directInputLabel
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Group,
                                            contentDescription = null,
                                            tint = ToolbarIconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isRecipientExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .heightIn(min = 48.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = BackgroundLight,
                                        unfocusedContainerColor = BackgroundLight,
                                        unfocusedBorderColor = Gray200,
                                        focusedBorderColor = Purple60,
                                        focusedTextColor = Slate900,
                                        unfocusedTextColor = Gray600
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isRecipientExpanded,
                                    onDismissRequest = { isRecipientExpanded = false }
                                ) {
                                    recipientRoleOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 13.sp) },
                                            onClick = {
                                                isRecipientExpanded = false
                                                if (option == directInputLabel) {
                                                    recipientMode = RoleInputMode.MANUAL
                                                } else {
                                                    recipientMode = RoleInputMode.PRESET
                                                    recipientPreset = option
                                                    recipientManual = ""
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Manual input fields
                    if (senderMode == RoleInputMode.MANUAL || recipientMode == RoleInputMode.MANUAL) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            if (senderMode == RoleInputMode.MANUAL) {
                                OutlinedTextField(
                                    value = senderManual,
                                    onValueChange = { senderManual = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            text = "나의 역할",
                                            style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                            color = Gray400
                                        )
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderGray,
                                        focusedBorderColor = Purple60
                                    )
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            Spacer(Modifier.width(8.dp))

                            if (recipientMode == RoleInputMode.MANUAL) {
                                OutlinedTextField(
                                    value = recipientManual,
                                    onValueChange = { recipientManual = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            text = "상대방 역할",
                                            style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                            color = Gray400
                                        )
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderGray,
                                        focusedBorderColor = Purple60
                                    )
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 관계 프롬프팅
                    Text(
                        "관계 프롬프팅(선택사항)",
                        color = Gray600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = personalPrompt,
                        onValueChange = { personalPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        placeholder = {
                            Text(
                                text = "상대방과의 관계를 설명해 주세요",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                color = Gray400
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = Gray200,
                            focusedBorderColor = Purple60
                        ),
                        maxLines = 3
                    )

                    Spacer(Modifier.height(16.dp))

                    // 그룹 선택
                    Text(
                        "그룹 선택(선택사항)",
                        color = Gray600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = isGroupExpanded,
                        onExpandedChange = { isGroupExpanded = !isGroupExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedGroup?.name ?: "그룹 선택",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = Gray600,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .heightIn(min = 48.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundLight,
                                unfocusedContainerColor = BackgroundLight,
                                unfocusedBorderColor = Gray200,
                                focusedBorderColor = Purple60
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isGroupExpanded,
                            onDismissRequest = { isGroupExpanded = false }
                        ) {
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(StableColor.forId(g.id))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                g.name,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedGroup = g
                                        isGroupExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("메일 작성 언어", color = Gray600, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                ) {
                    OutlinedTextField(
                        value = languageDisplayText(languagePreference),
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = "프로필 기본값",
                                style = LocalTextStyle.current.copy(fontSize = 13.sp),
                                color = Gray600
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            disabledBorderColor = BorderGray,
                            unfocusedBorderColor = BorderGray,
                            focusedBorderColor = Purple60,
                            disabledContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소", fontSize = 14.sp, color = Gray600)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val finalSenderRole = when (senderMode) {
                                RoleInputMode.PRESET -> senderPreset
                                RoleInputMode.MANUAL -> senderManual.ifBlank { null }
                            }
                            val finalRecipientRole = when (recipientMode) {
                                RoleInputMode.PRESET -> recipientPreset ?: ""
                                RoleInputMode.MANUAL -> recipientManual.ifBlank { "" }
                            }
                            onConfirm(
                                name,
                                email,
                                finalSenderRole,
                                finalRecipientRole,
                                personalPrompt.ifBlank { null },
                                selectedGroup?.id,
                                languagePreference.ifBlank { null }
                            )
                        },
                        enabled = savable,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple60,
                            disabledContainerColor = Gray200
                        )
                    ) {
                        Text("연락처 추가", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = if (languagePreference == "KOR") "Korean" else languagePreference,
            onLanguageSelected = { selected -> languagePreference = selected },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
