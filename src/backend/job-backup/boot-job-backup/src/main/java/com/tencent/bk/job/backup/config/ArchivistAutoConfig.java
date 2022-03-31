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

import com.tencent.bk.job.backup.archive.JobExecuteArchiveManage;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.JobExecuteDAO;
import com.tencent.bk.job.backup.dao.impl.ExecuteArchiveDAOImpl;
import com.tencent.bk.job.backup.dao.impl.JobExecuteDAOImpl;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Slf4j
public class ArchivistAutoConfig {

    @Bean(name = "execute-read-dao")
    @ConditionalOnExpression("${job.execute.archive.enabled:false} || ${job.execute.archive.delete.enabled:false}")
    public JobExecuteDAO executeReadDAO(@Qualifier("job-execute-dsl-context") DSLContext context,
                                        ArchiveConfig archiveConfig) {
        log.info("Init JobExecuteDAO");
        return new JobExecuteDAOImpl(context, archiveConfig);
    }

    @Bean(name = "execute-archive-dao")
    @ConditionalOnExpression("${job.execute.archive.enabled:false}")
    public ExecuteArchiveDAO executeArchiveDAO(@Qualifier("job-execute-archive-dsl-context") DSLContext context) {
        log.info("Init ExecuteArchiveDAO");
        return new ExecuteArchiveDAOImpl(context);
    }

    @Bean
    @ConditionalOnExpression("${job.execute.archive.enabled:false} || ${job.execute.archive.delete.enabled:false}")
    public JobExecuteArchiveManage archiveExecuteLogExecutor(@Autowired(required = false) JobExecuteDAO jobExecuteDAO,
                                                             @Autowired(required = false) ExecuteArchiveDAO executeArchiveDAO,
                                                             ArchiveProgressService archiveProgressService,
                                                             ArchiveConfig archiveConfig) {
        log.info("Init JobExecuteArchiveManage");
        return new JobExecuteArchiveManage(jobExecuteDAO, executeArchiveDAO, archiveProgressService, archiveConfig);
    }
}
