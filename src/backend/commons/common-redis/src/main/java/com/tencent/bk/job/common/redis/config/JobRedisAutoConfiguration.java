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

package com.tencent.bk.job.common.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tencent.bk.job.common.properties.JobSslProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.SslVerifyMode;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.File;
import java.util.Optional;

@Slf4j
@EnableConfigurationProperties(JobRedisSslProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import({RedisLockConfig.class, RedisAutoConfiguration.class})
public class JobRedisAutoConfiguration {

    @Bean("jsonRedisTemplate")
    @Primary
    public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisSerializer<String> stringRedisSerializer = RedisSerializer.string();

        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);
        redisTemplate.setDefaultSerializer(jsonRedisSerializer);
        redisTemplate.setEnableDefaultSerializer(true);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer(
        JobRedisSslProperties jobRedisSslProperties
    ) {
        return clientConfigurationBuilder -> {
            Optional<ClientOptions> optional = clientConfigurationBuilder.build().getClientOptions();
            ClientOptions currentClientOptions;
            if (optional.isPresent()) {
                currentClientOptions = optional.get();
            } else {
                log.info("ClientOptions is null, init one");
                currentClientOptions = ClientOptions.builder().build();
            }
            JobSslProperties sslProperties = jobRedisSslProperties.getRedis();
            if (!sslProperties.isEnabled()) {
                log.info("SSL for redis is not enabled, use default options");
                return;
            }
            if (!sslProperties.isVerifyHostname()) {
                SslVerifyMode verifyMode = SslVerifyMode.CA;
                clientConfigurationBuilder.useSsl().verifyMode(verifyMode);
                log.info("Redis sslVerifyMode set: {}", verifyMode);
            }
            SslOptions sslOptions = createSslOptions(sslProperties);
            ClientOptions newClientOptions = currentClientOptions.mutate()
                // 设置自定义的 SslOptions
                .sslOptions(sslOptions)
                .build();
            clientConfigurationBuilder.clientOptions(newClientOptions);
        };
    }

    private SslOptions createSslOptions(JobSslProperties sslProperties) {
        String trustStoreType = sslProperties.getTrustStoreType();
        String keyStoreType = sslProperties.getKeyStoreType();
        if (!keyStoreType.equals(trustStoreType)) {
            log.warn(
                "trustStoreType must be same with keyStoreType for redis, " +
                    "but keyStoreType({}) is not the same with trustStoreType({}) now, " +
                    "both use keyStoreType({}) finally",
                keyStoreType,
                trustStoreType,
                keyStoreType
            );
        }
        SslOptions.Builder builder = SslOptions.builder().keyStoreType(keyStoreType);
        if (StringUtils.isNotBlank(sslProperties.getTrustStore())) {
            builder.truststore(new File(sslProperties.getTrustStore()), sslProperties.getTrustStorePassword());
        }
        if (StringUtils.isNotBlank(sslProperties.getKeyStore())) {
            builder.keystore(new File(sslProperties.getKeyStore()), sslProperties.getKeyStorePassword().toCharArray());
        }
        return builder.build();
    }
}
