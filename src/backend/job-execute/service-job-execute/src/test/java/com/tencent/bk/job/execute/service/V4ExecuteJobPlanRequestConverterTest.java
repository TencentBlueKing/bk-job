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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * v4 启动执行方案请求转换的字段级对账单测。
 */
class V4ExecuteJobPlanRequestConverterTest {

    private static final long APP_ID = 2L;
    private static final String APP_CODE = "bk_ai";
    private static final User OPERATOR = new User("tenant_a", "admin", "管理员");

    @Test
    @DisplayName("v4 启动执行方案请求逐字段转换为作业执行参数")
    void convertAllFields() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setAppId(APP_ID);
        request.setPlanId(1000L);
        request.setCallbackUrl("http://127.0.0.1/callback");
        request.setStartTask(false);
        request.setGlobalVars(Arrays.asList(stringVar(), hostVar()));

        TaskExecuteParam param = V4ExecuteJobPlanRequestConverter.convert(request, OPERATOR, APP_CODE, false);

        assertThat(param.getAppId()).isEqualTo(APP_ID);
        assertThat(param.getPlanId()).isEqualTo(1000L);
        assertThat(param.getOperator()).isEqualTo(OPERATOR);
        assertThat(param.getAppCode()).isEqualTo(APP_CODE);
        assertThat(param.getCallbackUrl()).isEqualTo("http://127.0.0.1/callback");
        assertThat(param.getStartupMode()).isEqualTo(TaskStartupModeEnum.API);
        assertThat(param.getStartTask()).isFalse();
        assertThat(param.isDryRun()).isFalse();
        // 新入口不承接定时任务触发场景，cronTaskId 固定为空，因此 skipAuth 必然不生效
        assertThat(param.getCronTaskId()).isNull();
        assertThat(param.isSkipAuth()).isFalse();

        assertThat(param.getExecuteVariableValues()).hasSize(2);
        TaskVariableDTO stringVariable = param.getExecuteVariableValues().get(0);
        assertThat(stringVariable.getId()).isEqualTo(1L);
        assertThat(stringVariable.getName()).isEqualTo("var_str");
        assertThat(stringVariable.getValue()).isEqualTo("v1");
        assertThat(stringVariable.getExecuteTarget()).isNull();

        TaskVariableDTO hostVariable = param.getExecuteVariableValues().get(1);
        assertThat(hostVariable.getId()).isEqualTo(2L);
        assertThat(hostVariable.getName()).isEqualTo("var_host");
        assertThat(hostVariable.getValue()).isNull();
        assertThat(hostVariable.getExecuteTarget()).isNotNull();
        assertThat(hostVariable.getExecuteTarget().getStaticIpList()).hasSize(1);
        assertThat(hostVariable.getExecuteTarget().getStaticIpList().get(0).getHostId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("不传全局变量时转换出空的变量列表")
    void convertWithoutGlobalVars() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setAppId(APP_ID);
        request.setPlanId(1000L);

        TaskExecuteParam param = V4ExecuteJobPlanRequestConverter.convert(request, OPERATOR, APP_CODE, true);

        assertThat(param.getExecuteVariableValues()).isEmpty();
        assertThat(param.isDryRun()).isTrue();
    }

    @Test
    @DisplayName("执行方案 ID 为空时判为非法参数")
    void validatePlanIdRequired() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setAppId(APP_ID);

        assertThatThrownBy(() -> V4ExecuteJobPlanRequestConverter.convert(request, OPERATOR, APP_CODE, true))
            .isInstanceOf(InvalidParamException.class)
            .extracting(e -> ((InvalidParamException) e).getErrorCode())
            .isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME);
    }

    @Test
    @DisplayName("全局变量的 id 与 name 同时为空时判为非法参数")
    void validateGlobalVarIdOrNameRequired() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setAppId(APP_ID);
        request.setPlanId(1000L);
        V4GlobalVarDTO globalVar = new V4GlobalVarDTO();
        globalVar.setValue("v1");
        request.setGlobalVars(Collections.singletonList(globalVar));

        assertThatThrownBy(() -> V4ExecuteJobPlanRequestConverter.convert(request, OPERATOR, APP_CODE, true))
            .isInstanceOf(InvalidParamException.class);
    }

    private V4GlobalVarDTO stringVar() {
        V4GlobalVarDTO globalVar = new V4GlobalVarDTO();
        globalVar.setId(1L);
        globalVar.setName("var_str");
        globalVar.setValue("v1");
        return globalVar;
    }

    private V4GlobalVarDTO hostVar() {
        V4GlobalVarDTO globalVar = new V4GlobalVarDTO();
        globalVar.setId(2L);
        globalVar.setName("var_host");
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(101L);
        executeTarget.setHostList(Collections.singletonList(host));
        globalVar.setExecuteTarget(executeTarget);
        return globalVar;
    }
}
