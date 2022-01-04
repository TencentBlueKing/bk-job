package com.tencent.bk.job.common.util.http;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.ssl.SSLContexts;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HttpHelperFactory {

    private static MeterRegistry meterRegistry;

    private static final String CHARSET = "UTF-8";

    private static final CloseableHttpClient DEFAULT_HTTP_CLIENT;
    private static final CloseableHttpClient RETRYABLE_HTTP_CLIENT;
    private static final CloseableHttpClient LONG_RETRYABLE_HTTP_CLIENT;

    private static CloseableHttpClient getHttpClient(int connRequestTimeout,
                                                     int connTimeout,
                                                     int socketTimeout,
                                                     boolean canRetry) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setBufferSize(102400)
                    .setCharset(Charset.forName(CHARSET))
                    .build()
            ).setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(connRequestTimeout)
                    .setConnectTimeout(connTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build()
            )
            // esb的keep-alive时间为90s，需要<90s,防止连接超时抛出org.apache.http.NoHttpResponseException:
            // The target server failed to respond
            .setConnectionTimeToLive(34, TimeUnit.SECONDS).evictExpiredConnections()
            .evictIdleConnections(5, TimeUnit.SECONDS)
            .disableAutomaticRetries()
            .disableAuthCaching()
            .disableCookieManagement()
            .setMaxConnPerRoute(500)
            .setMaxConnTotal(1000);
        if (canRetry) {
            httpClientBuilder.setRetryHandler(
                new StandardHttpRequestRetryHandler()
            );
        }
        CloseableHttpClient httpClient;
        try {
            httpClient = httpClientBuilder.setSSLSocketFactory(
                new SSLConnectionSocketFactory(
                    SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .build()
                )
            ).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("", e);
            httpClient = httpClientBuilder.build();
        }
        return httpClient;
    }

    static {
        DEFAULT_HTTP_CLIENT = getHttpClient(
            15000, 15000, 15000, false
        );
        RETRYABLE_HTTP_CLIENT = getHttpClient(
            15000, 15000, 15000, true
        );
        LONG_RETRYABLE_HTTP_CLIENT = getHttpClient(
            15000, 15000, 30000, true
        );
    }

    public static void setMeterRegistry(MeterRegistry meterRegistry) {
        HttpHelperFactory.meterRegistry = meterRegistry;
    }

    private static HttpHelper getWatchableHttpHelper(HttpHelper httpHelper) {
        return new WatchableHttpHelper(httpHelper, meterRegistry);
    }

    private static ExtHttpHelper getWatchableExtHelper(HttpHelper httpHelper) {
        HttpHelper watchableHttpHelper = getWatchableHttpHelper(httpHelper);
        return new ExtHttpHelper(watchableHttpHelper);
    }

    public static ExtHttpHelper getDefaultHttpHelper() {
        HttpHelper baseHttpHelper = new BaseHttpHelper(DEFAULT_HTTP_CLIENT);
        return getWatchableExtHelper(baseHttpHelper);
    }

    public static ExtHttpHelper getRetryableHttpHelper() {
        HttpHelper baseHttpHelper = new BaseHttpHelper(RETRYABLE_HTTP_CLIENT);
        return getWatchableExtHelper(baseHttpHelper);
    }

    public static ExtHttpHelper getLongRetryableHttpHelper() {
        HttpHelper baseHttpHelper = new BaseHttpHelper(LONG_RETRYABLE_HTTP_CLIENT);
        return getWatchableExtHelper(baseHttpHelper);
    }
}
