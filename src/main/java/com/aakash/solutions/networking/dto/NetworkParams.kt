package com.aakash.solutions.networking.dto

import androidx.annotation.Keep
import com.aakash.solutions.networking.service.BaseInterceptor
import com.aakash.solutions.networking.service.NetworkErrorListener

/**
 * Created By Aakash Tripathi
 * March 2022
 */

@Keep
data class NetworkParams(
    val baseUrl: String,  // Base URL
    val networkInterceptors: ArrayList<BaseInterceptor>, // List of interceptors
    val networkErrorListener: NetworkErrorListener? = null, // network error listener to listen fot parsing errors
    val readTimeOut: Long = 15, // OkHttp readTimeOut 15
    val connectTimeOut: Long = 15, // OkHttp connectTimeOut 15
    val writeTimeOut: Long = 15 // OkHttp writeTimeOut 15
)