package com.fiveis.xend.ui.compose

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.UUID

/**
 * 템플릿 카테고리
 */
enum class TemplateCategory(val displayName: String) {
    ALL("전체"),
    WORK("업무"),
    SCHOOL("학업"),
    PERSONAL("개인")
}

/**
 * 메일 템플릿 데이터 모델
 */
data class EmailTemplate(
    val id: String,
    val category: TemplateCategory,
    val title: String,
    val description: String,
    val subject: String,
    val body: String
)

/**
 * 샘플 템플릿 데이터
 */
object TemplateData {
    private val initialTemplates = listOf(
        EmailTemplate(
            id = "1",
            category = TemplateCategory.WORK,
            title = "업무 협조 요청",
            description = "동료나 타 부서에 업무 협조를 정중하게 요청할 때",
            subject = "업무 협조 요청 드립니다",
            body = """
안녕하세요, [수신자 이름]님.

[프로젝트명 또는 업무명]과 관련하여 [구체적인 업무 내용]에 대한 협조를 부탁드리고자 메일드립니다.

현재 저희 팀에서는 [진행 상황 또는 목적]을 진행 중이며, 원활한 수행을 위해 [협조 요청 사항]이 필요합니다.
가능하시다면 [요청 기한]까지 검토 및 회신 부탁드립니다.

바쁘신 와중에도 시간 내주셔서 감사합니다.
좋은 하루 되시길 바랍니다.

감사합니다.
[보내는 사람 이름 / 부서명 / 연락처]
            """.trimIndent()
        ),
        EmailTemplate(
            id = "2",
            category = TemplateCategory.WORK,
            title = "면접 합격 통보",
            description = "면접 결과를 지원자에게 전달할 때",
            subject = "면접 결과 안내드립니다",
            body = """
안녕하세요, [지원자 이름]님.
[회사명] 인사담당자 [보내는 사람 이름]입니다.

먼저, 당사 [직무명] 채용 면접에 참여해 주셔서 진심으로 감사드립니다.
면접 결과, [지원자 이름]님께서 최종 합격되셨음을 기쁜 마음으로 알려드립니다.

향후 절차(입사 일정, 제출 서류 등)에 대해서는 별도의 이메일로 상세히 안내드릴 예정입니다.
궁금하신 사항이 있으시면 언제든 회신 부탁드립니다.

다시 한번 합격을 축하드립니다.
감사합니다.
[회사명 / 인사팀 / 담당자명 / 연락처]
            """.trimIndent()
        ),
        EmailTemplate(
            id = "3",
            category = TemplateCategory.SCHOOL,
            title = "교수님께 질문",
            description = "수업 내용이나 과제에 대해 질문할 때",
            subject = "[수업명] 관련 질문드립니다",
            body = """
교수님, 안녕하세요.
[수업명]을 수강 중인 [학번] [이름]입니다.

이번 주 수업에서 다루신 [주제명] 부분을 복습하던 중 몇 가지 궁금한 점이 있어 문의드립니다.

[구체적인 질문 내용 — 예: 특정 개념이나 과제의 요구사항 등]

바쁘신 일정에도 답변 주시면 감사하겠습니다.
좋은 하루 되시길 바랍니다.

감사합니다.
[이름 / 학번 / 연락처(선택)]
            """.trimIndent()
        ),
        EmailTemplate(
            id = "4",
            category = TemplateCategory.PERSONAL,
            title = "정원의 수강 신청",
            description = "수강정원이 차서 별도 허락을 요청할 때",
            subject = "[과목명] 수강 허가 요청드립니다",
            body = """
교수님, 안녕하세요.
[과목명] 수강을 희망하는 [학번] [이름]입니다.

수강 신청 기간 중 [과목명]의 정원이 마감되어 부득이하게 별도 허가를 요청드리고자 메일드립니다.
이 과목은 제 전공 및 진로에 꼭 필요한 내용이 포함되어 있어 꼭 수강하고 싶습니다.

혹시 가능하시다면 추가 허가를 부탁드립니다.
시간 내어 검토해 주셔서 감사드립니다.

감사합니다.
[이름 / 학번 / 학과명]
            """.trimIndent()
        ),
        EmailTemplate(
            id = "5",
            category = TemplateCategory.PERSONAL,
            title = "감사 인사",
            description = "도움을 받았을 때 고마움을 표현하는 메일",
            subject = "감사 인사드립니다",
            body = """
안녕하세요, [수신자 이름]님.

최근 [도움을 받은 구체적 상황 — 예: 프로젝트 조언, 업무 지원, 추천서 작성 등]에서 큰 도움을 주셔서 진심으로 감사드립니다.
덕분에 [결과나 성과 — 예: 문제 해결, 프로젝트 성공, 좋은 평가 등]를 얻을 수 있었습니다.

[수신자 이름]님의 조언과 배려 덕분에 많은 것을 배우고 성장할 수 있었습니다.
언제 기회가 된다면 저도 꼭 보답드릴 수 있으면 좋겠습니다.

항상 건강하시고, 좋은 일들만 가득하시길 바랍니다.

감사합니다.
[보내는 사람 이름]
            """.trimIndent()
        )
    )

    val templates: SnapshotStateList<EmailTemplate> = mutableStateListOf(*initialTemplates.toTypedArray())

    fun getTemplatesByCategory(category: TemplateCategory): List<EmailTemplate> {
        return if (category == TemplateCategory.ALL) {
            templates
        } else {
            templates.filter { it.category == category }
        }
    }

    fun addTemplate(category: TemplateCategory, title: String, description: String, subject: String, body: String) {
        val newTemplate = EmailTemplate(
            id = UUID.randomUUID().toString(),
            category = category,
            title = title,
            description = description,
            subject = subject,
            body = body
        )
        templates.add(0, newTemplate) // Add to beginning of list
    }

    fun deleteTemplate(templateId: String) {
        templates.removeIf { it.id == templateId }
    }

    fun updateTemplate(
        templateId: String,
        category: TemplateCategory,
        title: String,
        description: String,
        subject: String,
        body: String
    ) {
        val index = templates.indexOfFirst { it.id == templateId }
        if (index != -1) {
            templates[index] = EmailTemplate(
                id = templateId,
                category = category,
                title = title,
                description = description,
                subject = subject,
                body = body
            )
        }
    }
}
