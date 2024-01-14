package com.aakash.solutions.networking

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.aakash.solutions.networking.adapter.CallAdapterFactory
import com.aakash.solutions.networking.adapter.NetworkConvertorFactory
import com.aakash.solutions.networking.dto.NetworkParams
import com.aakash.solutions.networking.service.NetworkErrorListener
import com.aakash.solutions.networking.util.NetworkLogger
import com.fsn.networking.networking.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Created By Aakash Tripathi
 * 11 March 2022
 */
object Networking {

    internal lateinit var mRetrofit: Retrofit
    internal lateinit var chuckerCollector: ChuckerCollector
    private const val cacheSize = (5 * 1024 * 1024).toLong() // 5MB Cache Size
    private var mErrorListener: NetworkErrorListener? = null


    /**
     * This function should be called single time from the Application class
     * calling it multiple times will have no effect
     * Responsible for initialising retrofit
     * @param context Application Context
     * @param networkParams Network Params data class ( Responsible for setting base url, interceptors ,read-write timeout)
     */
    fun initialiseNetworking(networkParams: NetworkParams, context: Context) {
        mErrorListener = networkParams.networkErrorListener
        if (!this::mRetrofit.isInitialized) {
            NetworkLogger.d("Network SDK Initialised")
            mRetrofit = Retrofit.Builder()
                .baseUrl(networkParams.baseUrl)
                .addConverterFactory(NetworkConvertorFactory())
                .addCallAdapterFactory(CallAdapterFactory())
                .client(getOkHTTPClient(networkParams, context))
                .build()
        } else {
            NetworkLogger.d("Redundant Call to initialise Network SDK")
        }
    }

    private fun getOkHTTPClient(networkParams: NetworkParams, context: Context): OkHttpClient =
        OkHttpClient().newBuilder().apply {
            this.cache(Cache(context.cacheDir, cacheSize))
            this.connectTimeout(networkParams.connectTimeOut, TimeUnit.SECONDS)
            this.readTimeout(networkParams.readTimeOut, TimeUnit.SECONDS)
            this.writeTimeout(networkParams.writeTimeOut, TimeUnit.SECONDS)
            for (interceptor in networkParams.networkInterceptors) {
                this.addInterceptor(interceptor)
            }
            this.addInterceptor(getChuckerInterceptor(context))
            if (BuildConfig.DEBUGGABLE) {
                this.addInterceptor(HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                })
            }
        }.build()

    /**
     * Log error on Chucker to get easier visibility of error
     */
    fun logErrorOnChucker(tag: String?, throwable: Throwable? = null) {
        logError(tag, throwable ?: NotFatalException(tag))
    }

    /**
     * Log error on Chucker to get easier visibility of error
     */
    fun logWarningOnChucker(tag: String?, throwable: Throwable? = null) {
        logError(tag, throwable ?: Warning(tag))
    }

    private fun logError(tag: String?, throwable: Throwable? = null) {
        // HANDLE IT LATER ( Should Log on Chcuker)
    }

    private fun getChuckerInterceptor(context: Context): ChuckerInterceptor {
        chuckerCollector = ChuckerCollector(context)
        chuckerCollector.showNotification = BuildConfig.DEBUGGABLE
        return ChuckerInterceptor.Builder(context)
            .collector(chuckerCollector).alwaysReadResponseBody(true).build()
    }

    /**
     * Function to get Retrofit client Interface
     * @throws RuntimeException  if network SDK is not initialised
     */
    fun <Service> getClientInterface(clientClass: Class<Service>): Service =
        if (Networking::mRetrofit.isInitialized) {
            NetworkLogger.d("Retrofit client created $clientClass")
            mRetrofit.create(clientClass)
        } else {
            throw RuntimeException("Retrofit Instance not Initialised")
        }

    /**
     * To check whether network library is initialised or not
     * @return true if Initialised false otherwise
     */
    fun isRetrofitInitialised(): Boolean = Networking::mRetrofit.isInitialized

    private class NotFatalException(message: String? = "") : Exception(message)
    private class Warning(message: String? = "") : Exception(message)

    /**
     * Log error on NetworkErrorListener
     */
    fun logError(e: Throwable, calledFrom: String) = mErrorListener?.onError(e,calledFrom).let {
        NetworkLogger.d("Error logged on network Error Listener $e")
    }
}