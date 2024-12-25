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
        return new WatchableHttpHelper(httpHelper, () -> meterRegistry);
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

    /**
     * 创建自定义的HttpHelper，每创建一次都会生成一个常驻的连接驱逐线程，请在单例中使用，避免线程泄露
     *
     * @param customizer 自定义的HttpClient定制器
     * @return 自定义的HttpHelper
     */
    public static WatchableHttpHelper createHttpHelper(JobHttpClientFactory.HttpClientCustomizer customizer) {
        HttpHelper baseHttpHelper = createHttpHelper(
            15000,
            15000,
            15000,
            500,
            1000,
            60,
            false,
            null,
            customizer
        );
        return getWatchableExtHelper(baseHttpHelper);
    }

    /**
     * 创建自定义的HttpHelper，每创建一次都会生成一个常驻的连接驱逐线程，请在单例中使用，避免线程泄露
     *
     * @param connRequestTimeout 连接请求超时时间，单位毫秒
     * @param connTimeout        连接超时时间，单位毫秒
     * @param socketTimeout      socket读写超时时间，单位毫秒
     * @param maxConnPerRoute    单个路由最大连接数
     * @param maxConnTotal       总的最大连接数
     * @param timeToLive         连接驱逐线程存活时间，单位秒
     * @param allowRetry         是否允许重试
     * @param retryHandler       重试策略
     * @param customizer         自定义的HttpClient定制器
     * @return 自定义的HttpHelper
     */
    public static HttpHelper createHttpHelper(int connRequestTimeout,
                                              int connTimeout,
                                              int socketTimeout,
                                              int maxConnPerRoute,
                                              int maxConnTotal,
                                              int timeToLive,
                                              boolean allowRetry,
                                              HttpRequestRetryHandler retryHandler,
                                              JobHttpClientFactory.HttpClientCustomizer customizer) {
        CloseableHttpClient httpClient = JobHttpClientFactory.createHttpClient(
            connRequestTimeout,
            connTimeout,
            socketTimeout,
            maxConnPerRoute,
            maxConnTotal,
            timeToLive,
            allowRetry,
            retryHandler,
            customizer);
        return new BaseHttpHelper(httpClient);
    }
}
