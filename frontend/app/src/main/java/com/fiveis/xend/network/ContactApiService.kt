package com.fiveis.xend.network

import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.ContactResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactApiService {
    @POST("api/contact/")
    suspend fun addContact(@Body payload: AddContactRequest): Response<ContactResponse>

    // TODO
    @GET("api/contact/")
    suspend fun getAllContacts(): Response<List<ContactResponse>>

    // TODO
    @GET("api/contact/{id}/")
    suspend fun getContact(@Path("id") contactId: Long): Response<ContactResponse>
}
