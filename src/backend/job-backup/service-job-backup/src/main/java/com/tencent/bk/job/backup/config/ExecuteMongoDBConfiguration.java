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

package com.tencent.bk.job.backup.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.tencent.bk.job.backup.archive.JobExecuteLogArchivers;
import com.tencent.bk.job.backup.archive.impl.JobFileLogArchiver;
import com.tencent.bk.job.backup.archive.impl.JobScriptLogArchiver;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration("executeMongoDBConfiguration")
@ConditionalOnExpression("${job.backup.archive.execute-log.enabled:false}")
public class ExecuteMongoDBConfiguration {

    @Value("${spring.datasource.job-execute-mongodb.uri:}")
    private String mongoUri;

    @Value("${spring.datasource.job-execute-mongodb.database:joblog}")
    private String database;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, database);
    }

    @Bean
    public JobScriptLogArchiver jobScriptLogArchiver(MongoTemplate mongoTemplate,
                                                     ArchiveTaskService archiveTaskService,
                                                     ExecuteLogArchiveProperties archiveProperties) {
        return new JobScriptLogArchiver(mongoTemplate,
            archiveTaskService,
            archiveProperties);
    }

    @Bean
    public JobFileLogArchiver jobFileLogArchiver(MongoTemplate mongoTemplate,
                                                   ArchiveTaskService archiveTaskService,
                                                 ExecuteLogArchiveProperties archiveProperties) {
        return new JobFileLogArchiver(mongoTemplate,
            archiveTaskService,
            archiveProperties);
    }

    @Bean
    public JobExecuteLogArchivers jobExecuteLogArchivers(JobFileLogArchiver jobFileLogArchiver,
                                                         JobScriptLogArchiver jobScriptLogArchiver) {
        return new JobExecuteLogArchivers(jobFileLogArchiver,
            jobScriptLogArchiver);
    }
}
