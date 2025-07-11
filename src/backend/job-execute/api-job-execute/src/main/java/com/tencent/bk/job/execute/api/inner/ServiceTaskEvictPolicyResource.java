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
import com.tencent.bk.job.execute.model.inner.ComposedTaskEvictPolicyDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"job-execute:service:TaskEvictPolicys"})
@RestController
@InternalAPI
public interface ServiceTaskEvictPolicyResource {

    @ApiOperation(value = "获取当前使用的任务驱逐策略", produces = "application/json")
    @GetMapping("/service/task-evict-policys/current")
    InternalResponse<ComposedTaskEvictPolicyDTO> getCurrentPolicy();

    @ApiOperation(value = "设置任务驱逐策略，参考值：{\"@type\":\"ComposedTaskEvictPolicy\",\"operator\":\"OR\"," +
        "\"policyList\":[{\"@type\":\"TaskInstanceIdEvictPolicy\",\"taskInstanceIdsToEvict\":[1001,1002]}," +
        "{\"@type\":\"AppIdTaskEvictPolicy\",\"appIdsToEvict\":[2,3]},{\"@type\":\"AppCodeTaskEvictPolicy\"," +
        "\"appCodesToEvict\":[\"appCode1\",\"appCode2\"]}]}",
        produces = "application/json")
    @PutMapping("/service/task-evict-policys")
    InternalResponse<Boolean> setPolicy(@RequestBody ComposedTaskEvictPolicyDTO policyDTO);

    @ApiOperation(value = "清除所有任务驱逐策略", produces = "application/json")
    @DeleteMapping("/service/task-evict-policys/clear")
    InternalResponse<Boolean> clearPolicy();

}
