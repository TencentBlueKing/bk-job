/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.logsvr.config;

import com.mongodb.Block;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.tencent.bk.job.common.properties.JobSslProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@EnableConfigurationProperties({JobMongoSslProperties.class})
public class MongoDBConfig {
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientCustomizer(JobMongoSslProperties mongoSslProperties) {
        log.info("Init MongoClientSettingsBuilderCustomizer");
        JobSslProperties jobMongoSslProperties = mongoSslProperties.getMongodb();
        Block<SocketSettings.Builder> socketSettings = builder -> builder
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS);
        Block<ConnectionPoolSettings.Builder> connectionPoolSettings = builder -> builder
            .minSize(200)
            .maxSize(500)
            .maxConnectionLifeTime(0, TimeUnit.SECONDS)
            .maxConnectionIdleTime(0, TimeUnit.SECONDS);
        return clientSettingsBuilder -> {
            clientSettingsBuilder
                .writeConcern(WriteConcern.W1)
                .readConcern(ReadConcern.LOCAL)
                .readPreference(ReadPreference.primaryPreferred())
                .applyToSocketSettings(socketSettings)
                .applyToConnectionPoolSettings(connectionPoolSettings);

            // 根据配置文件判断是否开启ssl
            if (jobMongoSslProperties.isEnabled()) {
                clientSettingsBuilder.applyToSslSettings(ssl -> {
                    ssl.enabled(true);
                    ssl.invalidHostNameAllowed(!jobMongoSslProperties.isVerifyHostname());

                    try {
                        // 根据配置判断使用单向还是双向TLS
                        if (jobMongoSslProperties.isMutualTlsConfigured()) {
                            log.info("Detected client certificate configuration - enabling mutual TLS");
                            ssl.context(createMutualTlsSslContext(jobMongoSslProperties));
                        } else {
                            log.info("Using one-way TLS configuration");
                            ssl.context(createOnewayTlsSslContext(jobMongoSslProperties));
                        }
                    } catch (Exception e) {
                        log.error("Failed to configure MongoDB TLS context", e);
                        throw new RuntimeException("Failed to configure MongoDB TLS context", e);
                    }
                });
            }
        };
    }

    @Bean
    public WriteConcernResolver writeConcernResolver() {
        return action -> {
            log.info("Using Write Concern of Unacknowledged");
            return WriteConcern.W1;
        };
    }

    /**
     * 创建单向TLS的SSLContext
     */
    private SSLContext createOnewayTlsSslContext(JobSslProperties sslProps) throws Exception {
        TrustManager[] trustManagers = getTrustManagers(sslProps);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());

        return sslContext;
    }

    /**
     * 创建双向TLS的SSLContext
     */
    private SSLContext createMutualTlsSslContext(JobSslProperties sslProps) throws Exception {
        TrustManager[] trustManagers = getTrustManagers(sslProps);
        KeyManager[] keyManagers = getKeyManagers(sslProps);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        return sslContext;
    }

    private TrustManager[] getTrustManagers(JobSslProperties sslProps) throws Exception {
        TrustManager[] trustManagers;
        if (StringUtils.hasText(sslProps.getTrustStore())) {
            log.info("Loading mongo client trust store: {}", sslProps.getTrustStore());
            KeyStore trustStore = KeyStore.getInstance(sslProps.getTrustStoreType());
            try (FileInputStream fis = new FileInputStream(sslProps.getTrustStore())) {
                trustStore.load(fis, sslProps.getTrustStorePassword().toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();
        } else {
            // fallback至JVM默认信任库
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            trustManagers = tmf.getTrustManagers();
        }
        return trustManagers;
    }

    private KeyManager[] getKeyManagers(JobSslProperties sslProps) throws Exception {
        log.info("Loading mongo client key store: {}", sslProps.getKeyStore());
        KeyStore keyStore = KeyStore.getInstance(sslProps.getKeyStoreType());
        try (FileInputStream fis = new FileInputStream(sslProps.getKeyStore())) {
            keyStore.load(fis, sslProps.getKeyStorePassword().toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, sslProps.getKeyStorePassword().toCharArray());
        return kmf.getKeyManagers();
    }
}
