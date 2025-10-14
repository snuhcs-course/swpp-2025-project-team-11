package com.fiveis.xend.data.repository

import com.fiveis.xend.data.model.EmailItem

enum class InboxTab { All, Sent }

class InboxRepository {

    /**
     * 받은편지함 이메일 목록 조회
     * TODO: 추후 실제 API 호출로 교체
     */
    fun getEmails(tab: InboxTab): List<EmailItem> = when (tab) {
        InboxTab.All -> getAllEmails()
        InboxTab.Sent -> getSentEmails()
    }

    private fun getAllEmails(): List<EmailItem> = listOf(
        EmailItem(
            id = "1",
            sender = "김대표 (대표이사)",
            subject = "Q4 실적 보고서 검토 부탁드립니다",
            content = "첨부된 실적 보고서를 검토해 주시고, 내일 오전 회의 전...",
            timestamp = "방금 전",
            unread = true
        ),
        EmailItem(
            id = "2",
            sender = "박팀장 (개발팀)",
            subject = "앱 출시 일정 관련 긴급 논의",
            content = "출시 일정에 변경사항이 생겨서 오늘 오후에 회의를...",
            timestamp = "1시간 전",
            unread = true
        ),
        EmailItem(
            id = "3",
            sender = "이부장 (마케팅)",
            subject = "신제품 마케팅 전략 피드백",
            content = "지난주에 공유드린 마케팅 전략안에 대한 의견을 듣고...",
            timestamp = "3시간 전",
            unread = false
        ),
        EmailItem(
            id = "4",
            sender = "최고객 (VIP 고객)",
            subject = "계약 조건 재협상 요청",
            content = "기존 계약 조건에 대한 몇 가지 수정 사항을 논의하고...",
            timestamp = "어제",
            unread = false
        ),
        EmailItem(
            id = "5",
            sender = "정동료 (같은팀)",
            subject = "회식 장소 추천 좀 부탁해요",
            content = "다음주 회식 장소를 정해야 하는데 좋은 곳 있으면...",
            timestamp = "2일 전",
            unread = false
        ),
        EmailItem(
            id = "6",
            sender = "홍보님 (인사팀)",
            subject = "연차 신청서 승인 완료",
            content = "신청하신 12월 연차가 승인되었습니다. 업무 인수인계...",
            timestamp = "3일 전",
            unread = false
        )
    )

    private fun getSentEmails(): List<EmailItem> = emptyList()
}
