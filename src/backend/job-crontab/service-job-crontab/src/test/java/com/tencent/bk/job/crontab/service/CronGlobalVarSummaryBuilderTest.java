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
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.ResolvedSummary.ResolvedGlobalVar;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTargetDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 保存定时任务的审批概要里，全局变量章节的组装。
 * <p>
 * 定时任务到点就会拿这套变量去跑，因此列的是<b>执行方案的全部全局变量</b>：本次没传的按现值展示并标注，
 * 只列本次传的等于让审批人蒙着眼放行。密文变量的取值一律不进概要，主机变量按上限逐台列出或只报台数。
 */
@DisplayName("保存定时任务审批概要-全局变量组装测试")
class CronGlobalVarSummaryBuilderTest {

    private static final String TENANT_ID = "tenant-1";
    private static final Long APP_ID = 2L;
    private static final Long TEMPLATE_ID = 1000L;
    private static final Long PLAN_ID = 2000L;

    private ServiceTaskPlanResource taskPlanResource;
    private HostService hostService;
    private CronGlobalVarSummaryBuilder builder;
    private User operator;

    @BeforeEach
    void setUp() {
        taskPlanResource = mock(ServiceTaskPlanResource.class);
        hostService = mock(HostService.class);
        builder = new CronGlobalVarSummaryBuilder(taskPlanResource, hostService);
        operator = new User(TENANT_ID, "admin", "admin");
    }

    @Test
    @DisplayName("本次没传的变量按执行方案默认值展示并标为非本次指定")
    void notAssignedVarUsesPlanDefault() {
        givenPlanVars(stringVar(1L, "version", "v1.0.0"), stringVar(2L, "port", "8080"));
        ResolvedSummary summary = new ResolvedSummary();
        CronJobInfoDTO cronJobInfo = cronJobInfo(cronVar(1L, "version", "v2.0.0"));

        builder.fillGlobalVars(summary, cronJobInfo, operator);

        assertThat(summary.getGlobalVars()).hasSize(2);
        assertThat(globalVar(summary, "version").getAssigned()).isTrue();
        assertThat(globalVar(summary, "version").getValue()).isEqualTo("v2.0.0");
        assertThat(globalVar(summary, "port").getAssigned()).isFalse();
        assertThat(globalVar(summary, "port").getValue())
            .as("沿用现值的变量同样会被执行，取值不能空着")
            .isEqualTo("8080");
    }

    @Test
    @DisplayName("密文变量只带变量名与类型，取值一律不进概要")
    void cipherVarValueIsNeverIncluded() {
        ServiceTaskVariableDTO cipherVar = stringVar(1L, "password", "P@ssw0rd");
        cipherVar.setType(TaskVariableTypeEnum.CIPHER.getType());
        givenPlanVars(cipherVar);
        ResolvedSummary summary = new ResolvedSummary();

        builder.fillGlobalVars(summary, cronJobInfo(cronVar(1L, "password", "P@ssw0rd-new")), operator);

        ResolvedGlobalVar globalVar = globalVar(summary, "password");
        assertThat(globalVar.getType()).isEqualTo(TaskVariableTypeEnum.CIPHER.name());
        assertThat(globalVar.getValue()).isNull();
    }

    @Test
    @DisplayName("主机变量只传了 bk_host_id 时反查补成「云区域ID:IP」")
    void hostIdIsResolvedToCloudIp() {
        givenPlanVars(hostVar(1L, "target_hosts"));
        givenHostResolved();
        ResolvedSummary summary = new ResolvedSummary();
        CronJobVariableDTO assigned = cronVar(1L, "target_hosts", null);
        assigned.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);
        assigned.setServer(serverWithHostIds(1L));

        builder.fillGlobalVars(summary, cronJobInfo(assigned), operator);

        assertThat(globalVar(summary, "target_hosts").getHosts()).containsExactly("0:127.0.0.1");
        assertThat(globalVar(summary, "target_hosts").getHostCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("台数超过上限时只报台数、也不再反查IP")
    void overLimitHostVarReportsCountOnly() {
        givenPlanVars(hostVar(1L, "target_hosts"));
        int hostCount = ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1;
        Long[] hostIds = new Long[hostCount];
        for (int i = 0; i < hostCount; i++) {
            hostIds[i] = (long) (i + 1);
        }
        ResolvedSummary summary = new ResolvedSummary();
        CronJobVariableDTO assigned = cronVar(1L, "target_hosts", null);
        assigned.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);
        assigned.setServer(serverWithHostIds(hostIds));

        builder.fillGlobalVars(summary, cronJobInfo(assigned), operator);

        ResolvedGlobalVar globalVar = globalVar(summary, "target_hosts");
        assertThat(globalVar.getHosts()).isNull();
        assertThat(globalVar.getHostCount()).isEqualTo(hostCount);
        verify(hostService, never()).fillHosts(anyString(), any());
    }

    @Test
    @DisplayName("本次没传主机变量时按执行方案的默认目标展示，动态分组同时打开「放行时重新解析」提示")
    void notAssignedHostVarUsesPlanDefaultTarget() {
        ServiceTaskVariableDTO planVar = hostVar(1L, "target_hosts");
        ServiceTaskHostNodeDTO targetServer = new ServiceTaskHostNodeDTO();
        targetServer.setHostList(Collections.singletonList(serviceHost(1L, "127.0.0.1")));
        targetServer.setDynamicGroupId(Arrays.asList("group-1", "group-2"));
        ServiceTaskTargetDTO target = new ServiceTaskTargetDTO();
        target.setTargetServer(targetServer);
        planVar.setDefaultTargetValue(target);
        givenPlanVars(planVar);
        ResolvedSummary summary = new ResolvedSummary();

        builder.fillGlobalVars(summary, cronJobInfo(), operator);

        ResolvedGlobalVar globalVar = globalVar(summary, "target_hosts");
        assertThat(globalVar.getAssigned()).isFalse();
        assertThat(globalVar.getHosts()).containsExactly("0:127.0.0.1");
        assertThat(globalVar.getDynamicGroupCount()).isEqualTo(2);
        assertThat(summary.getContainsDynamicTarget()).isTrue();
        verify(hostService, never()).fillHosts(anyString(), any());
    }

    @Test
    @DisplayName("查执行方案变量失败时变量章节缺失，但预检不能跟着失败")
    void planVarQueryFailureDegradesGracefully() {
        when(taskPlanResource.getPlanVariable(anyString(), anyLong(), anyLong(), anyLong()))
            .thenThrow(new IllegalStateException("job-manage unavailable"));
        ResolvedSummary summary = new ResolvedSummary();

        builder.fillGlobalVars(summary, cronJobInfo(), operator);

        assertThat(summary.getGlobalVars()).isNull();
    }

    @Test
    @DisplayName("定时执行脚本的定时任务没有执行方案，不查变量")
    void cronWithoutPlanQueriesNothing() {
        ResolvedSummary summary = new ResolvedSummary();
        CronJobInfoDTO cronJobInfo = cronJobInfo();
        cronJobInfo.setTaskPlanId(0L);

        builder.fillGlobalVars(summary, cronJobInfo, operator);

        assertThat(summary.getGlobalVars()).isNull();
        verify(taskPlanResource, never()).getPlanVariable(anyString(), anyLong(), anyLong(), anyLong());
    }

    private void givenPlanVars(ServiceTaskVariableDTO... planVars) {
        when(taskPlanResource.getPlanVariable(anyString(), anyLong(), anyLong(), anyLong()))
            .thenReturn(successResp(Arrays.asList(planVars)));
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

    /**
     * 主机反查的替身：按 hostId 回填云区域与 IP，与真实实现"就地填充入参"的行为一致
     */
    private void givenHostResolved() {
        doAnswer(invocation -> {
            List<HostDTO> hosts = invocation.getArgument(1);
            for (HostDTO host : hosts) {
                host.setBkCloudId(0L);
                host.setIp("127.0.0." + host.getHostId());
            }
            return hosts.size();
        }).when(hostService).fillHosts(anyString(), any());
    }

    private ResolvedGlobalVar globalVar(ResolvedSummary summary, String name) {
        return summary.getGlobalVars().stream()
            .filter(globalVar -> name.equals(globalVar.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("概要里没有变量：" + name));
    }

    private CronJobInfoDTO cronJobInfo(CronJobVariableDTO... variables) {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setAppId(APP_ID);
        cronJobInfo.setTaskTemplateId(TEMPLATE_ID);
        cronJobInfo.setTaskPlanId(PLAN_ID);
        cronJobInfo.setVariableValue(Arrays.asList(variables));
        return cronJobInfo;
    }

    private CronJobVariableDTO cronVar(Long id, String name, String value) {
        CronJobVariableDTO variable = new CronJobVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING);
        variable.setValue(value);
        return variable;
    }

    private ServiceTaskVariableDTO stringVar(Long id, String name, String defaultValue) {
        ServiceTaskVariableDTO variable = new ServiceTaskVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING.getType());
        variable.setDefaultValue(defaultValue);
        return variable;
    }

    private ServiceTaskVariableDTO hostVar(Long id, String name) {
        ServiceTaskVariableDTO variable = new ServiceTaskVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType());
        return variable;
    }

    /**
     * 调用方最常传的形态：只带 bk_host_id，没有 IP
     */
    private ServerDTO serverWithHostIds(Long... hostIds) {
        List<HostDTO> hosts = new ArrayList<>(hostIds.length);
        for (Long hostId : hostIds) {
            hosts.add(HostDTO.fromHostId(hostId));
        }
        ServerDTO server = new ServerDTO();
        server.setIps(hosts);
        return server;
    }

    private ServiceHostInfoDTO serviceHost(Long hostId, String ip) {
        ServiceHostInfoDTO host = new ServiceHostInfoDTO();
        host.setHostId(hostId);
        host.setCloudAreaId(0L);
        host.setIp(ip);
        return host;
    }
}
