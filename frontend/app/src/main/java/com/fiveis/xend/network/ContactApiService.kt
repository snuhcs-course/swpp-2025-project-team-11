package com.fiveis.xend.network

import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.AddGroupRequest
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.model.PromptOptionRequest
import com.fiveis.xend.data.model.PromptOptionUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactApiService {
    // 연락처 관련
    @POST("api/contact/")
    suspend fun addContact(@Body payload: AddContactRequest): Response<ContactResponse>

    @GET("api/contact/")
    suspend fun getAllContacts(): Response<List<ContactResponse>>

    @GET("api/contact/{id}/")
    suspend fun getContact(@Path("id") contactId: Long): Response<ContactResponse>

    @DELETE("api/contact/{id}/")
    suspend fun deleteContact(@Path("id") contactId: Long): Response<Void>

    @PATCH("api/contact/{id}/")
    suspend fun updateContact(
        @Path("id") contactId: Long,
        @Body payload: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ContactResponse>

    // 그룹 관련
    @POST("api/contact/groups/")
    suspend fun addGroup(@Body payload: AddGroupRequest): Response<GroupResponse>

    @GET("api/contact/groups/")
    suspend fun getAllGroups(): Response<List<GroupResponse>>

    @GET("api/contact/groups/{id}/")
    suspend fun getGroup(@Path("id") groupId: Long): Response<GroupResponse>

    @DELETE("api/contact/groups/{id}/")
    suspend fun deleteGroup(@Path("id") groupId: Long): Response<Void>

    @PATCH("api/contact/groups/{id}/")
    suspend fun updateGroup(
        @Path("id") groupId: Long,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Response<GroupResponse>

    @POST("api/contact/prompt-options/")
    suspend fun addPromptOption(@Body payload: PromptOptionRequest): Response<PromptOption>

    @GET("api/contact/prompt-options/")
    suspend fun getAllPromptOptions(): Response<List<PromptOption>>

    @PATCH("api/contact/prompt-options/{id}/")
    suspend fun updatePromptOption(
        @Path("id") optionId: Long,
        @Body payload: PromptOptionUpdateRequest
    ): Response<PromptOption>

    @DELETE("api/contact/prompt-options/{id}/")
    suspend fun deletePromptOption(@Path("id") optionId: Long): Response<Void>
}
