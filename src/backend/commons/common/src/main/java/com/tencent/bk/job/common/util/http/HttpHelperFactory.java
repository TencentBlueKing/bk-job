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
    private static final CloseableHttpClient RETRYABLE_HTTP_CLIENT;
    private static final CloseableHttpClient LONG_RETRYABLE_HTTP_CLIENT;

    static {
        DEFAULT_HTTP_CLIENT = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            500,
            1000,
            60,
            false,
            null);
        RETRYABLE_HTTP_CLIENT = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            15000,
            500,
            1000,
            60,
            true,
            new JobHttpRequestRetryHandler());
        LONG_RETRYABLE_HTTP_CLIENT = JobHttpClientFactory.createHttpClient(
            15000,
            15000,
            30000,
            500,
            1000,
            60,
            true,
            new JobHttpRequestRetryHandler());
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
        HttpHelper baseHttpHelper = new BaseHttpHelper(DEFAULT_HTTP_CLIENT);
        return getWatchableExtHelper(baseHttpHelper);
    }

    @SuppressWarnings("unused")
    public static WatchableHttpHelper getRetryableHttpHelper() {
        HttpHelper baseHttpHelper = new BaseHttpHelper(RETRYABLE_HTTP_CLIENT);
        return getWatchableExtHelper(baseHttpHelper);
    }

    public static WatchableHttpHelper getLongRetryableHttpHelper() {
        HttpHelper baseHttpHelper = new BaseHttpHelper(LONG_RETRYABLE_HTTP_CLIENT);
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
        CloseableHttpClient httpClient = JobHttpClientFactory.createHttpClient(
            connRequestTimeout,
            connTimeout,
            socketTimeout,
            maxConnPerRoute,
            maxConnTotal,
            timeToLive,
            allowRetry,
            retryHandler);
        return new BaseHttpHelper(httpClient);
    }
}
