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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.client.ServiceExecuteTaskResourceClient;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.execute.model.inner.ServiceTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import com.tencent.bk.job.execute.model.inner.request.ServiceTaskExecuteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @since 17/2/2020 19:59
 */
@Slf4j
@Service
public class ExecuteTaskServiceImpl implements ExecuteTaskService {

    private ServiceExecuteTaskResourceClient serviceExecuteTaskResourceClient;

    @Autowired
    public ExecuteTaskServiceImpl(ServiceExecuteTaskResourceClient serviceExecuteTaskResourceClient) {
        this.serviceExecuteTaskResourceClient = serviceExecuteTaskResourceClient;
    }

    @Override
    public InternalResponse<ServiceTaskExecuteResult> executeTask(long appId, long taskId, long cronTaskId,
                                                                  String cronName,
                                                                  List<ServiceTaskVariable> variableList,
                                                                  String operator) {

        try {
            ServiceTaskExecuteRequest request = new ServiceTaskExecuteRequest();
            request.setAppId(appId);
            request.setOperator(operator);
            request.setPlanId(taskId);
            request.setCronTaskId(cronTaskId);
            request.setTaskName(cronName);
            request.setTaskVariables(variableList);
            request.setStartupMode(3);
            request.setSkipAuth(true);
            if (log.isDebugEnabled()) {
                log.debug("Sending request to executor|{}", request);
            }

            InternalResponse<ServiceTaskExecuteResult> taskExecuteResult =
                serviceExecuteTaskResourceClient.executeTask(request);
            log.info("Get execute task by cron|appId|{}|taskId|{}|cronTaskId|{}|{}|operator|{}|result|{}", appId,
                taskId, cronTaskId, cronName, operator, JsonUtils.toJson(taskExecuteResult));
            return taskExecuteResult;
        } catch (Throwable e) {
            log.error("Get execute task by cron caught exception|appId|{}|taskId|{}|cronTaskId|{}|{}|operator|{}",
                appId, taskId, cronTaskId, cronName, operator, e);
            return null;
        }
    }

    @Override
    public void authExecuteTask(
        long appId,
        long taskId,
        long cronTaskId,
        String cronName,
        List<ServiceTaskVariable> variableList,
        String operator
    ) throws TaskExecuteAuthFailedException {
        ServiceTaskExecuteRequest request = new ServiceTaskExecuteRequest();
        request.setAppId(appId);
        request.setOperator(operator);
        request.setPlanId(taskId);
        request.setCronTaskId(cronTaskId);
        request.setTaskName(cronName);
        request.setTaskVariables(variableList);
        request.setStartupMode(3);
        if (log.isDebugEnabled()) {
            log.debug("Sending auth execute request to executor|{}", request);
        }

        InternalResponse<AuthResultDTO> authExecuteResult = serviceExecuteTaskResourceClient.authExecuteTask(request);
        log.info("Auth execute result|appId|{}|taskId|{}|cronTaskId|{}|{}|operator|{}|result|{}", appId,
            taskId, cronTaskId, cronName, operator, JsonUtils.toJson(authExecuteResult));
        if (authExecuteResult != null) {
            AuthResultDTO authResult = authExecuteResult.getData();
            if (authResult != null && !authResult.isPass()) {
                throw new TaskExecuteAuthFailedException(AuthResult.fromAuthResultDTO(authResult), null);
            }
        }
    }
}
