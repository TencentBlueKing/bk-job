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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceStepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ServiceStepInstanceResourceImpl implements ServiceStepInstanceResource {

    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final StepInstanceService stepInstanceService;

    @Autowired
    public ServiceStepInstanceResourceImpl(TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                           StepInstanceService stepInstanceService) {
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    public InternalResponse<ServiceStepInstanceDTO> getStepInstance(String username,
                                                                    Long appId,
                                                                    Long taskInstanceId,
                                                                    Long stepInstanceId) {
        try {
            StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(appId,
                taskInstanceId, stepInstanceId);
            taskInstanceAccessProcessor.processBeforeAccess(username, appId, stepInstance.getTaskInstanceId());
            return InternalResponse.buildSuccessResp(stepInstance.toServiceStepInstanceDTO());
        } catch (NotFoundException e) {
            return InternalResponse.buildCommonFailResp(e);
        } catch (PermissionDeniedException e) {
            AuthResultDTO authResult = AuthResult.toAuthResultDTO(e.getAuthResult());
            return InternalResponse.buildAuthFailResp(authResult);
        }
    }
}
