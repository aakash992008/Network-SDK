package com.aakash.solutions.networking.adapter

import com.aakash.solutions.networking.Networking
import com.aakash.solutions.networking.util.NetworkLogger
import com.squareup.moshi.Moshi
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type

/**
 * Created By Aakash Tripathi
 * April 2022
 */
internal class NetworkConvertorFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        for (annotation in annotations) {
            if (annotation.annotationClass == MoshiConvertor::class) {
                NetworkLogger.d("Response converted through Moshi")
                return MoshiConverterFactory.create(Moshi.Builder().build())
                    .responseBodyConverter(type, annotations, retrofit)
            } else if (annotation.annotationClass == GsonConvertor::class) {
                NetworkLogger.d("Response converted through Gson")
                return GsonConverterFactory.create()
                    .responseBodyConverter(type, annotations, retrofit)
            }
        }
        Networking.logWarningOnChucker("Provide Convertor Factory")
        return GsonConverterFactory.create().responseBodyConverter(type, annotations, retrofit)
    }


    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        for (annotation in methodAnnotations) {
            if (annotation.annotationClass == MoshiConvertor::class) {
                NetworkLogger.d("Request converted through Moshi")
                return MoshiConverterFactory.create(Moshi.Builder().build())
                    .requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
            } else if (annotation.annotationClass == GsonConvertor::class) {
                NetworkLogger.d("Request converted through Gson")
                return GsonConverterFactory.create()
                    .requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
            }
        }
        return GsonConverterFactory.create()
            .requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }
}
