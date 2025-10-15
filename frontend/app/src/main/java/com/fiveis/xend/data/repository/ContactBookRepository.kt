package com.fiveis.xend.data.repository

import androidx.compose.ui.graphics.Color
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group

enum class ContactBookTab { Groups, Contacts }

class ContactBookRepository {
    // call either getGroups() or getContacts()
    fun getContactInfo(tab: ContactBookTab): List<Any> = when (tab) {
        ContactBookTab.Groups -> getGroups()
        ContactBookTab.Contacts -> getContacts()
    }

    // 그룹 목록 화면용
    private fun getGroups(): List<Group> {
        return listOf(
            Group(
                id="1",
                name="VIP",
                description="중요한 고객과 상급자들",
                members=listOf(Contact(id="1", name="김철수", email="kim@snu.ac.kr", groupId="1"), Contact(id="2", name="최철수", email="choi@snu.ac.kr", groupId="1")),
                color=Color(0xFFFF5C5C)
            ),
            Group(
                id="2",
                name="업무 동료",
                description="같은 회사 팀원들과 협업 파트너",
                members=listOf(Contact(id="1", name="김철수", email="kim@snu.ac.kr", groupId="2"), Contact(id="2", name="최철수", email="choi@snu.ac.kr", groupId="2")),
                color=Color(0xFFFFA500)
            ),
            Group(
                id="3",
                name="학술 관계",
                description="교수님, 연구진과의 학문적 소통",
                members=listOf(
                    Contact(id="1", name="김철수", email="kim@snu.ac.kr", groupId="3"),
                    Contact(id="2", name="최철수", email="choi@snu.ac.kr", groupId="3"),
                    Contact(id="3", name="김철수", email="kim@snu.ac.kr", groupId="3"),
                    Contact(id="4", name="최철수", email="choi@snu.ac.kr", groupId="3"),
                    Contact(id="5", name="김철수", email="kim@snu.ac.kr", groupId="3")),
                color=Color(0xFF8A2BE2)
            )
        )
    }

    // 전체 연락처 화면용
    private fun getContacts(): List<Contact> {
        return listOf(
            Contact(id="1",name="김철수", email="kim@snu.ac.kr", groupId="1"),
            Contact(id="2", name="최철수", email="choi@snu.ac.kr", groupId="2")
        )
    }
}
