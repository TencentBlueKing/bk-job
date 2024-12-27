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

package com.tencent.bk.job.analysis.service.ai.context.impl;

import com.tencent.bk.job.analysis.service.ai.context.TaskContextService;
import com.tencent.bk.job.analysis.service.ai.context.model.ScriptTaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContextQuery;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.execute.api.inner.ServiceStepInstanceResource;
import com.tencent.bk.job.execute.model.inner.ServiceScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceStepInstanceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 任务上下文服务
 */
@Service
public class TaskContextServiceImpl implements TaskContextService {

    private final ServiceStepInstanceResource serviceStepInstanceResource;
    private final FileTaskContextService fileTaskContextService;

    @Autowired
    public TaskContextServiceImpl(ServiceStepInstanceResource serviceStepInstanceResource,
                                  FileTaskContextService fileTaskContextService) {
        this.serviceStepInstanceResource = serviceStepInstanceResource;
        this.fileTaskContextService = fileTaskContextService;
    }

    /**
     * 根据用户名与上下文查询条件获取对应的任务上下文
     *
     * @param username     用户名
     * @param contextQuery 上下文查询条件
     * @return 任务上下文
     */
    @Override
    public TaskContext getTaskContext(String username, TaskContextQuery contextQuery) {
        InternalResponse<ServiceStepInstanceDTO> resp = serviceStepInstanceResource.getStepInstance(
            username,
            contextQuery.getAppId(),
            contextQuery.getTaskInstanceId(),
            contextQuery.getStepInstanceId()
        );
        if (resp.isSuccess()) {
            ServiceStepInstanceDTO stepInstance = resp.getData();
            if (stepInstance.isScriptStep()) {
                return buildContextForScriptTask(stepInstance);
            } else if (stepInstance.isFileStep()) {
                return fileTaskContextService.getTaskContext(stepInstance, contextQuery);
            } else {
                throw new InvalidParamException(ErrorCode.AI_ANALYZE_ERROR_ONLY_SUPPORT_SCRIPT_OR_FILE_STEP);
            }
        }
        if (resp.getAuthResult() != null && !resp.getAuthResult().isPass()) {
            throw new PermissionDeniedException(AuthResult.fromAuthResultDTO(resp.getAuthResult()));
        }
        throw new ServiceException(resp.getErrorMsg(), ErrorType.valOf(resp.getErrorType()), resp.getCode());
    }

    /**
     * 构建脚本任务上下文
     *
     * @param stepInstance 步骤实例
     * @return 脚本任务上下文
     */
    private TaskContext buildContextForScriptTask(ServiceStepInstanceDTO stepInstance) {
        ServiceScriptStepInstanceDTO scriptStepInstance = stepInstance.getScriptStepInstance();
        ScriptTaskContext scriptTaskContext = new ScriptTaskContext(
            stepInstance.getName(),
            scriptStepInstance.getScriptType(),
            scriptStepInstance.getScriptContent(),
            scriptStepInstance.getScriptParam(),
            scriptStepInstance.isSecureParam()
        );
        return new TaskContext(stepInstance.getExecuteType(), stepInstance.getStatus(), scriptTaskContext, null);
    }
}
