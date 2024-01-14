package com.aakash.solutions.networking.service

interface NetworkErrorListener {
    fun onError(throwable: Throwable?, calledFrom: String)
    fun onError(url: String, throwable: Throwable?)
}