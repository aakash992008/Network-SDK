package com.aakash.solutions.networking

import androidx.annotation.VisibleForTesting
import com.aakash.solutions.networking.dto.ApiResponse
import com.aakash.solutions.networking.dto.BaseResponse
import com.aakash.solutions.networking.util.NetworkUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


@VisibleForTesting(otherwise = VisibleForTesting.NONE)
object NetworkingTestingUtil {

    fun <T> getResponse(jsonString: String): ApiResponse<T> {
        return NetworkUtil.getResponse(
            Gson().fromJson(
                jsonString, object : TypeToken<BaseResponse<T>>() {}.type
            )
        )
    }

    fun <T> getRawResponse(jsonString: String, responseType: Class<T>): ApiResponse<T> {
        return NetworkUtil.getResponse(Gson().fromJson(jsonString, responseType))
    }
}