package com.fiveis.xend.network

import com.fiveis.xend.data.model.ProfileData
import com.fiveis.xend.data.model.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

interface ProfileApiService {
    @GET("api/user/me/profile/")
    suspend fun getProfile(): Response<ProfileData>

    @PUT("api/user/me/profile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileData>

    @PATCH("api/user/me/profile/")
    suspend fun patchProfile(@Body request: UpdateProfileRequest): Response<ProfileData>
}
