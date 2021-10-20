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

package com.tencent.bk.job.execute.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class JobExecuteConfig {

    @Value("${swagger.url:swagger.job.com}")
    private String swaggerUrl;

    /**
     * 功能开关 - 启用账号鉴权
     */
    @Value("${feature.toggle.auth-account.mode:enabled}")
    private String enableAuthAccountMode;

    /**
     * 账号鉴权灰度业务(用,分隔)
     */
    @Value("${feature.toggle.auth-account.gray.apps:}")
    private String accountAuthGrayApps;

    @Value("${job.execute.result.handle.tasks.limit: 2000}")
    private int resultHandleTasksLimit;

    /**
     * 作业平台web访问地址
     */
    @Value("${job.web.url:}")
    private String jobWebUrl;

    /**
     * Symmetric encryption password
     */
    @Value("${job.encrypt.password}")
    private String encryptPassword;

    @Value("${job.execute.limit.file-task.max-tasks:100000}")
    private Integer fileTasksMax;

    @Value("${job.execute.limit.script-task.max-target-server:50000}")
    private Integer scriptTaskMaxTargetServer;
}
