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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 启动执行方案 OpenAPI 的预检分支。
 * <p>
 * 审批链路的预检与放行走的是这同一个方法，只有 dryRun 取值不同，<b>这是"预检结果与实际执行不漂移"的
 * 结构保证</b>；预检必须只回带操作概要而不产生作业实例，否则审批还没通过方案就已经跑了。
 */
class OpenApiExecuteJobPlanV4ResourceImplTest {

    private static final String USERNAME = "admin";
    private static final String APP_CODE = "bk_ai";

    private TaskExecuteService taskExecuteService;

    private OpenApiExecuteJobPlanV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        taskExecuteService = mock(TaskExecuteService.class);
        resource = new OpenApiExecuteJobPlanV4ResourceImpl(taskExecuteService);
        // 操作人由网关鉴权后经拦截器写入上下文，实现类只从上下文取
        JobContextUtil.setUser(new User("tenant_a", USERNAME, USERNAME));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("预检时只回带操作概要，data 为空")
    void givenDryRunThenReturnSummaryOnly() {
        when(taskExecuteService.executeJobPlan(any())).thenReturn(buildResolvedTaskInstance());

        EsbV4Response<V4JobExecuteDTO> response =
            resource.executeJobPlan(USERNAME, APP_CODE, true, baseRequest());

        assertThat(response.getData()).isNull();
        assertThat(response.getDryRunSummary()).isNotNull();
        assertThat(response.getDryRunSummary().getTotalExecuteObjectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("dryRun 未传时按正式执行处理，返回作业实例而非概要")
    void givenNullDryRunThenExecute() {
        TaskInstanceDTO taskInstance = buildResolvedTaskInstance();
        taskInstance.setId(1000L);
        when(taskExecuteService.executeJobPlan(any())).thenReturn(taskInstance);

        EsbV4Response<V4JobExecuteDTO> response =
            resource.executeJobPlan(USERNAME, APP_CODE, null, baseRequest());

        assertThat(response.getDryRunSummary()).isNull();
        assertThat(response.getData().getTaskInstanceId()).isEqualTo(1000L);
        // 执行方案由多个步骤组成，没有单一步骤实例可返回
        assertThat(response.getData().getStepInstanceId()).isNull();
    }

    @Test
    @DisplayName("dryRun 标识透传到执行链路，由下游据此跳过写操作")
    void givenDryRunThenPassDownToExecuteChain() {
        when(taskExecuteService.executeJobPlan(any())).thenReturn(buildResolvedTaskInstance());

        resource.executeJobPlan(USERNAME, APP_CODE, true, baseRequest());

        verify(taskExecuteService).executeJobPlan(argThat(TaskExecuteParam::isDryRun));
    }

    @Test
    @DisplayName("执行方案 ID 非法时在进入执行链路前就拒绝")
    void givenInvalidPlanIdThenRejectBeforeExecute() {
        V4ExecuteJobPlanRequest request = baseRequest();
        request.setPlanId(0L);

        assertThatThrownBy(() -> resource.executeJobPlan(USERNAME, APP_CODE, true, request))
            .isInstanceOf(InvalidParamException.class);

        verify(taskExecuteService, never()).executeJobPlan(any());
    }

    private V4ExecuteJobPlanRequest baseRequest() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setAppId(2L);
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setPlanId(100L);
        return request;
    }

    /**
     * 模拟预检返回的作业实例：执行对象已在 dryRun 返回点之前解析完成
     */
    private TaskInstanceDTO buildResolvedTaskInstance() {
        HostDTO host = new HostDTO();
        host.setHostId(101L);
        host.setBkCloudId(0L);
        host.setIp("127.0.0.1");
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setExecuteObjects(Collections.singletonList(new ExecuteObject(host)));

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("step_1");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setAccountAlias("root");
        stepInstance.setTargetExecuteObjects(target);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("test_plan");
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));
        return taskInstance;
    }
}
