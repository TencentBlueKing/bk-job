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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v4 快速执行脚本请求转换的字段级对账单测。
 * <p>
 * 该转换从 ESB v4 Impl 抽取而来，抽取后 ESB 直接执行链路与审批预检链路共用它，
 * 这里逐字段断言，防止抽取过程中丢字段或改语义。
 */
class V4FastExecuteScriptRequestConverterTest {

    private static final long APP_ID = 2L;
    private static final String APP_CODE = "bk_ai";
    private static final User OPERATOR = new User("tenant_a", "admin", "管理员");

    @Test
    @DisplayName("v4 快速执行脚本请求逐字段转换为快速任务")
    void convertAllFields() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setAppId(APP_ID);
        request.setName("test_task");
        request.setContent(Base64Util.encodeContentToStr("echo 1"));
        request.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
        request.setScriptParam(Base64Util.encodeContentToStr("a\nb"));
        request.setAccountId(1000L);
        request.setAccountAlias("root");
        request.setParamSensitive(true);
        request.setWindowsInterpreter("  C:\\python.exe  ");
        request.setTimeout(300);
        request.setCallbackUrl("http://127.0.0.1/callback");
        request.setStartTask(false);
        request.setExecuteTarget(buildExecuteTarget());

        FastTaskDTO fastTask = V4FastExecuteScriptRequestConverter.convert(request, OPERATOR, APP_CODE, false);

        TaskInstanceDTO taskInstance = fastTask.getTaskInstance();
        assertThat(taskInstance.getName()).isEqualTo("test_task");
        assertThat(taskInstance.getAppId()).isEqualTo(APP_ID);
        assertThat(taskInstance.getAppCode()).isEqualTo(APP_CODE);
        assertThat(taskInstance.getOperator()).isEqualTo("admin");
        assertThat(taskInstance.getType()).isEqualTo(TaskTypeEnum.SCRIPT.getValue());
        assertThat(taskInstance.getStartupMode()).isEqualTo(TaskStartupModeEnum.API.getValue());
        assertThat(taskInstance.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(taskInstance.getPlanId()).isEqualTo(-1L);
        assertThat(taskInstance.getCronTaskId()).isEqualTo(-1L);
        assertThat(taskInstance.getTaskTemplateId()).isEqualTo(-1L);
        assertThat(taskInstance.getCurrentStepInstanceId()).isEqualTo(0L);
        assertThat(taskInstance.isDebugTask()).isFalse();
        assertThat(taskInstance.getCallbackUrl()).isEqualTo("http://127.0.0.1/callback");

        StepInstanceDTO stepInstance = fastTask.getStepInstance();
        assertThat(stepInstance.getName()).isEqualTo("test_task");
        assertThat(stepInstance.getAppId()).isEqualTo(APP_ID);
        assertThat(stepInstance.getStepId()).isEqualTo(-1L);
        assertThat(stepInstance.getExecuteType()).isEqualTo(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        assertThat(stepInstance.getScriptContent()).isEqualTo("echo 1");
        assertThat(stepInstance.getScriptType()).isEqualTo(ScriptTypeEnum.SHELL);
        // 脚本参数里的换行必须转成空格，否则脚本执行报错
        assertThat(stepInstance.getScriptParam()).isEqualTo("a b");
        assertThat(stepInstance.isSecureParam()).isTrue();
        assertThat(stepInstance.getWindowsInterpreter()).isEqualTo("C:\\python.exe");
        assertThat(stepInstance.getTimeout()).isEqualTo(300);
        assertThat(stepInstance.getAccountId()).isEqualTo(1000L);
        assertThat(stepInstance.getAccountAlias()).isEqualTo("root");
        assertThat(stepInstance.getOperator()).isEqualTo("admin");
        assertThat(stepInstance.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(stepInstance.getTargetExecuteObjects().getStaticIpList()).hasSize(1);
        assertThat(stepInstance.getTargetExecuteObjects().getStaticIpList().get(0).getHostId()).isEqualTo(101L);
        assertThat(stepInstance.getTargetExecuteObjects().getDynamicServerGroups()).hasSize(1);

        assertThat(fastTask.getOperator()).isEqualTo(OPERATOR);
        assertThat(fastTask.getStartTask()).isFalse();
        assertThat(fastTask.getDryRun()).isFalse();
        assertThat(fastTask.getRollingConfig()).isNull();
    }

    @Test
    @DisplayName("脚本版本 ID 优先于脚本 ID，脚本 ID 优先于脚本内容")
    void convertScriptSourcePriority() {
        V4FastExecuteScriptRequest request = baseRequest();
        request.setScriptVersionId(66L);
        request.setScriptId("script-id");
        request.setContent(Base64Util.encodeContentToStr("echo 1"));

        StepInstanceDTO stepInstance =
            V4FastExecuteScriptRequestConverter.convert(request, OPERATOR, APP_CODE, false).getStepInstance();

        assertThat(stepInstance.getScriptVersionId()).isEqualTo(66L);
        assertThat(stepInstance.getScriptId()).isNull();
        assertThat(stepInstance.getScriptContent()).isNull();
    }

    @Test
    @DisplayName("不传超时时间时用默认超时时间")
    void convertWithDefaultTimeout() {
        V4FastExecuteScriptRequest request = baseRequest();

        StepInstanceDTO stepInstance =
            V4FastExecuteScriptRequestConverter.convert(request, OPERATOR, APP_CODE, false).getStepInstance();

        assertThat(stepInstance.getTimeout()).isEqualTo(JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS);
    }

    @Test
    @DisplayName("dryRun 标记透传到快速任务")
    void convertWithDryRun() {
        FastTaskDTO fastTask =
            V4FastExecuteScriptRequestConverter.convert(baseRequest(), OPERATOR, APP_CODE, true);

        assertThat(fastTask.getDryRun()).isTrue();
    }

    private V4FastExecuteScriptRequest baseRequest() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setAppId(APP_ID);
        request.setName("test_task");
        request.setAccountAlias("root");
        request.setExecuteTarget(buildExecuteTarget());
        return request;
    }

    private V4ExecuteTargetDTO buildExecuteTarget() {
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(101L);
        executeTarget.setHostList(Collections.singletonList(host));
        EsbDynamicGroupDTO dynamicGroup = new EsbDynamicGroupDTO();
        dynamicGroup.setId("group-1");
        executeTarget.setDynamicGroups(Collections.singletonList(dynamicGroup));
        return executeTarget;
    }
}
