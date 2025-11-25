package com.fiveis.xend.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fiveis.xend.ui.theme.BackgroundWhite
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.TemplateBackground
import com.fiveis.xend.ui.theme.TemplateCardBg
import com.fiveis.xend.ui.theme.TemplateCardBorder
import com.fiveis.xend.ui.theme.TemplateCardDescription
import com.fiveis.xend.ui.theme.TemplateCardTitle
import com.fiveis.xend.ui.theme.TemplateNewButtonBg
import com.fiveis.xend.ui.theme.TemplateNewButtonText
import com.fiveis.xend.ui.theme.TemplateSelectedTabBg
import com.fiveis.xend.ui.theme.TemplateSelectedTabText
import com.fiveis.xend.ui.theme.TemplateTitle
import com.fiveis.xend.ui.theme.TemplateUnselectedTabBg
import com.fiveis.xend.ui.theme.TemplateUnselectedTabStroke
import com.fiveis.xend.ui.theme.TemplateUnselectedTabText
import com.fiveis.xend.ui.theme.TemplateUseButtonBg
import com.fiveis.xend.ui.theme.TemplateUseButtonText
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionScreen(
    onBack: () -> Unit,
    onTemplateSelected: (EmailTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(TemplateCategory.ALL) }
    var showNewTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplateForView by remember { mutableStateOf<EmailTemplate?>(null) }
    val filteredTemplates = remember(selectedCategory) {
        TemplateData.getTemplatesByCategory(selectedCategory)
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                title = {
                    Text(
                        "템플릿 선택",
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
                actions = {
                    ToolbarIconButton(
                        onClick = { /* TODO: 검색 기능 */ },
                        border = null,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "검색",
                            tint = ToolbarIconTint
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite,
                    scrolledContainerColor = BackgroundWhite
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(TemplateBackground)
        ) {
            // 카테고리 탭
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // "템플릿" 제목
            Text(
                text = "템플릿",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TemplateTitle,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // 템플릿 리스트
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
            ) {
                items(filteredTemplates) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { selectedTemplateForView = template },
                        onUseClick = { onTemplateSelected(template) },
                        onDeleteClick = { TemplateData.deleteTemplate(template.id) }
                    )
                }
            }

            // "새 템플릿 만들기" 버튼
            NewTemplateButton(
                onClick = { showNewTemplateDialog = true },
                modifier = Modifier.padding(20.dp)
            )
        }
    }

    // 새 템플릿 만들기 Dialog
    if (showNewTemplateDialog) {
        NewTemplateDialog(
            onDismiss = { showNewTemplateDialog = false },
            onSave = { category, title, description, subject, body ->
                TemplateData.addTemplate(category, title, description, subject, body)
            }
        )
    }

    // 템플릿 상세보기 Dialog
    selectedTemplateForView?.let { template ->
        NewTemplateDialog(
            onDismiss = { selectedTemplateForView = null },
            onSave = { category, title, description, subject, body ->
                TemplateData.updateTemplate(template.id, category, title, description, subject, body)
            },
            template = template
        )
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: TemplateCategory,
    onCategorySelected: (TemplateCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TemplateCategory.entries) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: TemplateCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        },
        modifier = modifier.height(35.dp),
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = TemplateUnselectedTabBg,
            selectedContainerColor = TemplateSelectedTabBg,
            labelColor = TemplateUnselectedTabText,
            selectedLabelColor = TemplateSelectedTabText
        ),
        border = if (isSelected) {
            null
        } else {
            BorderStroke(1.dp, TemplateUnselectedTabStroke)
        }
    )
}

@Composable
private fun TemplateCard(
    template: EmailTemplate,
    onClick: () -> Unit,
    onUseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = TemplateCardBg,
        border = BorderStroke(2.dp, TemplateCardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = template.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TemplateCardTitle
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = template.description,
                        fontSize = 13.sp,
                        color = TemplateCardDescription,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // "사용" 버튼
                Button(
                    onClick = onUseClick,
                    modifier = Modifier
                        .width(47.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TemplateUseButtonBg,
                        contentColor = TemplateUseButtonText
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = "사용",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 삭제 버튼 (오른쪽 위)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "삭제",
                    tint = TemplateCardDescription,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NewTemplateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TemplateNewButtonBg,
            contentColor = TemplateNewButtonText
        ),
        border = BorderStroke(1.dp, TemplateUnselectedTabStroke)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = TemplateNewButtonText
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "새 템플릿 만들기",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
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
private fun NewTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (TemplateCategory, String, String, String, String) -> Unit,
    template: EmailTemplate? = null
) {
    var selectedCategory by remember { mutableStateOf(template?.category ?: TemplateCategory.WORK) }
    var title by remember { mutableStateOf(template?.title ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var mailSubject by remember { mutableStateOf(template?.subject ?: "") }
    var mailBody by remember { mutableStateOf(template?.body ?: "") }
    val scrollState = rememberScrollState()
    val isViewMode = template != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = BackgroundWhite
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // 제목
                Text(
                    text = if (isViewMode) "템플릿 상세" else "새 템플릿 만들기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 카테고리 선택
                Text(
                    text = "카테고리",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ToolbarIconTint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(TemplateCategory.entries.filter { it != TemplateCategory.ALL }) { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.displayName,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = TemplateUnselectedTabBg,
                                selectedContainerColor = TemplateSelectedTabBg,
                                labelColor = TemplateUnselectedTabText,
                                selectedLabelColor = TemplateSelectedTabText
                            ),
                            border = if (category == selectedCategory) {
                                null
                            } else {
                                BorderStroke(1.dp, TemplateUnselectedTabStroke)
                            }
                        )
                    }
                }

                // 템플릿 제목
                Text(
                    text = "템플릿 제목",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ToolbarIconTint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(fontSize = 12.sp),
                    placeholder = { Text("예: 업무 협조 요청", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = ComposeSurface,
                        unfocusedContainerColor = ComposeSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // 템플릿 설명
                Text(
                    text = "템플릿 설명",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ToolbarIconTint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = TextStyle(fontSize = 12.sp),
                    placeholder = { Text("예: 동료나 타 부서에 업무 협조를 요청할 때", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = ComposeSurface,
                        unfocusedContainerColor = ComposeSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // 메일 제목
                Text(
                    text = "메일 제목",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ToolbarIconTint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = mailSubject,
                    onValueChange = { mailSubject = it },
                    textStyle = TextStyle(fontSize = 12.sp),
                    placeholder = { Text("예: 업무 협조 요청 드립니다", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = ComposeSurface,
                        unfocusedContainerColor = ComposeSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // 메일 본문
                Text(
                    text = "메일 본문",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ToolbarIconTint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = mailBody,
                    onValueChange = { mailBody = it },
                    textStyle = TextStyle(fontSize = 12.sp),
                    placeholder = { Text("메일 본문 내용을 입력하세요", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 5,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = ComposeSurface,
                        unfocusedContainerColor = ComposeSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // 버튼
                val canSave =
                    title.isNotBlank() && description.isNotBlank() &&
                        mailSubject.isNotBlank() && mailBody.isNotBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (canSave) {
                                onSave(selectedCategory, title, description, mailSubject, mailBody)
                                onDismiss()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TemplateSelectedTabBg,
                            contentColor = Color.White,
                            disabledContainerColor = TemplateSelectedTabBg.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateSelectionScreenPreview() {
    TemplateSelectionScreen(
        onBack = {},
        onTemplateSelected = {}
    )
}
