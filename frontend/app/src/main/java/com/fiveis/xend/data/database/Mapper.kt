package com.fiveis.xend.data.database

import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactContext
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption

/**
 * --- Entity -> Domain ---
 */

fun GroupWithMembersAndOptions.asDomain(): Group = Group(
    id = group.id,
    name = group.name,
    description = group.description,
    options = options.map { it.asDomain() },
    // 그룹 안에서 순환 막기 위해 group = null
    members = members.map { it.asDomain(null) },
    createdAt = group.createdAt,
    updatedAt = group.updatedAt
)

fun ContactWithContext.asDomain(group: Group?): Contact = Contact(
    id = contact.id,
    // 필요 시 null로? (순환 방지)
    group = group,
    name = contact.name,
    email = contact.email,
    context = context?.asDomain(),
    createdAt = contact.createdAt,
    updatedAt = contact.updatedAt
)

fun PromptOptionEntity.asDomain(): PromptOption = PromptOption(
    id = id,
    key = key,
    name = name,
    prompt = prompt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContactContextEntity.asDomain(): ContactContext = ContactContext(
    // PK = contact id를 context id로
    id = contactId,
    senderRole = senderRole,
    recipientRole = recipientRole,
    relationshipDetails = relationshipDetails,
    personalPrompt = personalPrompt,
    languagePreference = languagePreference,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * --- Domain -> Entity ---
 */

fun Group.asEntity(): GroupEntity = GroupEntity(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Contact.asEntity(): ContactEntity = ContactEntity(
    id = id,
    groupId = group?.id,
    name = name,
    email = email,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContactContext.asEntity(contactId: Long): ContactContextEntity = ContactContextEntity(
    contactId = contactId,
    senderRole = senderRole,
    recipientRole = recipientRole,
    relationshipDetails = relationshipDetails,
    personalPrompt = personalPrompt,
    languagePreference = languagePreference,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PromptOption.asEntity(): PromptOptionEntity = PromptOptionEntity(
    id = id,
    key = key,
    name = name,
    prompt = prompt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
