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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.execute.model.inner.ServiceStepInstanceDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 步骤实例API-服务内部调用
 */
@Tag(name = "StepInstance")
@SmartFeignClient(value = "job-execute", contextId = "stepInstanceResource")
@InternalAPI
public interface ServiceStepInstanceResource {
    @GetMapping("/service/app/{appId}/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}")
    InternalResponse<ServiceStepInstanceDTO> getStepInstance(
        @RequestHeader("username")
        String username,
        @Parameter(description = "作业平台业务ID", required = true)
        @PathVariable(value = "appId")
        Long appId,
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
        Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
        Long stepInstanceId);

    /**
     * 判断指定执行次数、滚动批次（及文件任务的上下行模式）下是否存在对应执行对象任务。
     * 不做任务查看权限校验，仅基于步骤与任务数据做存在性判断；调用方须已在业务侧完成鉴权。
     */
    @GetMapping("/service/app/{appId}/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}" +
        "/executeObjectTask/exists")
    InternalResponse<Boolean> executeObjectTaskExists(
        @PathVariable(value = "appId")
        Long appId,
        @PathVariable("taskInstanceId")
        Long taskInstanceId,
        @PathVariable("stepInstanceId")
        Long stepInstanceId,
        @Parameter(description = "执行次数", required = true)
        @RequestParam("executeCount")
        Integer executeCount,
        @Parameter(description = "滚动批次，非滚动步骤可为空")
        @RequestParam(value = "batch", required = false)
        Integer batch,
        @Parameter(description = "执行对象类型", required = true)
        @RequestParam("executeObjectType")
        Integer executeObjectType,
        @Parameter(description = "执行对象资源ID", required = true)
        @RequestParam("executeObjectResourceId")
        Long executeObjectResourceId,
        @Parameter(description = "文件任务模式：0-上传，1-下载；脚本步骤不必传")
        @RequestParam(value = "fileTaskMode", required = false)
        Integer fileTaskMode
    );
}
