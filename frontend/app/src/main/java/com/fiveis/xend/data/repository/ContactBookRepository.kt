package com.fiveis.xend.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.asDomain
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.AddContactRequestContext
import com.fiveis.xend.data.model.AddGroupRequest
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.model.PromptOptionRequest
import com.fiveis.xend.network.ContactApiService
import com.fiveis.xend.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ContactBookTab { Groups, Contacts }

sealed interface ContactBookData
data class GroupData(val groups: List<Group>) : ContactBookData
data class ContactData(val contacts: List<Contact>) : ContactBookData

// private var contactColorRandomSeed: Long = 5L
// private var groupColorRandomSeed: Long = 10L
// private var contactRnd: Random = Random(contactColorRandomSeed)
// private var groupRnd: Random = Random(groupColorRandomSeed)
// fun randomNotTooLightColor(rnd: Random = Random.Default): Color {
//     val hue = rnd.nextFloat() * 360f
//     val saturation = 0.65f + rnd.nextFloat() * 0.35f // 0.65 ~ 1.00
//     val value = 0.45f + rnd.nextFloat() * 0.40f // 0.45 ~ 0.85
//     return Color.hsv(hue, saturation, value)
// }

class ContactBookRepository(
    context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val api: ContactApiService = RetrofitClient.getContactApiService(context)

    // Room 주입
    private val groupDao = db.groupDao()
    private val contactDao = db.contactDao()
    private val optionDao = db.promptOptionDao()

    suspend fun localGroups(): List<Group> = groupDao.getGroupsWithMembersAndOptions().map { it.asDomain() }

    suspend fun localGroup(id: Long): Group? = groupDao.getGroupWithMembersAndOptions(id)?.asDomain()

    suspend fun localContacts(): List<Contact> = contactDao.getAllWithContext().map { it.asDomain(null) }

    suspend fun localContact(id: Long): Contact? = contactDao.getContactWithContext(id)?.asDomain(null)

    // 필요 시: 특정 그룹 멤버만
    suspend fun localContactsByGroup(groupId: Long): List<Contact> =
        contactDao.getContactsByGroupIdWithContext(groupId).map { it.asDomain(null) }

    suspend fun localContactWithGroup(id: Long): Contact? = contactDao.getByIdWithGroup(id)?.asDomain()

    fun observeGroups(): Flow<List<Group>> = groupDao.observeGroupsWithMembersAndOptions()
        .map { list -> list.map { it.asDomain() } }

    fun observeGroup(groupId: Long): Flow<Group?> = groupDao.observeGroup(groupId).map { it?.asDomain() }

    fun observeContacts(): Flow<List<Contact>> = contactDao.observeAllWithGroup()
        .map { list -> list.map { it.asDomain() } }

    fun observePromptOptions(): Flow<List<PromptOption>> = optionDao.observeAllOptions()
        .map { list -> list.map { it.asDomain() } }

    fun observeContact(id: Long): Flow<Contact?> = contactDao.observeByIdWithGroup(id).map { it?.asDomain() }

    fun searchContacts(keyword: String): Flow<List<Contact>> =
        contactDao.searchByNameOrEmail(keyword).map { list -> list.map { it.asDomain(null) } }

    // ----- DB 동기화(refresh) -----

    suspend fun refreshGroups() {
        val res = api.getAllGroups()
        if (!res.isSuccessful) error("HTTP ${res.code()} ${res.message()}")
        val body = res.body().orEmpty()

        db.withTransaction {
            optionDao.deleteAllCrossRefs()
            groupDao.deleteAllGroups()

            val groups = mutableListOf<GroupEntity>()
            val optionSet = linkedSetOf<PromptOptionEntity>()
            val refs = mutableListOf<GroupPromptOptionCrossRef>()

            body.forEach { gr ->
                val (g, opts, rfs) = gr.toEntities()
                groups += g
                optionSet += opts
                refs += rfs
            }
            groupDao.upsertGroups(groups)
            if (optionSet.isNotEmpty()) optionDao.upsertOptions(optionSet.toList())
            if (refs.isNotEmpty()) optionDao.upsertCrossRefs(refs)
        }
    }

    suspend fun refreshGroup(groupId: Long) {
        val res = api.getGroup(groupId)
        if (!res.isSuccessful) error("HTTP ${res.code()} ${res.message()}")
        val r = res.body() ?: error("Group body null")

        db.withTransaction {
            val (g, opts, refs) = r.toEntities()
            groupDao.upsertGroups(listOf(g))
            if (opts.isNotEmpty()) optionDao.upsertOptions(opts)
            optionDao.deleteCrossRefsByGroup(r.id)
            if (refs.isNotEmpty()) optionDao.upsertCrossRefs(refs)
        }
    }

    suspend fun refreshContacts() {
        val res = api.getAllContacts()
        if (!res.isSuccessful) error("HTTP ${res.code()} ${res.message()}")
        val body = res.body().orEmpty()

        db.withTransaction {
            contactDao.deleteAllContexts()
            contactDao.deleteAllContacts()
            val contacts = mutableListOf<ContactEntity>()
            val contexts = mutableListOf<ContactContextEntity>()
            body.forEach { cr ->
                val (c, ctx) = cr.toEntities()
                contacts += c
                ctx?.let { contexts += it }
            }
            contactDao.upsertContacts(contacts)
            if (contexts.isNotEmpty()) contactDao.upsertContexts(contexts)
        }
    }

    suspend fun refreshContact(id: Long) {
        val res = api.getContact(id)
        if (!res.isSuccessful) error("HTTP ${res.code()} ${res.message()}")
        val r = res.body() ?: error("Contact body null")
        db.withTransaction {
            val (c, ctx) = r.toEntities()
            contactDao.upsertContacts(listOf(c))
            ctx?.let { contactDao.upsertContexts(listOf(it)) }
        }
    }

    suspend fun refreshGroupAndMembers(groupId: Long) {
        refreshGroup(groupId)
        refreshContacts()
    }

    suspend fun refreshPromptOptions() {
        val res = api.getAllPromptOptions()
        if (!res.isSuccessful) error("HTTP ${res.code()} ${res.message()}")
        val all = res.body().orEmpty()
        optionDao.upsertOptions(all.map { it.toEntity() })
    }

    // ----- 읽기(get) API -----

    suspend fun getAllContacts(): List<Contact> = localContacts()

    suspend fun getAllGroups(): List<Group> = localGroups()

    suspend fun getContact(id: Long): Contact =
        localContact(id) ?: throw IllegalStateException("Contact($id) not found locally")

    suspend fun getGroup(id: Long): Group =
        localGroup(id) ?: throw IllegalStateException("Group($id) not found locally")

    // ======================
    // 쓰기 API (성공 시 로컬 갱신)
    // ======================

    suspend fun addContact(
        name: String,
        email: String,
        groupId: Long?,
        senderRole: String?,
        recipientRole: String,
        personalPrompt: String?
    ): ContactResponse {
        val requestContext = AddContactRequestContext(
            senderRole = senderRole ?: "",
            recipientRole = recipientRole,
            personalPrompt = personalPrompt ?: ""
        )
        val request = AddContactRequest(name = name, email = email, groupId = groupId, context = requestContext)
        val res = api.addContact(request)
        if (!res.isSuccessful) {
            val body = res.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException("Add contact failed: HTTP ${res.code()} ${res.message()} | body=$body")
        }
        val r = res.body() ?: error("Success but body null")
        // 로컬 반영
        db.withTransaction {
            val (c, ctx) = r.toEntities()
            contactDao.upsertContacts(listOf(c))
            ctx?.let { contactDao.upsertContexts(listOf(it)) }
        }
        return r
    }

    suspend fun deleteContact(contactId: Long) {
        val res = api.deleteContact(contactId)
        if (!res.isSuccessful) {
            throw IllegalStateException("Failed to delete contact: HTTP ${res.code()} ${res.message()}")
        }
        // 로컬 반영
        contactDao.deleteById(contactId)
    }

    suspend fun updateContactGroup(contactId: Long, groupId: Long?) {
        val payload = mapOf<String, Any?>("group_id" to groupId)
        val response = api.updateContact(contactId, payload)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "Update contact group failed: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
        val body = response.body()
        if (body != null) {
            db.withTransaction {
                val (contact, ctx) = body.toEntities()
                contactDao.upsertContacts(listOf(contact))
                ctx?.let { contactDao.upsertContexts(listOf(it)) }
            }
        } else {
            refreshContact(contactId)
        }
    }

    suspend fun updateContact(
        contactId: Long,
        name: String,
        email: String,
        senderRole: String?,
        recipientRole: String?,
        personalPrompt: String?,
        groupId: Long?
    ) {
        val payload = mutableMapOf<String, Any?>(
            "name" to name,
            "email" to email,
            "group_id" to groupId
        )
        val contextPayload = mutableMapOf<String, Any?>()
        if (senderRole != null) contextPayload["sender_role"] = senderRole
        if (recipientRole != null) contextPayload["recipient_role"] = recipientRole
        if (personalPrompt != null) contextPayload["personal_prompt"] = personalPrompt
        if (contextPayload.isNotEmpty()) {
            payload["context"] = contextPayload
        }
        val response = api.updateContact(contactId, payload)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "Update contact failed: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
        val body = response.body()
        if (body != null) {
            db.withTransaction {
                val (contact, ctx) = body.toEntities()
                contactDao.upsertContacts(listOf(contact))
                ctx?.let { contactDao.upsertContexts(listOf(it)) }
            }
        } else {
            refreshContact(contactId)
        }
    }

    suspend fun addGroup(name: String, description: String, options: List<PromptOption>): GroupResponse {
        val request = AddGroupRequest(name = name, description = description, optionIds = options.map { it.id })
        val res = api.addGroup(request)
        if (!res.isSuccessful) {
            val body = res.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException("Add group failed: HTTP ${res.code()} ${res.message()} | body=$body")
        }
        val r = res.body() ?: error("Success but body null")
        // ★ 로컬 반영
        db.withTransaction {
            val (g, opts, refs) = r.toEntities()
            groupDao.upsertGroups(listOf(g))
            if (opts.isNotEmpty()) optionDao.upsertOptions(opts)
            optionDao.deleteCrossRefsByGroup(r.id)
            if (refs.isNotEmpty()) optionDao.upsertCrossRefs(refs)
        }
        return r
    }

    suspend fun deleteGroup(groupId: Long) {
        val res = api.deleteGroup(groupId)
        if (!res.isSuccessful) {
            throw IllegalStateException("Failed to delete group: HTTP ${res.code()} ${res.message()}")
        }
        // 로컬 반영 (crossRef는 FK onDelete에 따라 정리되지만, 안전하게 그룹만 지우기)
        groupDao.deleteById(groupId)
    }

    suspend fun updateGroup(
        groupId: Long,
        name: String? = null,
        description: String? = null,
        optionIds: List<Long>? = null
    ): GroupResponse {
        val payload = mutableMapOf<String, Any>()
        if (name != null) payload["name"] = name
        if (description != null) payload["description"] = description
        if (optionIds != null) payload["option_ids"] = optionIds
        require(payload.isNotEmpty()) { "updateGroup payload is empty" }

        val response = api.updateGroup(groupId, payload)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "Update group info failed: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
        val updated = response.body() ?: error("Success but body null")

        // 로컬 반영
        db.withTransaction {
            val (g, opts, refs) = updated.toEntities()
            groupDao.upsertGroups(listOf(g))
            optionDao.deleteCrossRefsByGroup(groupId)
            if (opts.isNotEmpty()) optionDao.upsertOptions(opts)
            if (refs.isNotEmpty()) optionDao.upsertCrossRefs(refs)
        }
        return updated
    }

    suspend fun addPromptOption(key: String, name: String, prompt: String): PromptOption {
        val req = PromptOptionRequest(key = key, name = name, prompt = prompt)
        val res = api.addPromptOption(req)
        if (!res.isSuccessful) {
            val body = res.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException("Add prompt option failed: HTTP ${res.code()} ${res.message()} | body=$body")
        }
        val r = res.body() ?: error("Success but body null")
        // 로컬 반영 (옵션 테이블만)
        optionDao.upsertOptions(listOf(r.toEntity()))
        return r
    }

    suspend fun getAllPromptOptions(): Pair<List<PromptOption>, List<PromptOption>> {
        val res = api.getAllPromptOptions()
        if (!res.isSuccessful) {
            throw IllegalStateException("Failed to get all prompt options: HTTP ${res.code()} ${res.message()}")
        }
        val all = res.body().orEmpty()
        // 로컬 반영
        optionDao.upsertOptions(all.map { it.toEntity() })

        val tone = all.filter { it.key == "tone" }
        val format = all.filter { it.key == "format" }
        return tone to format
    }
}

private fun ContactResponse.toEntities(): Pair<ContactEntity, ContactContextEntity?> {
    val contact = ContactEntity(
        id = id,
        // groupResponse가 있다면 그 id
        groupId = group?.id,
        name = name,
        email = email,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    val ctx = context?.let {
        ContactContextEntity(
            // 1:1 PK = contactId
            contactId = id,
            senderRole = it.senderRole,
            recipientRole = it.recipientRole,
            relationshipDetails = it.relationshipDetails,
            personalPrompt = it.personalPrompt,
            languagePreference = it.languagePreference,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt
        )
    }
    return contact to ctx
}

private fun PromptOption.toEntity(): PromptOptionEntity {
    return PromptOptionEntity(
        id = id,
        key = key,
        name = name,
        prompt = prompt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun GroupResponse.toEntities(): Triple<GroupEntity, List<PromptOptionEntity>, List<GroupPromptOptionCrossRef>> {
    val g = GroupEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    val optionEntities = (options ?: emptyList()).map { it.toEntity() }
    val refs = (options ?: emptyList()).map { opt ->
        GroupPromptOptionCrossRef(groupId = id, optionId = opt.id)
    }
    return Triple(g, optionEntities, refs)
}
