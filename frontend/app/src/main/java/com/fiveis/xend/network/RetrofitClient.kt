package com.fiveis.xend.network

import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.source.AuthApiService
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
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit 인스턴스 생성
    private val retrofit = Retrofit.Builder()
        .baseUrl(SERVER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // AuthApiService 인터페이스의 구현체 생성
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)

    // [추가] MailApiService 인터페이스의 구현체 추가
    val mailApiService: MailApiService = retrofit.create(MailApiService::class.java)
}
