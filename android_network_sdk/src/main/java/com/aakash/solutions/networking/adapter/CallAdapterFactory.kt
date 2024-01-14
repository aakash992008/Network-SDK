package com.aakash.solutions.networking.adapter

import com.aakash.solutions.networking.util.NetworkLogger
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.Type

internal class CallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        for (annotation in annotations) {
             if (annotation.annotationClass == RxJavaFactory::class) {
                 NetworkLogger.d("Using RX_JAVA Call adapter factory")
                 return RxJava2CallAdapterFactory.create().get(returnType, annotations, retrofit)
            }
        }
        return null
    }
}