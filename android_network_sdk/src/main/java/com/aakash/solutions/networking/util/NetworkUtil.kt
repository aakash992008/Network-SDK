package com.aakash.solutions.networking.util

import androidx.core.text.isDigitsOnly
import com.aakash.solutions.networking.Networking
import com.aakash.solutions.networking.dto.ApiResponse
import com.aakash.solutions.networking.dto.BaseResponse
import com.aakash.solutions.networking.extensions.performApiCall
import com.aakash.solutions.networking.extensions.performRawApiCall
import com.aakash.solutions.networking.extensions.toJsonObject
import com.aakash.solutions.networking.service.ResponseListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSyntaxException
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException


/**
 * Created By
 * Aakash Tripathi
 * March 2022
 */
object NetworkUtil {


    fun getErrorResponse(
        errorBody: String,
        apiErrorCode: String = NetworkingConstant.UNKNOWN_ERROR_CODE,
        throwable: Throwable? = null,
    ): ApiResponse.Error {
        try {
            if (errorBody.trim().isNotEmpty()) {
                return try {
                    val errorJson = errorBody.toJsonObject()
                    errorJson?.let {
                        val errorMessage = when {
                            errorJson.has(NetworkingConstant.MESSAGE) -> {
                                getStringMessage(errorJson, NetworkingConstant.MESSAGE)
                            }
                            errorJson.has(NetworkingConstant.RESPONSE) -> {
                                getStringMessage(errorJson, NetworkingConstant.RESPONSE)
                            }
                            else -> {
                                NetworkingConstant.UNKNOWN_ERROR_MESSAGE
                            }
                        }
                        val errorCode = if (isPremitiveKeyPresent(errorJson, "code")) {
                            getStringMessage(
                                errorJson,
                                "code",
                                NetworkingConstant.UNKNOWN_ERROR_MESSAGE
                            )
                        } else {
                            getStringMessage(errorJson, NetworkingConstant.ERROR, apiErrorCode)
                        }
                        ApiResponse.Error(
                            errorCode,
                            errorMessage,
                            errorJson, throwable,
                        )
                    } ?: kotlin.run {
                        ApiResponse.Error(
                            apiErrorCode,
                            errorBody,
                            errorJson, throwable
                        )
                    }
                } catch (e: Exception) {
                    ApiResponse.Error(
                        NetworkingConstant.UNKNOWN_ERROR_CODE,
                        NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                        throwable = throwable
                    )
                }
            } else {
                return ApiResponse.Error(
                    apiErrorCode,
                    NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                    throwable = throwable
                )
            }
        } catch (e: Exception) {
            return ApiResponse.Error(
                apiErrorCode,
                NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                throwable = throwable
            )
        }
    }

    fun getStringMessage(
        errorJson: JsonObject,
        key: String,
        defaultMessage: String = NetworkingConstant.UNKNOWN_ERROR_MESSAGE
    ): String = if (errorJson.has(key)) {
        if (errorJson.get(key).isJsonPrimitive) {
            val element = errorJson.get(key) as JsonPrimitive
            if (element.isString) {
                element.asString
            } else {
                element.toString()
            }
        } else {
            defaultMessage
        }
    } else {
        defaultMessage
    }

    private fun isPremitiveKeyPresent(
        errorJson: JsonObject,
        key: String
    ) = errorJson.has(key) && errorJson.get(key).isJsonPrimitive

    fun getIntMessage(
        errorJson: JsonObject,
        key: String,
        defaultMessage: Int = NetworkingConstant.UNKNOWN_ERROR_CODE.toInt()
    ): Int = if (errorJson.has(key)) {
        if (errorJson.get(key).isJsonPrimitive) {
            val element = errorJson.get(key) as JsonPrimitive
            if (element.isNumber) {
                element.asInt
            } else if (element.isString && element.asString.isDigitsOnly()) {
                element.asString.toInt()
            } else {
                defaultMessage
            }
        } else {
            defaultMessage
        }
    } else {
        defaultMessage
    }

    private fun getErrorResponse(errorThrow: Throwable): ApiResponse.Error {
        NetworkLogger.e(errorThrow)
        Networking.logError(errorThrow, getCallingFunctionFromStackTrace())
        return when (errorThrow) {
            is HttpException -> {
                getErrorResponse(
                    errorThrow.response()?.errorBody()?.string()
                        ?: NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                    "" + errorThrow.response()?.code(), errorThrow
                )
            }
            is SocketTimeoutException -> {
                getErrorResponse(
                    NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                    NetworkingConstant.TIMEOUT_ERROR_CODE, errorThrow
                )
            }
            is IllegalStateException, is JsonSyntaxException -> {
                Networking.logErrorOnChucker("JSON EXCEPTION", errorThrow)
                //JSON Exception
                ApiResponse.Error(
                    NetworkingConstant.JSON_EXCEPTION_ERROR_CODE,
                    NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                    throwable = errorThrow
                )
            }
            else -> {
                Networking.logErrorOnChucker("EXCEPTION", errorThrow)
                ApiResponse.Error(
                    NetworkingConstant.UNKNOWN_ERROR_CODE,
                    NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                    throwable = errorThrow
                )
            }
        }
    }

    fun getJsonObjectFromGson(json: String): JsonObject? = try {
        Gson().fromJson(json, JsonObject::class.java)
    } catch (e: Exception) {
        Networking.logErrorOnChucker("getJsonObjectFromJson() Method" + e.message)
        null
    }

    fun getJsonObjectFromJson(response: retrofit2.Response<*>): JsonObject? = try {
        if (response.isSuccessful) {
            Gson().fromJson(Gson().toJson(response.body()), JsonObject::class.java)
        } else {
            Gson().fromJson(response.errorBody()?.string() ?: "", JsonObject::class.java)
        }
    } catch (e: Exception) {
        Networking.logErrorOnChucker("getJsonObjectFromJson() Method" + e.message)
        null
    }

    /**
     * Get API Response Wrapped in ApiResponse<>
     * @param response Response from server
     */
    fun <Response> getResponse(
        response: BaseResponse<Response>?
    ): ApiResponse<Response> = response?.let { body ->
        if (body.isResponseSuccess()) {
            body.getResponseData()?.let {
                ApiResponse.Success(it, body.getResponseMessage())
            } ?: kotlin.run {
                ApiResponse.Error(
                    "${body.getResponseErrorCode()}",
                    body.getResponseMessage(),
                    response.toJsonObject()
                )
            }
        } else {
            ApiResponse.Error(
                "${body.getResponseErrorCode()}",
                body.getResponseMessage(),
                response.toJsonObject()
            )
        }
    } ?: kotlin.run {
        ApiResponse.Error(
            NetworkingConstant.UNKNOWN_ERROR_CODE,
            NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
            null
        )
    }

    fun <Response> getResponse(response: Response?): ApiResponse<Response> = response?.let {
        ApiResponse.Success(it)
    } ?: kotlin.run {
        ApiResponse.Error(
            NetworkingConstant.UNKNOWN_ERROR_CODE,
            NetworkingConstant.UNKNOWN_ERROR_MESSAGE
        )
    }

    fun <Response, Base : BaseResponse<Response>> getBooleanResponse(response: Base): ApiResponse<Boolean> =
        try {
            if (response.isApiSuccessWithResponseDataNull()) {
                ApiResponse.Success(true, response.getResponseMessage())
            } else {
                ApiResponse.Error(
                    "${response.getResponseErrorCode()}",
                    response.getResponseMessage(),
                    response.toJsonObject()
                )
            }
        } catch (e: Exception) {
            Networking.logErrorOnChucker(
                "retrofit2.Response<BaseResponse<T>>.getBooleanResponse(): Response<T>",
                e
            )
            ApiResponse.Error(
                NetworkingConstant.UNKNOWN_ERROR_CODE,
                NetworkingConstant.UNKNOWN_ERROR_MESSAGE,
                null
            )
        }

    internal fun getDefaultResponseIfFireStoreBlockedApi(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("status", "fail")
            jsonObject.put("type", "none")
            jsonObject.put("title", "")
            jsonObject.put("message", "apiPrioritizationModel.getErrorMessage()")
            jsonObject.put("image_url", "apiPrioritizationModel.getErrorImageUrl()")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return jsonObject
    }


    /**
     * Get API Error response from exceptions
     * @param exception API exception
     */
    fun getError(exception: Throwable): ApiResponse.Error = getErrorResponse(exception)

    /**
     * To perform API calls using RX
     * @param observable RX service function
     * @param responseListener callback for success and error
     */
    fun <Response, Base : BaseResponse<Response>> performApiCall(
        observable: Observable<Base>,
        responseListener: ResponseListener<Response>, observeOnMainThread: Boolean = false
    ): Disposable = observable.performApiCall({
        when (it) {
            is ApiResponse.Error -> {
                responseListener.onErrorResponse(it)
            }
            is ApiResponse.Success -> {
                responseListener.onSuccessResponse(it)
            }
        }
    }, observeOnMainThread)


    /**
     * To perform API calls using RX
     * @param observable RX service function
     * @param responseListener callback for success and error
     */
    fun <Response> performRawApiCall(
        observable: Observable<Response>,
        responseListener: ResponseListener<Response>
    ): Disposable = observable.performRawApiCall {
        when (it) {
            is ApiResponse.Error -> {
                responseListener.onErrorResponse(it)
            }
            is ApiResponse.Success -> {
                responseListener.onSuccessResponse(it)
            }
        }
    }

    fun getCallingFunctionFromStackTrace(): String {
        val stackTrace = Thread.currentThread().stackTrace.asList()
        var string = "NOT FOUND"
        var networkModuleCallGot = false;
        for (element in stackTrace) {
            if (element.className.contains("com.fsn.nykaa.nykaa_networking")) {
                networkModuleCallGot = true
            } else if (networkModuleCallGot) {
                return element.className
            }
        }
        return string
    }
}