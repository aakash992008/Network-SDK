package com.aakash.solutions.networking.service

import androidx.annotation.Keep
import com.aakash.solutions.networking.adapter.GsonConvertor
import com.aakash.solutions.networking.dto.BaseResponse
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*


@Keep
interface ApiService {
    companion object {
        private const val GET = "GET"
        private const val POST = "POST"
        private const val PUT = "PUT"
    }

    @GsonConvertor
    @HTTP(method = GET, path = "{path}", hasBody = false)
    suspend fun performJsonGetCall(@Path("path") path: String): Response<JsonObject>

    @GsonConvertor
    @HTTP(method = GET, path = "{path}", hasBody = false)
    suspend fun <T> performGetCall(@Path("path") path: String): Response<BaseResponse<T>>

    @GsonConvertor
    @HTTP(method = GET, path = "{path}", hasBody = true)
    suspend fun performGetCall(
        @Path("path") path: String,
        @Body body: Any
    ): Response<JsonObject>

    @GsonConvertor
    @HTTP(method = POST, path = "{path}", hasBody = true)
    suspend fun performPostCall(
        @Path("path") path: String,
        @Body body: Any
    ): Response<JsonObject>

    @GsonConvertor
    @HTTP(method = PUT, path = "{path}", hasBody = true)
    suspend fun performPutCall(
        @Path("path") path: String,
        @Body body: Any
    ): Response<JsonObject>
}