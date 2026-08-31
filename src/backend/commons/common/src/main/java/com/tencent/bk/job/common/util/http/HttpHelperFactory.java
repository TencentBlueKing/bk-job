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
        LONG_RETRYABLE_HTTP_CLIENT = createClient(true, new JobHttpRequestRetryHandler(), 35000, true);
        LONG_RETRYABLE_HTTP_CLIENT_INSECURE = createClient(true, new JobHttpRequestRetryHandler(), 35000, false);
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
        return new WatchableHttpHelper(httpHelper, () -> meterRegistry);
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

    /**
     * 创建自定义的HttpHelper，每创建一次都会生成一个常驻的连接驱逐线程，请在单例中使用，避免线程泄露
     *
     * @param customizer 自定义的HttpClient定制器
     * @return 自定义的HttpHelper
     */
    public static WatchableHttpHelper createHttpHelper(JobHttpClientFactory.HttpClientCustomizer customizer) {
        return createHttpHelper(customizer, JobHttpSslVerifyConfig.isGlobalVerifyEnabled());
    }

    /**
     * 创建自定义的HttpHelper，每创建一次都会生成一个常驻的连接驱逐线程，请在单例中使用，避免线程泄露
     *
     * @param customizer       自定义的HttpClient定制器
     * @param sslVerifyEnabled 是否校验 HTTPS 证书
     * @return 自定义的HttpHelper
     */
    public static WatchableHttpHelper createHttpHelper(JobHttpClientFactory.HttpClientCustomizer customizer,
                                                      boolean sslVerifyEnabled) {
        HttpHelper baseHttpHelper = createHttpHelper(
            15000,
            15000,
            15000,
            500,
            1000,
            60,
            false,
            null,
            customizer,
            sslVerifyEnabled
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
        return createHttpHelper(
            connRequestTimeout,
            connTimeout,
            socketTimeout,
            maxConnPerRoute,
            maxConnTotal,
            timeToLive,
            allowRetry,
            retryHandler,
            customizer,
            true);
    }

    /**
     * 创建自定义的HttpHelper，每创建一次都会生成一个常驻的连接驱逐线程，请在单例中使用，避免线程泄露
     *
     * @param sslVerifyEnabled 是否校验 HTTPS 证书
     */
    public static HttpHelper createHttpHelper(int connRequestTimeout,
                                              int connTimeout,
                                              int socketTimeout,
                                              int maxConnPerRoute,
                                              int maxConnTotal,
                                              int timeToLive,
                                              boolean allowRetry,
                                              HttpRequestRetryHandler retryHandler,
                                              JobHttpClientFactory.HttpClientCustomizer customizer,
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
            customizer,
            sslVerifyEnabled);
        return new BaseHttpHelper(httpClient);
    }
}
