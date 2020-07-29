package com.pengrad.telegrambot;

import com.pengrad.telegrambot.model.ResponseParameters;
import com.pengrad.telegrambot.response.BaseResponse;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Stas Parshin
 * 26 January 2020
 */
public class RetryInterceptor implements Interceptor {

    private final int defaultSleepMillis;

    public RetryInterceptor() {
        this(1000);
    }

    public RetryInterceptor(int defaultSleepMillis) {
        this.defaultSleepMillis = defaultSleepMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Exception exception = null;
        int retries = 3;
        while (retries-- > 0) {
            try {
                Response response = chain.proceed(request);
                if (response.isSuccessful()) {
                    return response;
                }
                if (response.code() != 429) {
                    return response;
                }
                ResponseBody body = response.body();
                if(body != null) {
                    BaseResponse baseResponse = BotUtils.fromJson(body.string(), BaseResponse.class);
                    ResponseParameters params = baseResponse.parameters();
                    int sleepTimeMillis;
                    if (params != null && params.retryAfter() != null) {
                        sleepTimeMillis = params.retryAfter() * 1000;
                    } else {
                        sleepTimeMillis = defaultSleepMillis;
                    }
                    System.err.println("++++ response " + response.code() + " sleep for " + sleepTimeMillis + " ms");
                    Thread.sleep(sleepTimeMillis);
                }
            } catch (Exception e) {
                exception = e;
                System.err.println("++++ exception " + e.getMessage());
                e.printStackTrace(System.err);
                try {
                    Thread.sleep(defaultSleepMillis);
                } catch (InterruptedException ignored) {}
            }
        }
        if (exception instanceof IOException) {
            throw (IOException) exception;
        }
        if(exception == null) {
            throw new RuntimeException("empty exception");
        }
        throw new RuntimeException(exception);
    }
}
