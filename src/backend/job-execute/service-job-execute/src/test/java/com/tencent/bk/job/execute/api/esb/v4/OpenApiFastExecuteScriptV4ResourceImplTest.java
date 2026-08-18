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

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 快速执行脚本 OpenAPI 的预检分支。
 * <p>
 * 审批链路的预检与放行走的是这同一个方法，只有 dryRun 取值不同，<b>这是"预检结果与实际执行不漂移"的
 * 结构保证</b>；预检必须只回带操作概要而不产生作业实例，否则审批还没通过脚本就已经跑了。
 */
class OpenApiFastExecuteScriptV4ResourceImplTest {

    private static final String USERNAME = "admin";
    private static final String APP_CODE = "bk_ai";

    private TaskExecuteService taskExecuteService;

    private OpenApiFastExecuteScriptV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        taskExecuteService = mock(TaskExecuteService.class);
        resource = new OpenApiFastExecuteScriptV4ResourceImpl(taskExecuteService);
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
        when(taskExecuteService.executeFastTask(any())).thenReturn(buildResolvedTaskInstance());

        EsbV4Response<V4JobExecuteDTO> response =
            resource.fastExecuteScript(USERNAME, APP_CODE, true, baseRequest());

        assertThat(response.getData()).isNull();
        assertThat(response.getDryRunSummary()).isNotNull();
        assertThat(response.getDryRunSummary().getTotalExecuteObjectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("预检时把未传而按默认生效的超时时间标进概要")
    void givenDryRunWithoutTimeoutThenMarkDefaultApplied() {
        when(taskExecuteService.executeFastTask(any())).thenReturn(buildResolvedTaskInstance());

        EsbV4Response<V4JobExecuteDTO> response =
            resource.fastExecuteScript(USERNAME, APP_CODE, true, baseRequest());

        // 用户没传超时时间，实际会按默认值执行，单据必须显式说明
        assertThat(response.getDryRunSummary().getDefaultsApplied()).hasSize(1);
        assertThat(response.getDryRunSummary().getDefaultsApplied().get(0).getValue())
            .isEqualTo(JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS + "s");
    }

    @Test
    @DisplayName("用户显式传了超时时间时不标为按默认生效")
    void givenDryRunWithTimeoutThenNoDefaultApplied() {
        when(taskExecuteService.executeFastTask(any())).thenReturn(buildResolvedTaskInstance());
        V4FastExecuteScriptRequest request = baseRequest();
        request.setTimeout(600);

        EsbV4Response<V4JobExecuteDTO> response =
            resource.fastExecuteScript(USERNAME, APP_CODE, true, request);

        assertThat(response.getDryRunSummary().getDefaultsApplied()).isNullOrEmpty();
    }

    @Test
    @DisplayName("dryRun 未传时按正式执行处理，返回作业实例而非概要")
    void givenNullDryRunThenExecute() {
        when(taskExecuteService.executeFastTask(any())).thenAnswer(invocation -> {
            // 真实执行链路会把作业实例 ID 回填到入参上
            FastTaskDTO fastTask = invocation.getArgument(0, FastTaskDTO.class);
            fastTask.getTaskInstance().setId(1000L);
            fastTask.getStepInstance().setId(2000L);
            return null;
        });

        EsbV4Response<V4JobExecuteDTO> response =
            resource.fastExecuteScript(USERNAME, APP_CODE, null, baseRequest());

        assertThat(response.getDryRunSummary()).isNull();
        assertThat(response.getData().getTaskInstanceId()).isEqualTo(1000L);
        assertThat(response.getData().getStepInstanceId()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("dryRun 标识透传到执行链路，由下游据此跳过写操作")
    void givenDryRunThenPassDownToExecuteChain() {
        when(taskExecuteService.executeFastTask(any())).thenReturn(buildResolvedTaskInstance());

        resource.fastExecuteScript(USERNAME, APP_CODE, true, baseRequest());

        verify(taskExecuteService).executeFastTask(argThat(fastTask -> Boolean.TRUE.equals(fastTask.getDryRun())));
    }

    private V4FastExecuteScriptRequest baseRequest() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setAppId(2L);
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("test_task");
        request.setContent(Base64Util.encodeContentToStr("echo 1"));
        request.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
        request.setAccountAlias("root");
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(101L);
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setHostList(Collections.singletonList(host));
        request.setExecuteTarget(executeTarget);
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
        stepInstance.setName("test_task");
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setAccountAlias("root");
        stepInstance.setTargetExecuteObjects(target);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("test_task");
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));
        return taskInstance;
    }
}
