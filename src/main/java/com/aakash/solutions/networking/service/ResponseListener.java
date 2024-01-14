package com.aakash.solutions.networking.service;

import com.aakash.solutions.networking.dto.ApiResponse;

public interface ResponseListener<T> {

    void onSuccessResponse(ApiResponse.Success<T> successResponse);

    void onErrorResponse(ApiResponse.Error errorResponse);
}
