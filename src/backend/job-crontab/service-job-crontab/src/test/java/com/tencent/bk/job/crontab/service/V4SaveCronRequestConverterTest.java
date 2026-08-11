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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ContainerFilter;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v4 保存定时任务请求转换的字段级对账单测。
 * <p>
 * 该转换是从零新写的（仓库里没有 v4 直接保存定时任务接口），审批预检与放行两次都用它。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class V4SaveCronRequestConverterTest {

    private static final long APP_ID = 2L;
    private static final long PLAN_ID = 1000L;
    private static final User OPERATOR = new User("tenant_a", "admin", "管理员");

    @Mock
    private ServiceTaskPlanResource taskPlanResource;
    @Mock
    private CommonAppService commonAppService;

    private V4SaveCronRequestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new V4SaveCronRequestConverter(taskPlanResource, commonAppService);
        when(commonAppService.getAppTimeZoneById(anyLong())).thenReturn("Asia/Shanghai");
    }

    @Test
    @DisplayName("v4 新建定时任务请求逐字段转换为定时任务信息")
    void convertCreateAllFields() {
        V4SaveCronRequest request = createRequest();
        request.setExecuteTimeZone("Europe/London");
        stubPlanVariable(1L, "var_str", TaskVariableTypeEnum.STRING.getType());

        CronJobInfoDTO cronJobInfo = converter.convert(request, OPERATOR);

        assertThat(cronJobInfo.getId()).isNull();
        assertThat(cronJobInfo.getAppId()).isEqualTo(APP_ID);
        assertThat(cronJobInfo.getName()).isEqualTo("test_cron");
        assertThat(cronJobInfo.getTaskPlanId()).isEqualTo(PLAN_ID);
        // 分 时 日 月 周 的 5 段 Linux 表达式补齐为 quartz 的 6 段
        assertThat(cronJobInfo.getCronExpression()).startsWith("0 0/5 * * *");
        assertThat(cronJobInfo.getExecuteTimeZone()).isEqualTo("Europe/London");
        assertThat(cronJobInfo.getCreator()).isEqualTo("admin");
        assertThat(cronJobInfo.getLastModifyUser()).isEqualTo("admin");
        assertThat(cronJobInfo.getLastModifyTime()).isNotNull();
        assertThat(cronJobInfo.getDelete()).isFalse();
        // 保存不改变启停状态
        assertThat(cronJobInfo.getEnable()).isFalse();
    }

    @Test
    @DisplayName("不传时区时取业务时区")
    void convertWithAppTimeZone() {
        V4SaveCronRequest request = createRequest();
        stubPlanVariable(1L, "var_str", TaskVariableTypeEnum.STRING.getType());

        CronJobInfoDTO cronJobInfo = converter.convert(request, OPERATOR);

        assertThat(cronJobInfo.getExecuteTimeZone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    @DisplayName("全局变量的 id / name / type 一律以执行方案里的定义为准")
    void convertGlobalVarsFromPlanDefinition() {
        V4SaveCronRequest request = createRequest();
        V4GlobalVarDTO stringVar = new V4GlobalVarDTO();
        stringVar.setName("var_str");
        stringVar.setValue("v1");
        V4GlobalVarDTO hostVar = new V4GlobalVarDTO();
        hostVar.setId(2L);
        // 传入的 name 与执行方案里的定义不一致时，以按 id 解析出的为准
        hostVar.setName("wrong_name");
        hostVar.setExecuteTarget(hostExecuteTarget());
        request.setGlobalVarList(Arrays.asList(stringVar, hostVar));
        when(taskPlanResource.getGlobalVarByName(anyLong(), anyString()))
            .thenReturn(successResp(planVariable(1L, "var_str", TaskVariableTypeEnum.STRING.getType())));
        when(taskPlanResource.getGlobalVarById(anyLong(), anyLong()))
            .thenReturn(successResp(
                planVariable(2L, "var_host", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType())));

        List<CronJobVariableDTO> variables = converter.convert(request, OPERATOR).getVariableValue();

        assertThat(variables).hasSize(2);
        assertThat(variables.get(0).getId()).isEqualTo(1L);
        assertThat(variables.get(0).getName()).isEqualTo("var_str");
        assertThat(variables.get(0).getType()).isEqualTo(TaskVariableTypeEnum.STRING);
        assertThat(variables.get(0).getValue()).isEqualTo("v1");
        assertThat(variables.get(0).getServer()).isNull();

        assertThat(variables.get(1).getId()).isEqualTo(2L);
        assertThat(variables.get(1).getName()).isEqualTo("var_host");
        assertThat(variables.get(1).getType()).isEqualTo(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);
        assertThat(variables.get(1).getServer().getIps()).hasSize(1);
        assertThat(variables.get(1).getServer().getIps().get(0).getHostId()).isEqualTo(101L);
        assertThat(variables.get(1).getServer().getDynamicGroupIds()).containsExactly("group-1");
        verify(taskPlanResource).getGlobalVarByName(PLAN_ID, "var_str");
        verify(taskPlanResource).getGlobalVarById(PLAN_ID, 2L);
    }

    @Test
    @DisplayName("定时任务的主机变量不支持容器过滤器")
    void convertRejectContainerFilter() {
        V4SaveCronRequest request = createRequest();
        V4GlobalVarDTO hostVar = new V4GlobalVarDTO();
        hostVar.setId(2L);
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setKubeContainerFilters(Collections.singletonList(new V4ContainerFilter()));
        hostVar.setExecuteTarget(executeTarget);
        request.setGlobalVarList(Collections.singletonList(hostVar));
        when(taskPlanResource.getGlobalVarById(anyLong(), anyLong()))
            .thenReturn(successResp(
                planVariable(2L, "var_host", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType())));

        assertThatThrownBy(() -> converter.convert(request, OPERATOR))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("新建时执行方案 ID、名称、执行时机三者必填")
    void validateCreateRequiredParams() {
        V4SaveCronRequest noPlanId = createRequest();
        noPlanId.setPlanId(null);
        assertThatThrownBy(() -> converter.validate(noPlanId)).isInstanceOf(InvalidParamException.class);

        V4SaveCronRequest noName = createRequest();
        noName.setName(null);
        assertThatThrownBy(() -> converter.validate(noName)).isInstanceOf(InvalidParamException.class);

        V4SaveCronRequest noTiming = createRequest();
        noTiming.setCronExpression(null);
        assertThatThrownBy(() -> converter.validate(noTiming)).isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("更新时至少要改一项")
    void validateUpdateNeedsChange() {
        V4SaveCronRequest request = new V4SaveCronRequest();
        request.setAppId(APP_ID);
        request.setId(500L);

        assertThatThrownBy(() -> converter.validate(request)).isInstanceOf(InvalidParamException.class);

        request.setName("new_name");
        converter.validate(request);
    }

    @Test
    @DisplayName("cron 表达式非法时判为非法参数")
    void validateCronExpression() {
        V4SaveCronRequest request = createRequest();
        request.setCronExpression("not-a-cron");

        assertThatThrownBy(() -> converter.validate(request)).isInstanceOf(InvalidParamException.class);
    }

    private void stubPlanVariable(Long id, String name, Integer type) {
        when(taskPlanResource.getGlobalVarByName(anyLong(), anyString()))
            .thenReturn(successResp(planVariable(id, name, type)));
        when(taskPlanResource.getGlobalVarById(anyLong(), anyLong()))
            .thenReturn(successResp(planVariable(id, name, type)));
    }

    /**
     * 不用 InternalResponse.buildSuccessResp：它会走 I18nUtil 取错误文案，单测里没有 Spring 上下文
     */
    private <T> InternalResponse<T> successResp(T data) {
        InternalResponse<T> resp = new InternalResponse<>();
        resp.setSuccess(true);
        resp.setCode(ErrorCode.RESULT_OK);
        resp.setData(data);
        return resp;
    }

    private ServiceTaskVariableDTO planVariable(Long id, String name, Integer type) {
        ServiceTaskVariableDTO variable = new ServiceTaskVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(type);
        return variable;
    }

    private V4ExecuteTargetDTO hostExecuteTarget() {
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(101L);
        executeTarget.setHostList(Collections.singletonList(host));
        EsbDynamicGroupDTO dynamicGroup = new EsbDynamicGroupDTO();
        dynamicGroup.setId("group-1");
        executeTarget.setDynamicGroups(Collections.singletonList(dynamicGroup));
        return executeTarget;
    }

    private V4SaveCronRequest createRequest() {
        V4SaveCronRequest request = new V4SaveCronRequest();
        request.setAppId(APP_ID);
        request.setPlanId(PLAN_ID);
        request.setName("test_cron");
        request.setCronExpression("0/5 * * * *");
        return request;
    }
}
