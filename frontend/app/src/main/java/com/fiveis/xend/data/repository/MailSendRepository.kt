package com.fiveis.xend.data.repository

import com.fiveis.xend.data.model.SendResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MailSendRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonMT = "application/json".toMediaType()

    suspend fun sendEmail(
        endpointUrl: String,
        to: String,
        subject: String,
        body: String,
        accessToken: String?
    ): SendResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("to", to)
            put("subject", subject)
            put("body", body)
        }
        val json = payload.toString()

        val req = Request.Builder()
            .url(endpointUrl)
            .post(json.toRequestBody(jsonMT))
            .apply {
                if (!accessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $accessToken")
                }
            }
            .build()

        client.newCall(req).execute().use { resp ->
            val respText = resp.body?.string().orEmpty()
            if (resp.code == 201 && resp.isSuccessful) {
                val obj = JSONObject(respText)
                val ids = obj.optJSONArray("labelIds")?.let { arr ->
                    List(arr.length()) { i -> arr.getString(i) }
                } ?: emptyList()
                return@use SendResponse(
                    id = obj.getString("id"),
                    threadId = obj.optString("threadId"),
                    labelIds = ids
                )
            } else {
                throw IllegalStateException(
                    "Send failed: HTTP ${resp.code} ${resp.message} | body=${respText.take(500)}"
                )
            }
        }
    }
}
