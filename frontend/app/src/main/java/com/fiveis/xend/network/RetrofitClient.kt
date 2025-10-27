package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.source.AuthApiService
import com.fiveis.xend.data.source.TokenManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val SERVER_BASE_URL = BuildConfig.BASE_URL

    // HTTP 통신 로그를 확인하기 위한 로깅 인터셉터
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient 설정
    fun getClient(context: Context): OkHttpClient {
        val tokenManager = TokenManager(context)
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val accessToken = tokenManager.getAccessToken()
                val request = if (accessToken != null) {
                    chain.request().newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .authenticator(TokenRefreshAuthenticator(context, tokenManager))
            .build()
    }

    // Retrofit 인스턴스 생성
    private fun getRetrofit(client: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls() // null 값도 처리?
            .create()

        return Retrofit.Builder()
            .baseUrl(SERVER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // AuthApiService 인터페이스의 구현체 생성
    val authApiService: AuthApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        getRetrofit(client).create(AuthApiService::class.java)
    }

    // [추가] MailApiService 인터페이스의 구현체 추가
    fun getMailApiService(context: Context): MailApiService {
        return getRetrofit(getClient(context)).create(MailApiService::class.java)
    }

    fun getContactApiService(context: Context): ContactApiService {
        return getRetrofit(getClient(context)).create(ContactApiService::class.java)
    }
}
