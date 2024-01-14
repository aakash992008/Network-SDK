package com.aakash.solutions.networking.service


import com.aakash.solutions.networking.Networking
import com.aakash.solutions.networking.adapter.NoCommonBodyParameters
import com.aakash.solutions.networking.adapter.NoCommonHeadersParameters
import com.aakash.solutions.networking.adapter.NoCommonParameters
import com.aakash.solutions.networking.adapter.NoCommonQueryParameters
import com.aakash.solutions.networking.dto.BaseResponse
import com.aakash.solutions.networking.extensions.getCustomAnnotation
import com.aakash.solutions.networking.extensions.isNull
import com.aakash.solutions.networking.extensions.toJsonString
import com.aakash.solutions.networking.util.NetworkLogger
import com.aakash.solutions.networking.util.NetworkUtil
import com.aakash.solutions.networking.util.NetworkingConstant
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Created By
 * Aakash Tripathi
 * March 2022
 */
abstract class BaseInterceptor : Interceptor, BaseInterceptorInterface {

    companion object {
        private const val JSON_BODY = "application/json; charset=UTF-8"
        private const val FORM_BODY = "application/x-www-form-urlencoded"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var originalRequest = chain.request()
        val url = originalRequest.url.toString()

        if (originalRequest.getCustomAnnotation(NoCommonParameters::class.java).isNull()) {
            if (originalRequest.getCustomAnnotation(NoCommonHeadersParameters::class.java)
                    .isNull()
            ) {
                originalRequest = addHeaders(url, originalRequest)
            }
            if (originalRequest.getCustomAnnotation(NoCommonQueryParameters::class.java).isNull()) {
                originalRequest = addQuery(url, originalRequest)
            }
            if (originalRequest.getCustomAnnotation(NoCommonBodyParameters::class.java).isNull()) {
                originalRequest = addBody(url, originalRequest)
            }
        }


        NetworkLogger.d("Interceptor -> Request for ${originalRequest.url}")
        apiRequest(originalRequest)
        /**
         * isTheRequestAllowed()
         * In case of false value skip the api call and proceed directly with the error response
         */
        if (!isTheRequestAllowed(url)) {
            NetworkLogger.d("Interceptor -> Request BLOCKED ${originalRequest.url}")
            Networking.logErrorOnChucker("Request not allowed (Blocked) $url")
            return getResponseOnApiBlocked(url, originalRequest)
        } else {
            originalRequest = if (isInternetConnected()) {
                originalRequest.newBuilder()
                    .header(NetworkingConstant.CACHE, "public, max-age=" + 5).build()
            } else {
                NetworkLogger.d("Interceptor -> Internet Not available ${originalRequest.url}")
                //Caching only works for GET request
                val requestResponse = handleInternetNotAvailable(originalRequest)
                if (requestResponse.first != null) {
                    requestResponse.first!!
                } else {
                    return requestResponse.second!!
                }
            }
            var apiResponse: Response = chain.proceed(originalRequest)


            NetworkLogger.d("Interceptor -> Api Response  ${originalRequest.url}  ${apiResponse.code}")

            var jsonResponse = apiResponse.peekBody(Long.MAX_VALUE).string()
            val responseTime =
                apiResponse.receivedResponseAtMillis - apiResponse.sentRequestAtMillis
            apiResponseTime(responseTime, url, jsonResponse)
            if (apiResponse.header("Content-Encoding").equals("gzip")) {
                NetworkLogger.d("Interceptor -> GZIP encoded ${originalRequest.url}")
                jsonResponse = decompress(apiResponse.peekBody(Long.MAX_VALUE).bytes())
                apiResponse = apiResponse.newBuilder().body(
                    jsonResponse.toResponseBody(JSON_BODY.toMediaTypeOrNull())
                ).removeHeader("Content-Encoding").build()
            }


            if (isResponseSuccess(apiResponse.code)) {
                successResponse(url, apiResponse.code, jsonResponse)
            } else {
                errorResponse(url, apiResponse.code, jsonResponse)
            }
            return apiResponse
        }
    }

    private fun addHeaders(url: String, request: Request): Request {
        addCommonHeaders(url)?.let {
            if (it.isNotEmpty()) {
                val builder = request.newBuilder()
                for (pair in it) {
                    builder.addHeader(pair.first, pair.second)
                }
                return builder.build()
            }
        }
        return request
    }

    private fun addQuery(url: String, request: Request): Request {
        addCommonQueryParameters(url)?.let {
            if (it.isNotEmpty()) {
                val urlBuilder = request.url.newBuilder()
                for (pair in it) {
                    urlBuilder.addQueryParameter(pair.first, pair.second)
                }
                return request.newBuilder().url(urlBuilder.build()).build()
            }
        }
        return request
    }

    private fun addBody(url: String, request: Request): Request {
        addCommonBodyParameters(url)?.let {
            request.body?.let { body ->
                when (body.contentType()?.subtype) {
                    JSON_BODY.toMediaTypeOrNull()?.subtype -> {
                        //JSON_DATA
                        processApplicationJsonRequestBody(body, it)?.let {
                            return request.newBuilder()
                                .method(request.method,it)
                                .build()
                        }
                    }
                    FORM_BODY.toMediaTypeOrNull()?.subtype -> {
                        //FORM_DATA
                        processFormDataRequestBody(body, it)?.let {
                            return request.newBuilder()
                                .method(request.method,it)
                                .build()
                        }
                    }
                    else -> {
                        return request
                    }
                }
            }
        }
        return request
    }

    private fun getResponseOnApiBlocked(url: String, request: Request): Response {
        val errorJson = getRequestBlockResponse(url).toJsonString()
        val body: ResponseBody = ResponseBody.create(
            JSON_BODY.toMediaTypeOrNull(),
            errorJson
        )
        errorResponse(url, NetworkingConstant.API_BLOCKED.toInt(), errorJson)
        return Response.Builder().code(NetworkingConstant.API_BLOCKED.toInt())
            .protocol(Protocol.HTTP_2)
            .request(request)
            .body(body)
            .message(NetworkingConstant.UNKNOWN_ERROR_MESSAGE)
            .build()
    }

    private fun handleInternetNotAvailable(request: Request): Pair<Request?, Response?> {
        return if (request.method == "GET") {
            if (isCachedEnabled(request.headers)) {
                Pair(
                    request.newBuilder()
                        .header(
                            NetworkingConstant.CACHE,
                            "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7
                        )
                        .build(), null
                )
            } else {
                Pair(null, getNoInternetConnectionResponse(request))
            }
        } else {
            Pair(null, getNoInternetConnectionResponse(request))
        }
    }

    /**
     * Covert to JSON string from gZip Compress
     */
    private fun decompress(compressed: ByteArray?): String = try {
        val bis = ByteArrayInputStream(compressed)
        val gis = GZIPInputStream(bis)
        val br = BufferedReader(InputStreamReader(gis, "UTF-8"))
        val sb = StringBuilder()
        var line: String?
        while (br.readLine().also { line = it } != null) {
            sb.append(line)
        }
        br.close()
        gis.close()
        bis.close()
        sb.toString()
    } catch (e: Exception) {
        Networking.logErrorOnChucker("ERROR In decompress() Base Interceptor", e)
        ""
    }

    private fun getNoInternetConnectionResponse(request: Request): Response {
        val response: ResponseBody = Gson().toJson(
            BaseResponse(
                "fail", null, null, null, NetworkingConstant.INTERNET_UNAVAILABLE,
                NetworkingConstant.NO_INTERNET_CONNECTION_CODE.toInt(), null, null
            )
        ).toResponseBody(JSON_BODY.toMediaTypeOrNull())

        return Response.Builder().code(NetworkingConstant.NO_INTERNET_CONNECTION_CODE.toInt())
            .request(request)
            .protocol(Protocol.HTTP_2)
            .message(NetworkingConstant.INTERNET_UNAVAILABLE)
            .body(response).build()
    }
}


private fun processApplicationJsonRequestBody(
    requestBody: RequestBody,
    listOfBodyParameters: ArrayList<Pair<String, Any>>
): RequestBody? {
    val customReq = bodyToString(requestBody) ?: ""
    try {
        val jsonObject = if (customReq.isNullOrEmpty()) {
            JSONObject()
        } else {
            JSONObject(customReq)
        }
        for (pair in listOfBodyParameters) {
            jsonObject.put(pair.first, pair.second)
        }
        return jsonObject.toString().toRequestBody(requestBody.contentType())
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    return null
}

private fun isCachedEnabled(headers: Headers): Boolean =
    headers[NetworkingConstant.CACHE_DATA] != null


private fun processFormDataRequestBody(
    requestBody: RequestBody,
    listOfBodyParameters: ArrayList<Pair<String, Any>>
): RequestBody? {
    val builder = FormBody.Builder()
    for (pair in listOfBodyParameters) {
        builder.add(pair.first, pair.second.toString())
    }
    var postBodyString = bodyToString(requestBody)
    postBodyString += (if (postBodyString!!.isNotEmpty()) "&" else "") + bodyToString(builder.build())
    return postBodyString.toRequestBody(requestBody.contentType())
}


private fun bodyToString(request: RequestBody): String? {
    return try {
        val buffer = Buffer()
        if (request != null) request.writeTo(buffer) else return ""
        buffer.readUtf8()
    } catch (e: IOException) {
        ""
    }
}


interface BaseInterceptorInterface {
    /**
     * default function which gives you the entire request that is being send to server.
     * @param request okhttp3.Request
     * @author Aakash
     */
    fun apiRequest(request: Request) {
        //For Default Use
    }

    /**
     * default function which takes list of Headers
     * @param url api URL
     * @return ArrayList<Pair<String, String>>? return null if no headers need to be added else return the ArrayList<Pair<String, String>> where Pair<HEADER_NAME,HEADER_VALUE>
     * @author Aakash
     */
    fun addCommonHeaders(url: String): ArrayList<Pair<String, String>>? = null

    /**
     * default function which takes list of Query Parameters
     * @param url api URL
     * @return ArrayList<Pair<String, String>>? return null if no query parameters need to be added else return the ArrayList<Pair<String, String>> where Pair<QUERY_NAME,QUERY_VALUE>
     * @author Aakash
     */
    fun addCommonQueryParameters(url: String): ArrayList<Pair<String, String>>? = null

    /**
     * default function which takes list of Body Parameters
     * @param url api URL
     * @return ArrayList<Pair<String, Any>>? return null if no Body parameters need to be added else return the ArrayList<Pair<String, Any>> where Pair<KEY,VALUE>
     * @author Aakash
     */
    fun addCommonBodyParameters(url: String): ArrayList<Pair<String, Any>>? = null

    /**
     * default function which check the internet connectivity
     * default return type  is true
     * @return boolean return true if internet is connected false otherwise
     * @author Aakash
     */
    fun isInternetConnected(): Boolean = true

    fun apiResponseTime(timeInMilli: Long, url: String, jsonResponse: String?) {

    }

    fun getErrorResponseOnBlock(): JSONObject? =
        NetworkUtil.getDefaultResponseIfFireStoreBlockedApi()

    /**
     * default function which check whether the URL is allowed or not
     * @return boolean true if request is allowed false otherwise
     * @author Aakash
     */
    fun isTheRequestAllowed(url: String): Boolean = true

    /**
     * default function which should return the Object
     * @return Object (Can be null) return the object which will be converted to json
     * @author Aakash
     */
    fun getRequestBlockResponse(url: String): Any? = null

    /**
     * default function which get called when isResponseSuccess return false
     * @param url complete API url
     * @param responseCode api response code
     * @param jsonBody errorJson Body
     * @author Aakash
     *  @see isResponseSuccess
     */
    fun errorResponse(url: String, responseCode: Int, jsonBody: String): Boolean = false

    /**
     * default function which get called when isResponseSuccess return true
     * @param url complete API url
     * @param responseCode api response code
     * @param jsonBody success Body
     * @author Aakash
     *  @see isResponseSuccess
     */
    fun successResponse(url: String, responseCode: Int, jsonBody: String) {
        // default
    }

    /**
     * default function which return true when api response code is in range 200..2009
     * @param statusCode api response code
     * @return boolean is response code success
     * @author Aakash
     */
    fun isResponseSuccess(statusCode: Int): Boolean = statusCode in 200..299
}
