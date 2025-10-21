package com.fiveis.xend.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.AddContactRequestContext
import com.fiveis.xend.data.model.AddGroupRequest
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactContext
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.network.ContactApiService
import com.fiveis.xend.network.RetrofitClient
import kotlin.random.Random

enum class ContactBookTab { Groups, Contacts }

sealed interface ContactBookData
data class GroupData(val groups: List<Group>) : ContactBookData
data class ContactData(val contacts: List<Contact>) : ContactBookData

private var contactColorRandomSeed: Long = 5L
private var groupColorRandomSeed: Long = 7L
private var contactRnd: Random = Random(contactColorRandomSeed)
private var groupRnd: Random = Random(groupColorRandomSeed)
fun randomNotTooLightColor(rnd: Random = Random.Default): Color {
    val hue = rnd.nextFloat() * 360f
    val saturation = 0.65f + rnd.nextFloat() * 0.35f // 0.65 ~ 1.00
    val value = 0.45f + rnd.nextFloat() * 0.40f // 0.45 ~ 0.85
    return Color.hsv(hue, saturation, value)
}

class ContactBookRepository(context: Context) {
    private val contactApiService: ContactApiService = RetrofitClient.getContactApiService(context)

    // call either getGroups() or getContacts()
    suspend fun getContactInfo(tab: ContactBookTab): ContactBookData = when (tab) {
        ContactBookTab.Groups -> GroupData(getAllGroups())
        ContactBookTab.Contacts -> ContactData(getAllContacts())
    }

    // 그룹 목록 화면용
    fun getDummyGroups(): List<Group> {
        return listOf(
            Group(
                id = 1L,
                name = "VIP",
                description = "중요한 고객과 상급자들",
                members = listOf(
                    Contact(id = 1L, name = "김철수", email = "kim@snu.ac.kr", groupId = 1L),
                    Contact(id = 2L, name = "최철수", email = "choi@snu.ac.kr", groupId = 1L)
                ),
                color = Color(0xFFFF5C5C)
            ),
            Group(
                id = 2L,
                name = "업무 동료",
                description = "같은 회사 팀원들과 협업 파트너",
                members = listOf(
                    Contact(id = 1L, name = "김철수", email = "kim@snu.ac.kr", groupId = 2L),
                    Contact(id = 2L, name = "최철수", email = "choi@snu.ac.kr", groupId = 2L)
                ),
                color = Color(0xFFFFA500)
            ),
            Group(
                id = 3L,
                name = "학술 관계",
                description = "교수님, 연구진과의 학문적 소통",
                members = listOf(
                    Contact(id = 1L, name = "김철수", email = "kim@snu.ac.kr", groupId = 3L),
                    Contact(id = 2L, name = "최철수", email = "choi@snu.ac.kr", groupId = 3L),
                    Contact(id = 3L, name = "이영희", email = "lee@snu.ac.kr", groupId = 2L),
                    Contact(id = 4L, name = "박민수", email = "park@snu.ac.kr", groupId = 3L),
                    Contact(id = 5L, name = "정수진", email = "jung@snu.ac.kr", groupId = 3L)
                ),
                color = Color(0xFF8A2BE2)
            )
        )
    }

    // 전체 연락처 화면용
    fun getDummyContacts(): List<Contact> {
        return listOf(
            Contact(id = 1L, name = "김철수", email = "kim@snu.ac.kr", groupId = 1L),
            Contact(id = 2L, name = "최철수", email = "choi@snu.ac.kr", groupId = 1L),
            Contact(id = 3L, name = "이영희", email = "lee@snu.ac.kr", groupId = 2L),
            Contact(id = 4L, name = "박민수", email = "park@snu.ac.kr", groupId = 3L),
            Contact(id = 5L, name = "정수진", email = "jung@snu.ac.kr", groupId = 3L)
        )
    }

    suspend fun addContact(
        name: String,
        email: String,
        relationshipRole: String,
        personalPrompt: String?
    ): ContactResponse {
        val requestContext = AddContactRequestContext(
            relationshipRole = relationshipRole,
            personalPrompt = personalPrompt
        )

        val request = AddContactRequest(
            name = name,
            email = email,
            context = requestContext
        )

        val response = contactApiService.addContact(
            payload = request
        )

        if (response.isSuccessful) {
            return response.body()
                ?: throw IllegalStateException("Success response but body is null")
        } else {
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "Add contact failed: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
    }

    suspend fun getAllContacts(): List<Contact> {
        val response = contactApiService.getAllContacts()
        if (response.isSuccessful) {
            return response.body()?.map { contactData ->
                Contact(
                    id = contactData.id,
                    groupId = contactData.groupId,
                    name = contactData.name,
                    email = contactData.email,
                    context = contactData.context?.let { contextData ->
                        ContactContext(
                            id = contextData.id,
                            relationshipRole = contextData.relationshipRole,
                            relationshipDetails = contextData.relationshipDetails,
                            personalPrompt = contextData.personalPrompt,
                            languagePreference = contextData.languagePreference,
                            createdAt = contextData.createdAt,
                            updatedAt = contextData.updatedAt
                        )
                    },
                    createdAt = contactData.createdAt,
                    updatedAt = contactData.updatedAt,
                    color = randomNotTooLightColor(contactRnd)
                )
            } ?: emptyList()
        }
        throw IllegalStateException("Failed to get all contacts: HTTP ${response.code()} ${response.message()}")
    }

    suspend fun deleteContact(contactId: Long) {
        val response = contactApiService.deleteContact(contactId)
        if (!response.isSuccessful) {
            throw IllegalStateException("Failed to delete contact: HTTP ${response.code()} ${response.message()}")
        }
    }

    suspend fun addGroup(name: String, description: String): GroupResponse {
        val request = AddGroupRequest(
            name = name,
            description = description
        )

        val response = contactApiService.addGroup(
            payload = request
        )

        if (response.isSuccessful) {
            return response.body()
                ?: throw IllegalStateException("Success response but body is null")
        } else {
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "Add group failed: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
    }

    suspend fun getAllGroups(): List<Group> {
        val response = contactApiService.getAllGroups()
        if (response.isSuccessful) {
            return response.body()?.map {
                Group(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    members = emptyList(),
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    color = randomNotTooLightColor(groupRnd)
                )
            } ?: emptyList()
        }
        throw IllegalStateException("Failed to get all groups: HTTP ${response.code()} ${response.message()}")
    }
}
