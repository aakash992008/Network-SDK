package com.aakash.solutions.networking.util

import androidx.annotation.Keep

@Keep
object NetworkingConstant {

    const val MESSAGE = "message"
    const val RESPONSE = "response"
    const val SUCCESS = "success"
    const val DATA = "data"
    const val STATUS = "status"
    const val ERROR = "error"
    const val CODE = "code"
    const val UNKNOWN_ERROR_MESSAGE = "Something went wrong"
    const val INTERNET_UNAVAILABLE = "Internet Unavailable"
    const val API_SUCCESS_STATUS = "success"
    const val API_FAIL_STATUS = "fail"
    const val API_RESPONSE_TYPE = "type"

    const val ERROR_IMAGE_URL = "image_url"
    const val ERROR_TITLE = "title"
    const val ERROR_WAIT_TIME_IN_SECONDS = "wait_time_in_seconds"
    const val FORCE_UPGRADE_ERROR_CODE = 5001
    const val SOFT_UPGRADE_ERROR_CODE = 5000
    const val AUTHENTICATION_ERROR = 1003
    const val API_PRIORITIZATION_ERROR_CODE = 6001
    const val API_PRIORITIZATION_FORCE_ERROR_CODE = 6002
    const val CACHE_DATA = "cache_data"


    const val CACHE = "Cache-Control"
    const val RESPONSE_SUCCESS = 200
    const val RESPONSE_BAD_REQUEST = 400
    const val RESPONSE_UNAUTHORISED = 401
    const val RESPONSE_FORBIDDEN = 403
    const val RESPONSE_NOT_FOUND = 404
    const val RESPONSE_INTERNAL_SERVER_ERROR = 500


    const val NO_INTERNET_CONNECTION_CODE = "1009"
    const val API_BLOCKED = "1010"
    const val UNKNOWN_ERROR_CODE = "1000"
    const val JSON_EXCEPTION_ERROR_CODE = "9001"
    const val TIMEOUT_ERROR_CODE = "9002"
}