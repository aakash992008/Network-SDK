package com.aakash.solutions.networking.extensions

import androidx.lifecycle.MutableLiveData
import com.aakash.solutions.networking.dto.ApiResponse
import com.aakash.solutions.networking.dto.BaseResponse
import com.aakash.solutions.networking.util.NetworkLogger
import com.aakash.solutions.networking.util.NetworkUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Invocation
import java.util.concurrent.TimeUnit

/**
 * Utility extension to convert Object to JsonObject if possible
 * @return JsonObject? null if not able to convert it
 */
fun Any?.toJsonObject(): JsonObject? = try {
    if (this == null) {
        null
    } else if (this is String) {
        if (this.isEmpty()) {
            null
        } else {
            try {
                JSONObject(this)
                Gson().fromJson(this, JsonObject::class.java)
            } catch (e: Exception) {
                null
            }
        }
    } else {
        NetworkUtil.getJsonObjectFromGson(Gson().toJson(this))
    }
} catch (e: Exception) {
    null
}

fun Any?.toJsonString(defaultJson: String = ""): String = try {
    when {
        this == null -> {
            defaultJson
        }
        this is JSONObject || this is JsonObject -> {
            this.toString()
        }
        else -> {
            Gson().toJson(this)
        }
    }
} catch (e: Exception) {
    defaultJson
}

fun <T> Call<T>.getUrl(): String = (this.request() as Request).url.toString()

fun <Response, Base : BaseResponse<Response>> Base.getResult(): ApiResponse<Response> =
    NetworkUtil.getResponse(this)

fun <Response, Base : BaseResponse<Response>> Base.getBooleanResult(): ApiResponse<Boolean> =
    NetworkUtil.getBooleanResponse(this)

internal fun Throwable.getApiErrorResponse(): ApiResponse.Error = NetworkUtil.getError(this)


//RX JAVA CALLS

fun <Response, Base : BaseResponse<Response>> Observable<Base>.performApiCall(
    successResponse: (ApiResponse.Success<Response>) -> Unit,
    errorResponse: (ApiResponse.Error) -> Unit,
    subscribeOn: Scheduler = Schedulers.io(),
    observeOnMainThread: Boolean = false,
): Disposable =
    subscribeOn(subscribeOn).observeOn(if (observeOnMainThread) AndroidSchedulers.mainThread() else Schedulers.io())
        .subscribe({ baseResponse ->
            baseResponse.getResult().apply {
                NetworkLogger.apiResponse(this)
                if (this is ApiResponse.Success) {
                    successResponse.invoke(this)
                } else if (this is ApiResponse.Error) {
                    errorResponse.invoke(this)
                }
            }
        }, {
            it.getApiErrorResponse().apply {
                errorResponse.invoke(this)
            }
        })


fun <Response, Base : BaseResponse<Response>> Observable<Base>.performApiCall(
    responseCallBack: (apiResponse: ApiResponse<Response>) -> Unit,
): Disposable {
    val disposable = subscribeOn(Schedulers.io()).subscribe({
        it.getResult().apply {
            NetworkLogger.apiResponse(this)
            responseCallBack.invoke(this)
        }
    }, {
        it.getApiErrorResponse().apply {
            responseCallBack.invoke(this)
        }
    })
    return disposable
}

fun <Response, Base : BaseResponse<Response>> Observable<Base>.performApiCall(
    responseCallBack: (apiResponse: ApiResponse<Response>) -> Unit,
    observeOnMainThread: Boolean = false,
): Disposable {
    val disposable =
        subscribeOn(Schedulers.io()).observeOn(if (observeOnMainThread) AndroidSchedulers.mainThread() else Schedulers.io())
            .subscribe({
                it.getResult().apply {
                    NetworkLogger.apiResponse(this)
                    responseCallBack.invoke(this)
                }
            }, {
                it.getApiErrorResponse().apply {
                    responseCallBack.invoke(this)
                }
            })
    return disposable
}

/**
 * Extension to perform apiCall using RX
 * @param successResponse receive callback when response is success
 * @param errorResponse receive callback when response is error
 * @param observeOnMainThread default value is false
 */
fun <Response, Base : BaseResponse<Response>> Observable<Base>.performApiCall(
    successResponse: (apiResponse: ApiResponse.Success<Response>) -> Unit,
    errorResponse: (apiResponse: ApiResponse.Error) -> Unit,
    observeOnMainThread: Boolean = false,
): Disposable {
    val disposable =
        subscribeOn(Schedulers.io()).observeOn(if (observeOnMainThread) AndroidSchedulers.mainThread() else Schedulers.io())
            .subscribe({
                it.getResult().apply {
                    when (this) {
                        is ApiResponse.Error -> {
                            errorResponse.invoke(this)
                        }
                        is ApiResponse.Success -> {
                            successResponse.invoke(this)
                        }
                    }
                }
            }, {
                it.getApiErrorResponse().apply {
                    errorResponse.invoke(this)
                }
            })
    return disposable
}


fun <Response, Base : BaseResponse<Response>> Observable<Base>.performApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Response>>,
): Disposable {
    val disposable = subscribeOn(Schedulers.io()).subscribe({ baseResponse ->
        baseResponse.getResult().apply {
            responseLiveData.postValue(this)
        }
    }, {
        it.getApiErrorResponse().apply {
            responseLiveData.postValue(this)
        }
    })
    return disposable
}

fun <Response> Observable<Response>.performRawApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Response>>,
): Disposable {
    val disposable = subscribeOn(Schedulers.io()).subscribe({ jsonResponse ->
        responseLiveData.postValue(NetworkUtil.getResponse(jsonResponse))
    }, {
        it.getApiErrorResponse().apply {
            responseLiveData.postValue(this)
        }
    })
    return disposable
}

fun <Response> Observable<Response>.performRawApiCall(
    response: (apiResponse: ApiResponse<Response>) -> Unit,
): Disposable {
    val disposable = subscribeOn(Schedulers.io()).subscribe({ jsonResponse ->

        response.invoke(NetworkUtil.getResponse(jsonResponse))
    }, {
        it.getApiErrorResponse().apply {
            response.invoke(this)
        }
    })
    return disposable
}

fun <Response> Observable<BaseResponse<Response>>.performBooleanApiCall(
    responseCallBack: (apiResponse: ApiResponse<Boolean>) -> Unit,
): Disposable {
    val disposable = subscribeOn(Schedulers.io()).subscribe({
        it.getBooleanResult().apply {
            responseCallBack.invoke(this)
            NetworkLogger.apiResponse(this)
        }
    }, {
        it.getApiErrorResponse().apply {
            NetworkLogger.apiResponse(this)
            responseCallBack.invoke(this)
        }
    })
    return disposable
}

fun <Response> Observable<BaseResponse<Response>>.performBooleanApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Boolean>>,
): Disposable {
    val disposable = subscribeOn(Schedulers.io()).subscribe({
        responseLiveData.postValue(it.getBooleanResult().apply {
            NetworkLogger.apiResponse(this)
        })
    }, {
        it.getApiErrorResponse().apply {
            responseLiveData.postValue(this)
        }
    })
    return disposable
}


//Retrofit Calls

fun <Response> Call<BaseResponse<Response>>.performApiCall(
    onResponse: (ApiResponse<Response>) -> Unit,
    errorCall: ((Pair<Call<*>, Throwable>) -> Unit)? = null,
    successResponse: ((Pair<Call<*>, retrofit2.Response<BaseResponse<Response>>>) -> Unit)? = null,
) {
    this.timeout().timeout(3, TimeUnit.SECONDS)
    this@performApiCall.enqueue(object : Callback<BaseResponse<Response>> {
        override fun onResponse(
            call: Call<BaseResponse<Response>>,
            response: retrofit2.Response<BaseResponse<Response>>,
        ) {
            response.body()?.getResult()?.apply {
                NetworkLogger.apiResponse(this)
                successResponse?.invoke(Pair(call, response))
                onResponse(this)
            }
        }

        override fun onFailure(call: Call<BaseResponse<Response>>, t: Throwable) {
            t.getApiErrorResponse().apply {
                onResponse.invoke(this)
            }
        }
    })
}

fun <Response> Call<BaseResponse<Response>>.performApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Response>>,
) {
    this@performApiCall.enqueue(object : Callback<BaseResponse<Response>> {
        override fun onResponse(
            call: Call<BaseResponse<Response>>,
            response: retrofit2.Response<BaseResponse<Response>>,
        ) {
            response.body()?.getResult()?.apply {
                NetworkLogger.apiResponse(this)
                responseLiveData.postValue(response.body()?.getResult())
            }
        }

        override fun onFailure(call: Call<BaseResponse<Response>>, t: Throwable) {
            t.getApiErrorResponse().apply {
                responseLiveData.postValue(this)
            }
        }
    })
}


fun <Response> Call<Response>.performRawApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Response>>,
) {
    this.enqueue(object : Callback<Response> {
        override fun onResponse(
            call: Call<Response>,
            response: retrofit2.Response<Response>,
        ) {
            NetworkUtil.getResponse(response.body()).apply {
                NetworkLogger.apiResponse(this)
                responseLiveData.postValue(this)
            }
        }

        override fun onFailure(call: Call<Response>, t: Throwable) {
            t.getApiErrorResponse().apply {
                responseLiveData.postValue(this)
            }
        }
    })
}

fun <Response> Call<Response>.performRawApiCall(
    responseCallBack: (apiResponse: ApiResponse<Response>) -> Unit,
) {
    this.enqueue(object : Callback<Response> {
        override fun onResponse(
            call: Call<Response>,
            response: retrofit2.Response<Response>,
        ) {
            NetworkUtil.getResponse(response.body()).apply {
                NetworkLogger.apiResponse(this)
                responseCallBack.invoke(this)
            }
        }

        override fun onFailure(call: Call<Response>, t: Throwable) {
            t.getApiErrorResponse().apply {
                responseCallBack.invoke(this)
            }
        }
    })
}


fun <Response> Call<Response>.performRawApiCall(
    responseCallBack: (apiResponse: ApiResponse<Response>) -> Unit,
    timeOutInSeconds: Long,
) {
    this.timeout().timeout(timeOutInSeconds, TimeUnit.SECONDS)
    this.enqueue(object : Callback<Response> {
        override fun onResponse(
            call: Call<Response>,
            response: retrofit2.Response<Response>,
        ) {
            NetworkUtil.getResponse(response.body()).apply {
                NetworkLogger.apiResponse(this)
                responseCallBack.invoke(this)
            }
        }

        override fun onFailure(call: Call<Response>, t: Throwable) {
            t.getApiErrorResponse().apply {
                responseCallBack.invoke(this)
            }
        }
    })
}


//Coroutine Calls


suspend inline fun <Response> performRawApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Response>>,
    crossinline apiCall: suspend () -> Response,
) {
    try {
        apiCall.invoke().apply {
            NetworkLogger.apiResponse(ApiResponse.Success(this))
            responseLiveData.postValue(ApiResponse.Success(this))
        }
    } catch (exception: Exception) {
        NetworkUtil.getError(exception).apply {
            responseLiveData.postValue(this)
        }
    }
}


suspend inline fun <Response, Base : BaseResponse<Response>> performApiCall(
    crossinline apiCall: suspend () -> Base,
): Flow<ApiResponse<Response>> = flow {
    try {
        apiCall.invoke().getResult().apply {
            emit(this)
        }
    } catch (exception: Exception) {
        NetworkUtil.getError(exception).apply {
            emit(this)
        }
    }
}.flowOn(Dispatchers.IO)

suspend inline fun <Response> performRawApiCall(
    crossinline apiCall: suspend () -> Response,
): Flow<ApiResponse<Response>> = flow {
    try {
        apiCall.invoke().apply {
            NetworkLogger.apiResponse(ApiResponse.Success(this))
            emit(ApiResponse.Success(this))
        }
    } catch (exception: Exception) {
        NetworkUtil.getError(exception).apply {
            emit(this)
        }
    }
}.flowOn(Dispatchers.IO)


suspend fun <Response, Base : BaseResponse<Response>> performBooleanApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Boolean>>,
    apiCall: suspend () -> Base,
) {
    try {
        responseLiveData.postValue(apiCall.invoke().getBooleanResult().apply {
            NetworkLogger.apiResponse(this)
        })
    } catch (e: Exception) {
        NetworkUtil.getError(e).apply {
            responseLiveData.postValue(this)
        }
    }
}

suspend fun <Response, Base : BaseResponse<Response>> performBooleanApiCall(
    apiCall: suspend () -> Base,
): Flow<ApiResponse<Boolean>> = flow {
    try {
        emit(apiCall.invoke().getBooleanResult().apply { NetworkLogger.apiResponse(this) })
    } catch (e: Exception) {
        NetworkUtil.getError(e).apply {
            emit(this)
        }
    }
}


suspend fun <Response> performRawBooleanApiCall(
    apiCall: suspend () -> Response,
): Flow<ApiResponse<Boolean>> = flow {
    try {
        apiCall.invoke().apply {
            emit(ApiResponse.Success(true))
        }
    } catch (e: Exception) {
        NetworkUtil.getError(e).apply {
            emit(this)
        }
    }
}

suspend inline fun <Response, Base : BaseResponse<Response>> performApiCall(
    responseLiveData: MutableLiveData<ApiResponse<Response>>,
    crossinline apiCall: suspend () -> Base,
) {
    try {
        apiCall.invoke().getResult().apply {
            NetworkLogger.apiResponse(this)
            responseLiveData.postValue(this)
        }
    } catch (e: Exception) {
        NetworkUtil.getError(e).apply {
            responseLiveData.postValue(this)
        }
    }
}

suspend inline fun <Response> performCall(
    crossinline apiCall: suspend () -> BaseResponse<Response>,
): ApiResponse<Response> = try {
    apiCall.invoke().getResult().apply {
        NetworkLogger.apiResponse(this)
    }
} catch (e: Exception) {
    NetworkUtil.getError(e)
}


suspend fun <Response, Base : BaseResponse<Response>> performBooleanCall(
    apiCall: suspend () -> Base,
): ApiResponse<Boolean> = try {
    apiCall.invoke().getBooleanResult().apply { NetworkLogger.apiResponse(this) }
} catch (e: Exception) {
    NetworkUtil.getError(e)
}

suspend fun <Response> performRawBooleanCall(
    apiCall: suspend () -> Response,
): ApiResponse<Boolean> = try {
    apiCall.invoke()
    ApiResponse.Success(true, null).apply {
        NetworkLogger.apiResponse(this)
    }
} catch (e: Exception) {
    NetworkUtil.getError(e)
}

suspend inline fun <Response> performRawCall(
    crossinline apiCall: suspend () -> Response,
): ApiResponse<Response> = try {
    ApiResponse.Success(apiCall.invoke())
} catch (e: Exception) {
    NetworkUtil.getError(e)
}

fun <T : Annotation> Request.getCustomAnnotation(annotationClass: Class<T>): T? = this.tag(
    Invocation::class.java
)?.method()?.getAnnotation(annotationClass)


fun Any?.isNull(): Boolean = (this == null)
fun Any?.isNotNull(): Boolean = (this != null)





