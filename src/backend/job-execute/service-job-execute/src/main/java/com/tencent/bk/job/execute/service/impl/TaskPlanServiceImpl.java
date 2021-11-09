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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.client.TaskPlanResourceClient;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskPlanServiceImpl implements TaskPlanService {
    private final TaskPlanResourceClient taskPlanResourceClient;

    @Autowired
    public TaskPlanServiceImpl(TaskPlanResourceClient taskPlanResourceClient) {
        this.taskPlanResourceClient = taskPlanResourceClient;
    }

    @Override
    public ServiceTaskPlanDTO getPlanById(Long appId, Long planId) {
        try {
            InternalResponse<ServiceTaskPlanDTO> resp = taskPlanResourceClient.getPlanById(appId, planId, false);
            log.info("Get plan by id, appId={}, planId={}, result={}", appId, planId, JsonUtils.toJson(resp));
            return resp.getData();
        } catch (Throwable e) {
            String errorMsg = "Get plan by id caught exception, appId=" + appId + ",planId=" + planId;
            log.warn(errorMsg, e);
            return null;
        }
    }

    @Override
    public String getPlanName(Long planId) {
        try {
            InternalResponse<String> resp = taskPlanResourceClient.getPlanName(planId);
            return resp.getData();
        } catch (Throwable e) {
            String errorMsg = "Get plan name by id caught exception, planId=" + planId;
            log.warn(errorMsg, e);
            return null;
        }
    }

    @Override
    public Long getPlanAppId(Long planId) {
        try {
            InternalResponse<Long> resp = taskPlanResourceClient.getPlanAppId(planId);
            log.info("Get planAppId by id, planId={}, result={}", planId, JsonUtils.toJson(resp));
            return resp.getData();
        } catch (Throwable e) {
            String errorMsg = "Get planAppId by id caught exception" + ",planId=" + planId;
            log.warn(errorMsg, e);
            return null;
        }
    }
}
