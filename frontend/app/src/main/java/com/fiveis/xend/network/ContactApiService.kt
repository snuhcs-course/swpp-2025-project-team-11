package com.fiveis.xend.network

import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.AddGroupRequest
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.GroupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactApiService {
    @POST("api/contact/")
    suspend fun addContact(@Body payload: AddContactRequest): Response<ContactResponse>

    @GET("api/contact/")
    suspend fun getAllContacts(): Response<List<ContactResponse>>

    // TODO
    @GET("api/contact/{id}/")
    suspend fun getContact(@Path("id") contactId: Int): Response<ContactResponse>

    @POST("api/contact/groups/")
    suspend fun addGroup(@Body payload: AddGroupRequest): Response<GroupResponse>

    // TODO
    @GET("api/contact/groups/")
    suspend fun getAllGroups(): Response<List<GroupResponse>>
}
