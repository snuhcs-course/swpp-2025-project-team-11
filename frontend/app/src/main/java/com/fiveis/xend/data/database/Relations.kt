package com.fiveis.xend.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
/** Contact + Context 1:1 */
data class ContactWithContext(
    @Embedded val contact: ContactEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contactId"
    )
    val context: ContactContextEntity?
)

data class ContactWithGroupAndContext(
    @Embedded val contact: ContactEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contactId"
    )
    val context: ContactContextEntity?,

    // 연락처의 groupId → GroupEntity
    @Relation(
        parentColumn = "groupId",
        entityColumn = "id",
        entity = GroupEntity::class
    )
    val group: GroupEntity?

//    // 그룹 옵션
//    @Relation(
//        parentColumn = "groupId",
//        entityColumn = "id",
//        entity = PromptOptionEntity::class,
//        associateBy = Junction(
//            value = GroupPromptOptionCrossRef::class,
//            parentColumn = "groupId",
//            entityColumn = "optionId"
//        )
//    )
//    val groupOptions: List<PromptOptionEntity> = emptyList()
)

/** Group + (Members, Options) 1:N + 1:N */
data class GroupWithMembersAndOptions(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId",
        entity = ContactEntity::class
    )
    val members: List<ContactWithContext>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GroupPromptOptionCrossRef::class,
            parentColumn = "groupId",
            entityColumn = "optionId"
        )
    )
    val options: List<PromptOptionEntity>
)
