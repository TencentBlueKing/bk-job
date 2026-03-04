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

package com.tencent.bk.job.analysis.api.esb.v4;

import com.tencent.bk.job.analysis.model.esb.v4.resp.TaskContextForSingleExecuteObjectDTO;
import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.validation.CheckEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RequestMapping("/esb/api/v4")
@EsbV4API
@RestController
@Validated
public interface OpenApiGetTaskContextV4Resource {

    @GetMapping("/get_script_task_context")
    EsbV4Response<TaskContextForSingleExecuteObjectDTO> getScriptTaskContext(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_scope_type")
        @NotNull(message = "{validation.constraints.EmptyScopeType.message}")
        @CheckEnum(
            enumClass = ResourceScopeTypeEnum.class,
            message = "{validation.constraints.InvalidValueScopeType.message}"
        )
        String scopeType,
        @RequestParam(value = "bk_scope_id")
        @NotBlank(message = "{validation.constraints.EmptyScopeId.message}")
        String scopeId,
        @RequestParam(value = "job_instance_id")
        @NotNull(message = "{validation.constraints.InvalidJobInstanceId.message}")
        @Min(value = 1L, message = "{validation.constraints.InvalidJobInstanceId.message}")
        Long taskInstanceId,
        @RequestParam(value = "step_instance_id")
        @NotNull(message = "{validation.constraints.InvalidStepInstanceId.message}")
        @Min(value = 1L, message = "{validation.constraints.InvalidStepInstanceId.message}")
        Long stepInstanceId,
        @RequestParam(value = "execute_count")
        @NotNull(message = "{validation.constraints.InvalidExecuteCount.message}")
        @Min(value = 0L, message = "{validation.constraints.InvalidExecuteCount.message}")
        Integer executeCount,
        @RequestParam(value = "batch", required = false)
        Integer batch,
        @RequestParam(value = "execute_object_type")
        @NotNull(message = "{validation.constraints.InvalidExecuteObjectType.message}")
        Integer executeObjectType,
        @RequestParam(value = "execute_object_resource_id")
        @NotNull(message = "{validation.constraints.InvalidExecuteObjectResourceId.message}")
        @Min(value = 1L, message = "{validation.constraints.InvalidExecuteObjectResourceId.message}")
        Long executeObjectResourceId,
        @RequestParam(value = "content", required = false)
        String content
    );

    @GetMapping("/get_file_task_context")
    EsbV4Response<TaskContextForSingleExecuteObjectDTO> getFileTaskContext(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_scope_type")
        @NotNull(message = "{validation.constraints.EmptyScopeType.message}")
        @CheckEnum(
            enumClass = ResourceScopeTypeEnum.class,
            message = "{validation.constraints.InvalidValueScopeType.message}"
        )
        String scopeType,
        @RequestParam(value = "bk_scope_id")
        @NotBlank(message = "{validation.constraints.EmptyScopeId.message}")
        String scopeId,
        @RequestParam(value = "job_instance_id")
        @NotNull(message = "{validation.constraints.InvalidJobInstanceId.message}")
        @Min(value = 1L, message = "{validation.constraints.InvalidJobInstanceId.message}")
        Long taskInstanceId,
        @RequestParam(value = "step_instance_id")
        @NotNull(message = "{validation.constraints.InvalidStepInstanceId.message}")
        @Min(value = 1L, message = "{validation.constraints.InvalidStepInstanceId.message}")
        Long stepInstanceId,
        @RequestParam(value = "execute_count")
        @NotNull(message = "{validation.constraints.InvalidExecuteCount.message}")
        @Min(value = 0L, message = "{validation.constraints.InvalidExecuteCount.message}")
        Integer executeCount,
        @RequestParam(value = "batch", required = false)
        Integer batch,
        @RequestParam(value = "execute_object_type")
        @NotNull(message = "{validation.constraints.InvalidExecuteObjectType.message}")
        Integer executeObjectType,
        @RequestParam(value = "execute_object_resource_id")
        @NotNull(message = "{validation.constraints.InvalidExecuteObjectResourceId.message}")
        @Min(value = 1L, message = "{validation.constraints.InvalidExecuteObjectResourceId.message}")
        Long executeObjectResourceId,
        @RequestParam(value = "mode")
        @NotNull(message = "{validation.constraints.InvalidFileTaskMode.message}")
        Integer mode
    );

}
