package com.tencent.bk.job.common.util.http;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Service;

/**
 * Http请求基础工厂类
 */
@Slf4j
@Service
public class HttpHelperFactory {

    private static MeterRegistry meterRegistry;

    private static final CloseableHttpClient DEFAULT_HTTP_CLIENT;
    private static final CloseableHttpClient DEFAULT_HTTP_CLIENT_INSECURE;
    private static final CloseableHttpClient RETRYABLE_HTTP_CLIENT;
    private static final CloseableHttpClient RETRYABLE_HTTP_CLIENT_INSECURE;
    private static final CloseableHttpClient LONG_RETRYABLE_HTTP_CLIENT;
    private static final CloseableHttpClient LONG_RETRYABLE_HTTP_CLIENT_INSECURE;

    static {
        DEFAULT_HTTP_CLIENT = createClient(false, null, 15000, true);
        DEFAULT_HTTP_CLIENT_INSECURE = createClient(false, null, 15000, false);
        RETRYABLE_HTTP_CLIENT = createClient(true, new JobHttpRequestRetryHandler(), 15000, true);
        RETRYABLE_HTTP_CLIENT_INSECURE = createClient(true, new JobHttpRequestRetryHandler(), 15000, false);
        LONG_RETRYABLE_HTTP_CLIENT = createClient(true, new JobHttpRequestRetryHandler(), 30000, true);
        LONG_RETRYABLE_HTTP_CLIENT_INSECURE = createClient(true, new JobHttpRequestRetryHandler(), 30000, false);
    }

    private static CloseableHttpClient createClient(boolean allowRetry,
                                                    HttpRequestRetryHandler retryHandler,
                                                    int socketTimeout,
                                                    boolean sslVerifyEnabled) {
        return JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            socketTimeout,
            500,
            1000,
            60,
            allowRetry,
            retryHandler,
            httpClientBuilder -> {
                // do nothing
            },
            sslVerifyEnabled);
    }

    public static void setMeterRegistry(MeterRegistry meterRegistry) {
        HttpHelperFactory.meterRegistry = meterRegistry;
    }

    private static WatchableHttpHelper getWatchableHttpHelper(HttpHelper httpHelper) {
        return new WatchableHttpHelper(httpHelper, meterRegistry);
    }

    private static WatchableHttpHelper getWatchableExtHelper(HttpHelper httpHelper) {
        return getWatchableHttpHelper(httpHelper);
    }

    public static WatchableHttpHelper getDefaultHttpHelper() {
        return getDefaultHttpHelper(JobHttpSslVerifyConfig.isGlobalVerifyEnabled());
    }

    public static WatchableHttpHelper getDefaultHttpHelper(boolean sslVerifyEnabled) {
        HttpHelper baseHttpHelper = new BaseHttpHelper(
            sslVerifyEnabled ? DEFAULT_HTTP_CLIENT : DEFAULT_HTTP_CLIENT_INSECURE);
        return getWatchableExtHelper(baseHttpHelper);
    }

    @SuppressWarnings("unused")
    public static WatchableHttpHelper getRetryableHttpHelper() {
        return getRetryableHttpHelper(JobHttpSslVerifyConfig.isGlobalVerifyEnabled());
    }

    public static WatchableHttpHelper getRetryableHttpHelper(boolean sslVerifyEnabled) {
        HttpHelper baseHttpHelper = new BaseHttpHelper(
            sslVerifyEnabled ? RETRYABLE_HTTP_CLIENT : RETRYABLE_HTTP_CLIENT_INSECURE);
        return getWatchableExtHelper(baseHttpHelper);
    }

    public static WatchableHttpHelper getLongRetryableHttpHelper() {
        return getLongRetryableHttpHelper(JobHttpSslVerifyConfig.isGlobalVerifyEnabled());
    }

    public static WatchableHttpHelper getLongRetryableHttpHelper(boolean sslVerifyEnabled) {
        HttpHelper baseHttpHelper = new BaseHttpHelper(
            sslVerifyEnabled ? LONG_RETRYABLE_HTTP_CLIENT : LONG_RETRYABLE_HTTP_CLIENT_INSECURE);
        return getWatchableExtHelper(baseHttpHelper);
    }

    public static HttpHelper createHttpHelper(int connRequestTimeout,
                                              int connTimeout,
                                              int socketTimeout,
                                              int maxConnPerRoute,
                                              int maxConnTotal,
                                              int timeToLive,
                                              boolean allowRetry,
                                              HttpRequestRetryHandler retryHandler) {
        return createHttpHelper(
            connRequestTimeout,
            connTimeout,
            socketTimeout,
            maxConnPerRoute,
            maxConnTotal,
            timeToLive,
            allowRetry,
            retryHandler,
            true);
    }

    public static HttpHelper createHttpHelper(int connRequestTimeout,
                                              int connTimeout,
                                              int socketTimeout,
                                              int maxConnPerRoute,
                                              int maxConnTotal,
                                              int timeToLive,
                                              boolean allowRetry,
                                              HttpRequestRetryHandler retryHandler,
                                              boolean sslVerifyEnabled) {
        CloseableHttpClient httpClient = JobHttpClientFactory.createHttpClient(
            connRequestTimeout,
            connTimeout,
            socketTimeout,
            maxConnPerRoute,
            maxConnTotal,
            timeToLive,
            allowRetry,
            retryHandler,
            httpClientBuilder -> {
                // do nothing
            },
            sslVerifyEnabled);
        return new BaseHttpHelper(httpClient);
    }
}
