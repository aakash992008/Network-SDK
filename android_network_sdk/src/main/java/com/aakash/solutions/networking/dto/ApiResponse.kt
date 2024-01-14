package com.aakash.solutions.networking.dto

import com.google.gson.JsonObject

/**
 * Created By
 * Aakash Tripathi
 * March 2022
 */
sealed class ApiResponse<out T> {
    /**
     * ApiResponse is success
     * @param data T Generic response for the data requested (Non Nullable)
     * @param successMessage api success response
     */
    data class Success<T>(val data: T, val successMessage: String? = null) : ApiResponse<T>()

    /**
     * ApiResponse is error
     * @param errorCode Api error code
     * @param errorMessage api error message
     * @param errorResponse Json representation of error
     * @param throwable exception if any parsing error occurs
     */
    data class Error(
        val errorCode: String,
        val errorMessage: String,
        val errorResponse: JsonObject? = null,
        val throwable: Throwable? = null
    ) : ApiResponse<Nothing>()
}

