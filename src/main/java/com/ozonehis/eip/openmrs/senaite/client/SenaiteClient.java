package com.ozonehis.eip.openmrs.senaite.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
public class SenaiteClient {
    @Value("${senaite.username}")
    private String senaiteUsername;

    @Value("${senaite.password}")
    private String senaitePassword;

    @Value("${senaite.baseUrl}")
    private String senaiteBaseUrl;

    @Getter
    private final OkHttpClient httpClient;

    private final int maxIdleConnections = 5;

    private final long keepAliveDurationMs = 300000;

    private final long callTimeoutMs = 0;

    private final long readTimeoutMs = 10000;

    private final long writeTimeoutMs = 10000;

    private final long connectTimeoutMs = 10000;

    public SenaiteClient() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS))
                .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .addInterceptor(this::addAuthenticationInterceptor)
                .retryOnConnectionFailure(true);

        this.httpClient = httpClientBuilder.build();
    }

    private Response addAuthenticationInterceptor(Interceptor.Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder().header("Authorization", authHeader());
        Request authorizedRequest = builder.build();
        return chain.proceed(authorizedRequest);
    }

    public String authHeader() {
        String auth = getSenaiteUsername() + ":" + getSenaitePassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    public Response executeRequest(Request request) throws IOException {
        return httpClient.newCall(request).execute();
    }
}
