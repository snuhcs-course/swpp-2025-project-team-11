package com.fiveis.xend.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.Blue80
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary

data class LanguageOption(
    val value: String,
    val displayName: String,
    val flagEmoji: String
) {
    val label: String get() = "$flagEmoji  $displayName"
}

val languageOptions = listOf(
    LanguageOption("Korean", "Korean", "\uD83C\uDDF0\uD83C\uDDF7"),
    LanguageOption("English", "English", "\uD83C\uDDFA\uD83C\uDDF8"),
    LanguageOption("Spanish", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8"),
    LanguageOption("French", "French", "\uD83C\uDDEB\uD83C\uDDF7"),
    LanguageOption("German", "German", "\uD83C\uDDE9\uD83C\uDDEA"),
    LanguageOption("Japanese", "Japanese", "\uD83C\uDDEF\uD83C\uDDF5"),
    LanguageOption("Chinese", "Chinese", "\uD83C\uDDE8\uD83C\uDDF3"),
    LanguageOption("Portuguese", "Portuguese", "\uD83C\uDDF5\uD83C\uDDF9")
)

fun languageDisplayText(value: String): String {
    if (value.isBlank()) return ""
    if (value == "KOR") return languageOptions[0].label
    return languageOptions.firstOrNull { it.value.equals(value, ignoreCase = true) }?.label ?: value
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onForceLogout: () -> Unit = {},
    onDismissLogoutFailureDialog: () -> Unit = {},
    onToggleEditMode: () -> Unit = {},
    onUpdateDisplayName: (String) -> Unit = {},
    onUpdateInfo: (String) -> Unit = {},
    onUpdateLanguagePreference: (String) -> Unit = {},
    onSaveProfile: () -> Unit = {},
    onDismissProfileError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ProfileHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Profile Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF4285F4)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // User Email
                Text(
                    text = uiState.userEmail,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF202124)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Xend 계정",
                    fontSize = 14.sp,
                    color = Color(0xFF5F6368)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Information Card
                ProfileInfoCard(
                    uiState = uiState,
                    onToggleEditMode = onToggleEditMode,
                    onUpdateDisplayName = onUpdateDisplayName,
                    onUpdateInfo = onUpdateInfo,
                    onUpdateLanguagePreference = onUpdateLanguagePreference,
                    onSaveProfile = onSaveProfile
                )

                // Success/Error Messages
                if (uiState.saveSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "프로필이 저장되었습니다",
                        fontSize = 14.sp,
                        color = Color(0xFF34A853),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (uiState.profileError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.profileError,
                        fontSize = 14.sp,
                        color = Color(0xFFEA4335),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (uiState.logoutError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.logoutError,
                        fontSize = 14.sp,
                        color = Color(0xFFEA4335),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout Button
                Button(
                    onClick = onLogout,
                    enabled = !uiState.isLoggingOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA4335),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    if (uiState.isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "로그아웃",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Logout Failure Dialog
        if (uiState.showLogoutFailureDialog) {
            LogoutFailureDialog(
                errorMessage = uiState.logoutError ?: "로그아웃 중 오류가 발생했습니다",
                onDismiss = onDismissLogoutFailureDialog,
                onForceLogout = onForceLogout
            )
        }
    }
}

@Composable
private fun ProfileInfoCard(
    uiState: ProfileUiState,
    onToggleEditMode: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateInfo: (String) -> Unit,
    onUpdateLanguagePreference: (String) -> Unit,
    onSaveProfile: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = if (uiState.languagePreference == "KOR") "Korean" else uiState.languagePreference,
            onLanguageSelected = {
                onUpdateLanguagePreference(it)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "개인정보",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF202124)
                )

                if (!uiState.isEditing) {
                    IconButton(
                        onClick = onToggleEditMode,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "편집",
                            tint = Blue80
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Blue80
                    )
                }
            } else if (uiState.isEditing) {
                // Edit Mode
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onUpdateDisplayName,
                    label = { Text("표시 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue80,
                        focusedLabelColor = Blue80,
                        cursorColor = Blue80
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.info,
                    onValueChange = onUpdateInfo,
                    label = { Text("소개") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue80,
                        focusedLabelColor = Blue80,
                        cursorColor = Blue80
                    ),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                ) {
                    Text(
                        text = "메일 언어 설정",
                        fontSize = 13.sp,
                        color = Color(0xFF5A5E60)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = languageDisplayText(uiState.languagePreference),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = null,
                        placeholder = {
                            Text("터치하여 메일 작성 언어를 설정하세요", color = TextSecondary, fontSize = 15.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue80,
                            focusedTextColor = TextPrimary,
                            disabledTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onToggleEditMode,
                        enabled = !uiState.isSaving
                    ) {
                        Text("취소", color = Color(0xFF5F6368))
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = onSaveProfile,
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue80
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("저장")
                        }
                    }
                }
            } else {
                // View Mode
                ProfileInfoItem(label = "표시 이름", value = uiState.displayName.ifBlank { "설정되지 않음" })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileInfoItem(label = "소개", value = uiState.info.ifBlank { "설정되지 않음" })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileInfoItem(
                    label = "메일 작성 언어",
                    value = languageDisplayText(uiState.languagePreference).ifBlank { "설정되지 않음" }
                )
            }
        }
    }
}

@Composable
fun LanguageDialog(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    languages: List<LanguageOption> = languageOptions
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingSelection by rememberSaveable(selectedLanguage) { mutableStateOf(selectedLanguage) }

    val filteredLanguages = remember(searchQuery, languages) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            languages
        } else {
            languages.filter { option ->
                option.displayName.contains(query, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "메일 언어 설정",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "메일 작성에 사용할 언어를 선택하세요",
                    fontSize = 15.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                val selectedOption = languages.firstOrNull { option ->
                    option.value.equals(pendingSelection, ignoreCase = true)
                }

                SelectedLanguageCard(
                    selectedOption = selectedOption,
                    onClearSelection = { pendingSelection = "" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("언어 검색") },
                    placeholder = { Text("ex) English") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF12110D)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue80,
                        focusedLabelColor = Blue80,
                        cursorColor = Blue80
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                val listModifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)

                if (filteredLanguages.isEmpty()) {
                    Box(
                        modifier = listModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "일치하는 언어가 없습니다.",
                            color = Color(0xFF5F6368)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = listModifier
                    ) {
                        items(filteredLanguages) { option ->
                            val isOptionSelected = option.value.equals(pendingSelection, ignoreCase = true)
                            LanguageOptionRow(
                                option = option,
                                isSelected = isOptionSelected,
                                onSelect = {
                                    pendingSelection = if (isOptionSelected) "" else option.value
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onLanguageSelected(pendingSelection)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue60,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "완료",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedLanguageCard(selectedOption: LanguageOption?, onClearSelection: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, Blue60),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagBadge(flagEmoji = selectedOption?.flagEmoji ?: "\uD83C\uDF10")
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "선택된 언어",
                    fontSize = 13.sp,
                    color = Color(0xFF5A5E60)
                )
                Text(
                    text = selectedOption?.displayName ?: "선택된 언어가 없습니다",
                    fontSize = 16.sp,
                    color = Color(0xFF12110D),
                    fontWeight = FontWeight.Medium
                )
            }
//            SelectionIndicator(
//                isSelected = selectedOption != null,
//                onClick = {
//                    if (selectedOption != null) {
//                        onClearSelection()
//                    }
//                }
//            )

            if (selectedOption != null) {
                IconButton(
                    onClick = { onClearSelection() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "언어 설정 취소"
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(option: LanguageOption, isSelected: Boolean, onSelect: (LanguageOption) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) Color(0xFFEBFAFB) else Color.Transparent)
            .clickable { onSelect(option) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagBadge(flagEmoji = option.flagEmoji)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = option.displayName,
            fontSize = 16.sp,
            color = Color(0xFF12110D),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        SelectionIndicator(
            isSelected = isSelected,
            onClick = { onSelect(option) }
        )
    }
}

@Composable
private fun FlagBadge(flagEmoji: String) {
    Box(
        modifier = Modifier
            .size(41.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0x1A12110D)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = flagEmoji,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean, onClick: (() -> Unit)? = null) {
    val backgroundColor = if (isSelected) Blue60 else Color.White
    val borderColor = if (isSelected) Color.Transparent else Color(0xFFE6E6E6)

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF5F6368),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF202124)
        )
    }
}

@Composable
private fun LogoutFailureDialog(errorMessage: String, onDismiss: () -> Unit, onForceLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "로그아웃 실패",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF5F6368)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "서버와의 연결이 실패했습니다.\n강제 로그아웃 시 로컬 데이터가 모두 삭제됩니다.",
                    fontSize = 13.sp,
                    color = Color(0xFF80868B)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onForceLogout,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFEA4335)
                )
            ) {
                Text("강제 로그아웃")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Blue80
                )
            ) {
                Text("취소")
            }
        },
        containerColor = Color.White,
        tonalElevation = 0.dp
    )
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
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
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0xFF5F6368)
                )
            }

            Text(
                text = "프로필",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF202124),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFFE8EAED),
            thickness = 1.dp
        )
    }
}
