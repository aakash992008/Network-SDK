**Network SDK Android**

It makes possible to read and write data across machines on the Internet which is made on top of OkHttp Networking Layer.


![enter image description here](https://firebasestorage.googleapis.com/v0/b/nykaa-android-preprod.appspot.com/o/Flowcharts.jpeg?alt=media&token=1b951b14-f055-457d-a448-94b9d8236c35)

[Implementation Guide](https://docs.google.com/document/d/17gTC-Fk3UpnoDIrLoAaD2cEpfWoW9_3HawKbqMaPeDM/edit)

    RX
    override fun getDataRx(responseCallBack: (apiResponse: ApiResponse<JsonObject>) -> Unit): Disposable =  
    client.getDataRx().performApiCall(responseCallBack)

 

    Coroutine
    override suspend fun getSignUpDataSuccessCoroutine(): Flow<ApiResponse<SignUpDataDto>> =  
         performApiCall {  
     client.getSignUpDataCoroutine()  
        }


Steps to Integrate

1. Add gradle dependencies
2. Initialise network SDK in application class
3. Setup BaseInterceptor
4. You are all set!! Now you can use extension functions in order to make API calls.