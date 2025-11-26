package com.fiveis.xend.data.repository

import android.content.Context
import android.util.Log
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.asDomain
import com.fiveis.xend.data.database.asEntity
import com.fiveis.xend.data.model.ProfileData
import com.fiveis.xend.data.model.UpdateProfileRequest
import com.fiveis.xend.network.ProfileApiService
import com.fiveis.xend.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val context: Context) {

    private val apiService: ProfileApiService = RetrofitClient.getProfileApiService(context)
    private val profileDao = AppDatabase.getDatabase(context).profileDao()

    fun observeProfile(): Flow<ProfileData?> {
        return profileDao.observeProfile().map { entity ->
            entity?.asDomain()
        }
    }

    suspend fun getProfile(): ProfileResult {
        return try {
            val response = apiService.getProfile()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    profileDao.upsert(body.asEntity())
                    ProfileResult.Success(body)
                } else {
                    ProfileResult.Failure("프로필 정보가 없습니다")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                ProfileResult.Failure("서버 오류 (HTTP ${response.code()}): $errorBody")
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "프로필 조회 실패", e)
            ProfileResult.Failure("프로필 조회 실패: ${e.message}")
        }
    }

    suspend fun updateProfile(displayName: String?, info: String?, languagePreference: String?): ProfileResult {
        return try {
            val request = UpdateProfileRequest(
                displayName = displayName,
                info = info,
                languagePreference = languagePreference
            )
            val response = apiService.patchProfile(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    profileDao.upsert(body.asEntity())
                    ProfileResult.Success(body)
                } else {
                    ProfileResult.Failure("프로필 업데이트 응답이 없습니다")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                ProfileResult.Failure("서버 오류 (HTTP ${response.code()}): $errorBody")
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "프로필 업데이트 실패", e)
            ProfileResult.Failure("프로필 업데이트 실패: ${e.message}")
        }
    }
}

sealed class ProfileResult {
    data class Success(val data: ProfileData) : ProfileResult()
    data class Failure(val message: String) : ProfileResult()
}
