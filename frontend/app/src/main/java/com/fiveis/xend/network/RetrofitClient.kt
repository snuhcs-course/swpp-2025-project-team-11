package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.source.AuthApiService
import com.fiveis.xend.data.source.TokenManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import okhttp3.Dns
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

    // Google DNS(8.8.8.8)를 대체 DNS로 사용하는 커스텀 DNS 리졸버
    private val customDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                // 기본적으로 시스템 DNS 사용
                Dns.SYSTEM.lookup(hostname)
            } catch (e: UnknownHostException) {
                // 시스템 DNS 실패 시, Google Public DNS를 통해 직접 조회
                try {
                    val client = OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("https://dns.google/resolve?name=$hostname&type=A")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        throw UnknownHostException("Google DNS lookup failed")
                    }

                    // JSON 응답 파싱 (Gson 라이브러리 필요)
                    val gson = com.google.gson.Gson()
                    val result = gson.fromJson(body, DnsResponse::class.java)

                    val addresses = result.answer
                        ?.mapNotNull { InetAddress.getByName(it.data) }
                        ?: throw UnknownHostException("No address found in Google DNS response")

                    if (addresses.isEmpty()) {
                        throw UnknownHostException("No address found for $hostname")
                    }
                    addresses
                } catch (e2: Exception) {
                    e.addSuppressed(e2)
                    throw e
                }
            }
        }
    }

    // Google DNS 응답 파싱을 위한 데이터 클래스
    private data class DnsResponse(val answer: List<DnsAnswer>?)
    private data class DnsAnswer(val data: String)

    // OkHttpClient 설정
    fun getClient(context: Context): OkHttpClient {
        val tokenManager = TokenManager(context)
        return OkHttpClient.Builder()
            .dns(customDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->

                val accessToken = tokenManager.getAccessToken()
                val request = if (accessToken != null) {
                    val fullToken = "Bearer $accessToken"
                    android.util.Log.d("RetrofitClient", "Authorization header added: $fullToken")
                    chain.request().newBuilder().addHeader("Authorization", fullToken).build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .authenticator(TokenRefreshAuthenticator(context, tokenManager))
            .build()
    }

    // WebSocket 전용 OkHttpClient (Bearer 토큰을 수동으로 추가)
    fun getWebSocketClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(customDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            // WebSocket은 수동으로 Authorization 헤더 추가하므로 인터셉터 없음
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

    // AuthApiService 인터페이스의 구현체 생성 (인증 없이 사용)
    val authApiService: AuthApiService by lazy {
        val client = OkHttpClient.Builder()
            .dns(customDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
        getRetrofit(client).create(AuthApiService::class.java)
    }

    // AuthApiService 인터페이스의 구현체 생성 (인증 포함)
    fun getAuthApiService(context: Context): AuthApiService {
        return getRetrofit(getClient(context)).create(AuthApiService::class.java)
    }

    // [추가] MailApiService 인터페이스의 구현체 추가
    fun getMailApiService(context: Context): MailApiService {
        return getRetrofit(getClient(context)).create(MailApiService::class.java)
    }

    fun getContactApiService(context: Context): ContactApiService {
        return getRetrofit(getClient(context)).create(ContactApiService::class.java)
    }

    fun getProfileApiService(context: Context): ProfileApiService {
        return getRetrofit(getClient(context)).create(ProfileApiService::class.java)
    }
}
