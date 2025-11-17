package com.fiveis.xend.data.database

import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactContext
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.PromptOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {

    @Test
    fun groupWithMembersAndOptions_asDomain_maps_correctly() {
        val groupEntity = createMockGroupEntity(1L, "Team A")
        val contactEntities = listOf(
            ContactWithContext(
                contact = createMockContactEntity(1L, "John", "john@example.com", groupId = 1L),
                context = null
            ),
            ContactWithContext(
                contact = createMockContactEntity(2L, "Jane", "jane@example.com", groupId = 1L),
                context = null
            )
        )
        val optionEntities = listOf(
            createMockPromptOptionEntity(1L, "formal", "Formal")
        )

        val groupWithMembersAndOptions = GroupWithMembersAndOptions(
            group = groupEntity,
            members = contactEntities,
            options = optionEntities
        )

        val result = groupWithMembersAndOptions.asDomain()

        assertEquals(1L, result.id)
        assertEquals("Team A", result.name)
        assertEquals(2, result.members.size)
        assertEquals(1, result.options.size)
        assertEquals("John", result.members[0].name)
        assertEquals("Jane", result.members[1].name)
        assertEquals("Formal", result.options[0].name)
    }

    @Test
    fun groupWithMembersAndOptions_asDomain_sets_members_group_to_null_to_prevent_cycles() {
        val groupEntity = createMockGroupEntity(1L, "Team A")
        val contactEntity = ContactWithContext(
            contact = createMockContactEntity(1L, "John", "john@example.com", groupId = 1L),
            context = null
        )

        val groupWithMembersAndOptions = GroupWithMembersAndOptions(
            group = groupEntity,
            members = listOf(contactEntity),
            options = emptyList()
        )

        val result = groupWithMembersAndOptions.asDomain()

        assertNull(result.members[0].group)
    }

    @Test
    fun groupWithMembersAndOptions_asDomain_with_empty_members_and_options() {
        val groupEntity = createMockGroupEntity(1L, "Team A")

        val groupWithMembersAndOptions = GroupWithMembersAndOptions(
            group = groupEntity,
            members = emptyList(),
            options = emptyList()
        )

        val result = groupWithMembersAndOptions.asDomain()

        assertEquals(1L, result.id)
        assertTrue(result.members.isEmpty())
        assertTrue(result.options.isEmpty())
    }

    @Test
    fun contactWithContext_asDomain_maps_correctly_with_context() {
        val contactEntity = createMockContactEntity(1L, "John", "john@example.com")
        val contextEntity = createMockContactContextEntity(1L, "Manager", "Employee")

        val contactWithContext = ContactWithContext(
            contact = contactEntity,
            context = contextEntity
        )

        val group = createMockGroup(1L, "Team A")
        val result = contactWithContext.asDomain(group)

        assertEquals(1L, result.id)
        assertEquals("John", result.name)
        assertEquals("john@example.com", result.email)
        assertNotNull(result.context)
        assertEquals("Manager", result.context?.senderRole)
        assertNotNull(result.group)
        assertEquals("Team A", result.group?.name)
    }

    @Test
    fun contactWithContext_asDomain_maps_correctly_without_context() {
        val contactEntity = createMockContactEntity(1L, "John", "john@example.com")

        val contactWithContext = ContactWithContext(
            contact = contactEntity,
            context = null
        )

        val result = contactWithContext.asDomain(null)

        assertEquals(1L, result.id)
        assertEquals("John", result.name)
        assertNull(result.context)
        assertNull(result.group)
    }

    @Test
    fun contactWithGroupAndContext_asDomain_maps_correctly_with_group_and_context() {
        val contactEntity = createMockContactEntity(1L, "John", "john@example.com", groupId = 1L)
        val contextEntity = createMockContactContextEntity(1L, "Manager", "Employee")
        val groupEntity = createMockGroupEntity(1L, "Team A")

        val contactWithGroupAndContext = ContactWithGroupAndContext(
            contact = contactEntity,
            context = contextEntity,
            group = groupEntity
        )

        val result = contactWithGroupAndContext.asDomain()

        assertEquals(1L, result.id)
        assertEquals("John", result.name)
        assertNotNull(result.context)
        assertEquals("Manager", result.context?.senderRole)
        assertNotNull(result.group)
        assertEquals("Team A", result.group?.name)
        assertEquals(1L, result.group?.id)
    }

    @Test
    fun contactWithGroupAndContext_asDomain_maps_correctly_without_group() {
        val contactEntity = createMockContactEntity(1L, "John", "john@example.com", groupId = null)
        val contextEntity = createMockContactContextEntity(1L, "Manager", "Employee")

        val contactWithGroupAndContext = ContactWithGroupAndContext(
            contact = contactEntity,
            context = contextEntity,
            group = null
        )

        val result = contactWithGroupAndContext.asDomain()

        assertEquals(1L, result.id)
        assertNotNull(result.context)
        assertNull(result.group)
    }

    @Test
    fun contactWithGroupAndContext_asDomain_maps_correctly_without_context() {
        val contactEntity = createMockContactEntity(1L, "John", "john@example.com", groupId = 1L)
        val groupEntity = createMockGroupEntity(1L, "Team A")

        val contactWithGroupAndContext = ContactWithGroupAndContext(
            contact = contactEntity,
            context = null,
            group = groupEntity
        )

        val result = contactWithGroupAndContext.asDomain()

        assertEquals(1L, result.id)
        assertNull(result.context)
        assertNotNull(result.group)
    }

    @Test
    fun contactWithGroupAndContext_asDomain_group_has_empty_options_and_members() {
        val contactEntity = createMockContactEntity(1L, "John", "john@example.com", groupId = 1L)
        val groupEntity = createMockGroupEntity(1L, "Team A")

        val contactWithGroupAndContext = ContactWithGroupAndContext(
            contact = contactEntity,
            context = null,
            group = groupEntity
        )

        val result = contactWithGroupAndContext.asDomain()

        assertNotNull(result.group)
        assertTrue(result.group?.options?.isEmpty() ?: false)
        assertTrue(result.group?.members?.isEmpty() ?: false)
    }

    @Test
    fun promptOptionEntity_asDomain_maps_correctly() {
        val optionEntity = createMockPromptOptionEntity(1L, "formal", "Formal")

        val result = optionEntity.asDomain()

        assertEquals(1L, result.id)
        assertEquals("formal", result.key)
        assertEquals("Formal", result.name)
        assertEquals("Prompt for Formal", result.prompt)
        assertEquals("2025-01-01T00:00:00Z", result.createdAt)
        assertEquals("2025-01-01T00:00:00Z", result.updatedAt)
    }

    @Test
    fun promptOptionEntity_asDomain_with_null_timestamps() {
        val optionEntity = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language",
            createdAt = null,
            updatedAt = null
        )

        val result = optionEntity.asDomain()

        assertNull(result.createdAt)
        assertNull(result.updatedAt)
    }

    @Test
    fun contactContextEntity_asDomain_maps_correctly() {
        val contextEntity = createMockContactContextEntity(1L, "Manager", "Employee")

        val result = contextEntity.asDomain()

        assertEquals(1L, result.id)
        assertEquals("Manager", result.senderRole)
        assertEquals("Employee", result.recipientRole)
        assertEquals("Professional", result.relationshipDetails)
        assertEquals("Be formal", result.personalPrompt)
        assertEquals("en-US", result.languagePreference)
    }

    @Test
    fun contactContextEntity_asDomain_with_null_fields() {
        val contextEntity = ContactContextEntity(
            contactId = 1L,
            senderRole = null,
            recipientRole = null,
            relationshipDetails = null,
            personalPrompt = null,
            languagePreference = null,
            createdAt = null,
            updatedAt = null
        )

        val result = contextEntity.asDomain()

        assertEquals(1L, result.id)
        assertNull(result.senderRole)
        assertNull(result.recipientRole)
        assertNull(result.relationshipDetails)
        assertNull(result.personalPrompt)
        assertNull(result.languagePreference)
    }

    @Test
    fun group_asEntity_maps_correctly() {
        val group = createMockGroup(1L, "Team A", "Description")

        val result = group.asEntity()

        assertEquals(1L, result.id)
        assertEquals("Team A", result.name)
        assertEquals("Description", result.description)
        assertEquals("2025-01-01T00:00:00Z", result.createdAt)
        assertEquals("2025-01-01T00:00:00Z", result.updatedAt)
    }

    @Test
    fun group_asEntity_with_null_description() {
        val group = Group(
            id = 1L,
            name = "Team A",
            description = null,
            options = emptyList(),
            members = emptyList(),
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val result = group.asEntity()

        assertNull(result.description)
    }

    @Test
    fun group_asEntity_ignores_options_and_members() {
        val options = listOf(createMockPromptOption(1L, "formal", "Formal"))
        val members = listOf(createMockContact(1L, "John", "john@example.com"))

        val group = Group(
            id = 1L,
            name = "Team A",
            description = "Description",
            options = options,
            members = members,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val result = group.asEntity()

        assertEquals(1L, result.id)
        assertEquals("Team A", result.name)
    }

    @Test
    fun contact_asEntity_maps_correctly_with_group() {
        val group = createMockGroup(1L, "Team A")
        val contact = Contact(
            id = 1L,
            group = group,
            name = "John",
            email = "john@example.com",
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val result = contact.asEntity()

        assertEquals(1L, result.id)
        assertEquals(1L, result.groupId)
        assertEquals("John", result.name)
        assertEquals("john@example.com", result.email)
    }

    @Test
    fun contact_asEntity_maps_correctly_without_group() {
        val contact = Contact(
            id = 1L,
            group = null,
            name = "John",
            email = "john@example.com",
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val result = contact.asEntity()

        assertEquals(1L, result.id)
        assertNull(result.groupId)
    }

    @Test
    fun contact_asEntity_ignores_context() {
        val context = createMockContactContext(1L, "Manager", "Employee")
        val contact = Contact(
            id = 1L,
            group = null,
            name = "John",
            email = "john@example.com",
            context = context,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val result = contact.asEntity()

        assertEquals(1L, result.id)
        assertEquals("John", result.name)
    }

    @Test
    fun contactContext_asEntity_maps_correctly() {
        val context = createMockContactContext(1L, "Manager", "Employee")

        val result = context.asEntity(1L)

        assertEquals(1L, result.contactId)
        assertEquals("Manager", result.senderRole)
        assertEquals("Employee", result.recipientRole)
        assertEquals("Professional", result.relationshipDetails)
        assertEquals("Be formal", result.personalPrompt)
        assertEquals("en-US", result.languagePreference)
    }

    @Test
    fun contactContext_asEntity_with_null_fields() {
        val context = ContactContext(
            id = 99L,
            senderRole = null,
            recipientRole = null,
            relationshipDetails = null,
            personalPrompt = null,
            languagePreference = null,
            createdAt = null,
            updatedAt = null
        )

        val result = context.asEntity(1L)

        assertEquals(1L, result.contactId)
        assertNull(result.senderRole)
        assertNull(result.recipientRole)
    }

    @Test
    fun contactContext_asEntity_uses_provided_contactId_not_context_id() {
        val context = ContactContext(
            id = 99L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Professional",
            personalPrompt = "Be formal",
            languagePreference = "en-US",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val result = context.asEntity(42L)

        assertEquals(42L, result.contactId)
    }

    @Test
    fun promptOption_asEntity_maps_correctly() {
        val option = createMockPromptOption(1L, "formal", "Formal")

        val result = option.asEntity()

        assertEquals(1L, result.id)
        assertEquals("formal", result.key)
        assertEquals("Formal", result.name)
        assertEquals("Prompt for Formal", result.prompt)
    }

    @Test
    fun promptOption_asEntity_with_null_timestamps() {
        val option = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language",
            createdAt = null,
            updatedAt = null
        )

        val result = option.asEntity()

        assertNull(result.createdAt)
        assertNull(result.updatedAt)
    }

    @Test
    fun roundtrip_group_domain_to_entity_to_domain() {
        val originalGroup = createMockGroup(1L, "Team A", "Description")

        val entity = originalGroup.asEntity()
        val groupWithMembersAndOptions = GroupWithMembersAndOptions(
            group = entity,
            members = emptyList(),
            options = emptyList()
        )
        val result = groupWithMembersAndOptions.asDomain()

        assertEquals(originalGroup.id, result.id)
        assertEquals(originalGroup.name, result.name)
        assertEquals(originalGroup.description, result.description)
    }

    @Test
    fun roundtrip_contact_domain_to_entity_to_domain() {
        val originalContact = createMockContact(1L, "John", "john@example.com")

        val entity = originalContact.asEntity()
        val contactWithContext = ContactWithContext(
            contact = entity,
            context = null
        )
        val result = contactWithContext.asDomain(null)

        assertEquals(originalContact.id, result.id)
        assertEquals(originalContact.name, result.name)
        assertEquals(originalContact.email, result.email)
    }

    @Test
    fun roundtrip_promptOption_domain_to_entity_to_domain() {
        val originalOption = createMockPromptOption(1L, "formal", "Formal")

        val entity = originalOption.asEntity()
        val result = entity.asDomain()

        assertEquals(originalOption.id, result.id)
        assertEquals(originalOption.key, result.key)
        assertEquals(originalOption.name, result.name)
        assertEquals(originalOption.prompt, result.prompt)
    }

    @Test
    fun roundtrip_contactContext_domain_to_entity_to_domain() {
        val originalContext = createMockContactContext(1L, "Manager", "Employee")

        val entity = originalContext.asEntity(1L)
        val result = entity.asDomain()

        assertEquals(originalContext.senderRole, result.senderRole)
        assertEquals(originalContext.recipientRole, result.recipientRole)
        assertEquals(originalContext.relationshipDetails, result.relationshipDetails)
    }

    @Test
    fun mapping_handles_special_characters_in_strings() {
        val group = Group(
            id = 1L,
            name = "Team \"A\" & B's",
            description = "Description with !@#$%^&*()",
            options = emptyList(),
            members = emptyList(),
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val entity = group.asEntity()
        val groupWithMembersAndOptions = GroupWithMembersAndOptions(
            group = entity,
            members = emptyList(),
            options = emptyList()
        )
        val result = groupWithMembersAndOptions.asDomain()

        assertEquals("Team \"A\" & B's", result.name)
        assertEquals("Description with !@#$%^&*()", result.description)
    }

    @Test
    fun mapping_handles_unicode_characters() {
        val contact = Contact(
            id = 1L,
            group = null,
            name = "李明",
            email = "李明@例え.com",
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val entity = contact.asEntity()
        val contactWithContext = ContactWithContext(
            contact = entity,
            context = null
        )
        val result = contactWithContext.asDomain(null)

        assertEquals("李明", result.name)
        assertEquals("李明@例え.com", result.email)
    }

    private fun createMockGroupEntity(
        id: Long,
        name: String,
        description: String = "Description for $name"
    ) = GroupEntity(
        id = id,
        name = name,
        description = description,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockContactEntity(
        id: Long,
        name: String,
        email: String,
        groupId: Long? = null
    ) = ContactEntity(
        id = id,
        groupId = groupId,
        name = name,
        email = email,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockContactContextEntity(
        contactId: Long,
        senderRole: String,
        recipientRole: String
    ) = ContactContextEntity(
        contactId = contactId,
        senderRole = senderRole,
        recipientRole = recipientRole,
        relationshipDetails = "Professional",
        personalPrompt = "Be formal",
        languagePreference = "en-US",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockPromptOptionEntity(
        id: Long,
        key: String,
        name: String
    ) = PromptOptionEntity(
        id = id,
        key = key,
        name = name,
        prompt = "Prompt for $name",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockGroup(
        id: Long,
        name: String,
        description: String = "Description for $name"
    ) = Group(
        id = id,
        name = name,
        description = description,
        options = emptyList(),
        members = emptyList(),
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockContact(
        id: Long,
        name: String,
        email: String
    ) = Contact(
        id = id,
        group = null,
        name = name,
        email = email,
        context = null,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockContactContext(
        id: Long,
        senderRole: String,
        recipientRole: String
    ) = ContactContext(
        id = id,
        senderRole = senderRole,
        recipientRole = recipientRole,
        relationshipDetails = "Professional",
        personalPrompt = "Be formal",
        languagePreference = "en-US",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockPromptOption(
        id: Long,
        key: String,
        name: String
    ) = PromptOption(
        id = id,
        key = key,
        name = name,
        prompt = "Prompt for $name",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )
}
