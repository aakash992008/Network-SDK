package com.aakash.solutions.networking.util

import android.util.Log
import com.fsn.networking.networking.BuildConfig
import com.aakash.solutions.networking.dto.ApiResponse
import com.aakash.solutions.networking.extensions.toJsonString

class NetworkLogger {
    companion object {
        private const val TAG = "nykaa.NETWORK_SDK"
        private val LOGGER_ENABLED by lazy {
            BuildConfig.DEBUGGABLE
        }

        fun e(exception: Throwable, message: String? = null) {
            if (LOGGER_ENABLED) {
                Log.e(TAG, "Exception->$exception \n Message->$message")
            }
        }

        fun e(message: String? = null) {
            if (LOGGER_ENABLED) {
                Log.e(TAG, " Message->$message")
            }
        }

        fun d(message: String?) {
            if (LOGGER_ENABLED && !message.isNullOrEmpty()) {
                Log.d(TAG, "Message-> $message")
            }
        }

        fun <T> apiResponse(response: ApiResponse<T>) {
            if (LOGGER_ENABLED) {
                when (response) {
                    is ApiResponse.Error -> {
                        Log.d(
                            TAG,
                            " \n Error Response $response \n Error Code->${response.errorCode} \n Error Message->${response.errorMessage} \n Error Exception ${response.throwable}"
                        )
                    }
                    is ApiResponse.Success -> {
                        Log.d(
                            TAG,
                            " \n Success Response -> $response \n  Response-> ${response.data.toJsonString()}"
                        )
                    }
                }
            }
        }
    }

}