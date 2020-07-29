package com.pengrad.telegrambot.impl;

import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * stas
 * 5/1/16.
 */
public class TelegramBotClient {

    private final OkHttpClient client;
    private OkHttpClient clientWithTimeout;
    private final String baseUrl;

    public TelegramBotClient(OkHttpClient client, String baseUrl) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.clientWithTimeout = client;
    }

    public <T extends BaseRequest<T,R>, R extends BaseResponse> void send(final T request, final Callback<T, R> callback) {
        OkHttpClient client = getOkHttpClient(request);
        client.newCall(createRequest(request)).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                int responseCode = response.code();

                ResponseBody body = response.body();
                if(body == null) {
                    callback.onFailure(request, new IOException("Empty response"));
                    return;
                }
                String bodyText = null;
                try {
                    bodyText = body.string();
                    R result = BotUtils.fromJson(bodyText, request.getResponseType());
                    if (result == null) {
                        callback.onFailure(request, new IOException("Empty response (deserialization)"));
                    } else {
                        callback.onResponse(request, result);
                    }
                } catch (Throwable e) {
                    if(bodyText == null) {
                        callback.onFailure(request, new IOException(e.getMessage() + "\nResponseCode: "+responseCode+"\nBody: N/A", e));
                    } else {
                        callback.onFailure(request, new IOException(e.getMessage() + "\nResponseCode: "+responseCode+"\nBody: [" + bodyText + "]", e));
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(request, e);
            }
        });
    }

    public <T extends BaseRequest<T, R>, R extends BaseResponse> R send(final BaseRequest<T, R> request) throws RuntimeException {
        String bodyText = null;
        int responseCode = -1;
        try {
            OkHttpClient client = getOkHttpClient(request);
            Response response = client.newCall(createRequest(request)).execute();
            responseCode = response.code();
            ResponseBody body = response.body();
            if(body == null) {
                throw new RuntimeException("Empty response");
            }
            bodyText = body.string();
            return BotUtils.fromJson(bodyText, request.getResponseType());
        } catch (Throwable e) {
            if(bodyText == null) {
                throw new RuntimeException(e.getMessage() + "\nResponseCode: "+responseCode+"\nBody: N/A", e);
            }
            throw new RuntimeException(e.getMessage() + "\nResponseCode: "+responseCode+"\nBody: [" + bodyText + "]", e);
        }
    }

    private OkHttpClient getOkHttpClient(BaseRequest<?, ?> request) {
        int timeoutMillis = request.getTimeoutSeconds() * 1000;

        if (client.readTimeoutMillis() == 0 || client.readTimeoutMillis() > timeoutMillis) return client;
        if (clientWithTimeout.readTimeoutMillis() > timeoutMillis) return clientWithTimeout;

        clientWithTimeout = client.newBuilder().readTimeout(timeoutMillis + 1000, TimeUnit.MILLISECONDS).build();
        return clientWithTimeout;
    }

    private Request createRequest(BaseRequest<?, ?> request) {
        return new Request.Builder()
                .url(baseUrl + request.getMethod())
                .post(createRequestBody(request))
                .build();
    }

    RequestBody createRequestBody(BaseRequest<?, ?> request) {
        if (request.isMultipart()) {
            MediaType contentType = MediaType.parse(request.getContentType());

            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            for (Map.Entry<String, Object> parameter : request.getParameters().entrySet()) {
                String name = parameter.getKey();
                Object value = parameter.getValue();
                if (value instanceof byte[]) {
                    builder.addFormDataPart(name, request.getFileName(), RequestBody.create(contentType, (byte[]) value));
                } else if (value instanceof File) {
                    builder.addFormDataPart(name, request.getFileName(), RequestBody.create(contentType, (File) value));
                } else {
                    builder.addFormDataPart(name, toParamValue(value));
                }
            }

            return builder.build();
        } else {
            FormBody.Builder builder = new FormBody.Builder();
            for (Map.Entry<String, Object> parameter : request.getParameters().entrySet()) {
                builder.add(parameter.getKey(), toParamValue(parameter.getValue()));
            }
            return builder.build();
        }
    }

    private final static List<Class<?>> useToString = Collections.unmodifiableList(
            Arrays.asList(
                    int.class,
                    Integer.class,
                    float.class,
                    Float.class,
                    double.class,
                    Double.class,
                    String.class));

    String toParamValue(Object obj) {
        if(useToString.contains(obj.getClass())) {
            return String.valueOf(obj);
        }
        return BotUtils.toJson(obj);
    }
}
