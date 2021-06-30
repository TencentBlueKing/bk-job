/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.WriteConcernResolver;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MongoDBConfig {
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientCustomizer() {
        log.info("Init MongoClientSettingsBuilderCustomizer");
        Block<SocketSettings.Builder> socketSettings = builder -> builder.connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS);
        Block<ConnectionPoolSettings.Builder> connectionPoolSettings = builder -> builder.minSize(200).maxSize(500)
            .maxConnectionLifeTime(0, TimeUnit.SECONDS)
            .maxConnectionIdleTime(0, TimeUnit.SECONDS);
        return clientSettingsBuilder -> clientSettingsBuilder.writeConcern(WriteConcern.W1)
            .readConcern(ReadConcern.LOCAL)
            .readPreference(ReadPreference.primaryPreferred())
            .applyToSocketSettings(socketSettings)
            .applyToConnectionPoolSettings(connectionPoolSettings);

    }

    @Bean
    public WriteConcernResolver writeConcernResolver() {
        return action -> {
            log.info("Using Write Concern of Unacknowledged");
            return WriteConcern.W1;
        };
    }
}
