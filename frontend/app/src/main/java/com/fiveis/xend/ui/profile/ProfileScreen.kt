package com.fiveis.xend.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.ui.theme.Blue80

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
    onSaveProfile: () -> Unit
) {
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
            }
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
