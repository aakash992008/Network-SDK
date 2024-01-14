package com.aakash.solutions.networking.dto

import androidx.annotation.Keep
import androidx.core.text.isDigitsOnly
import com.aakash.solutions.networking.util.NetworkingConstant
import com.google.gson.annotations.SerializedName

/**
 * Created By
 * Aakash Tripathi
 * March 2022
 */

@Keep
open class BaseResponse<T>(
    private val status: String?,
    @SerializedName(NetworkingConstant.SUCCESS)
    private val success: Boolean?,
    @SerializedName(NetworkingConstant.RESPONSE)
    val response: T?,
    @SerializedName(NetworkingConstant.DATA)
    private val dataResponse: T?,
    @SerializedName(NetworkingConstant.MESSAGE)
    protected val message: String?,
    @SerializedName(NetworkingConstant.ERROR)
    protected val errorCode: Int?,
    @SerializedName(NetworkingConstant.API_RESPONSE_TYPE)
    val responseType: String?,
    @SerializedName(NetworkingConstant.CODE)
    private val code: Any?
) {

    constructor() : this("", null, null, null, null, null, null, null)

    open fun isResponseSuccess(): Boolean {
        return if (status == NetworkingConstant.API_SUCCESS_STATUS || success == true || status == "1") {
            response != null || dataResponse != null
        } else {
            false
        }
    }

    /**
     * Used for cases when response is null but API is success
     */
    open fun isApiSuccessWithResponseDataNull(): Boolean {
        return if (status == NetworkingConstant.API_SUCCESS_STATUS || success == true || status == "1") {
            return true
        } else {
            false
        }
    }

    open fun getResponseData(): T? = response ?: dataResponse

    open fun getResponseMessage(): String = if (message.isNullOrEmpty()) {
        if (response is String) {
            response
        } else {
            ""
        }
    } else {
        message
    }

    open fun getResponseErrorCode(): Int = errorCode
        ?: if (code is String && code.isDigitsOnly()) {
            code.toInt()
        } else {
            NetworkingConstant.UNKNOWN_ERROR_CODE.toInt()
        }

}
