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

package com.tencent.bk.job.manage.service.plan;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.ResolvedSummary.ResolvedGlobalVar;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 创建执行方案的审批概要里，全局变量章节的组装。
 * <p>
 * 盯住三件"破了单据就骗人"的事：<b>密文变量的取值绝不进概要</b>（概要整份明文落库）、
 * <b>主机变量按上限逐台列出或只报台数</b>、<b>沿用模板默认值的变量也必须列出并标注</b>。
 */
@DisplayName("创建执行方案审批概要-全局变量组装测试")
class PlanGlobalVarSummaryBuilderTest {

    private static final String TENANT_ID = "default";

    private TenantHostService tenantHostService;
    private PlanGlobalVarSummaryBuilder builder;

    @BeforeEach
    void setUp() {
        tenantHostService = mock(TenantHostService.class);
        builder = new PlanGlobalVarSummaryBuilder(tenantHostService);
    }

    @Test
    @DisplayName("请求未覆盖的变量同样列出并标为沿用默认值")
    void notAssignedVarIsStillListed() {
        ResolvedSummary summary = new ResolvedSummary();
        List<TaskVariableDTO> variables = Arrays.asList(
            stringVar("version", "v1.2.3"),
            stringVar("port", "8080"));

        builder.fillGlobalVars(summary, variables, assignedNames("version"), TENANT_ID);

        assertThat(summary.getGlobalVars()).hasSize(2);
        assertThat(globalVar(summary, "version").getAssigned()).isTrue();
        assertThat(globalVar(summary, "version").getValue()).isEqualTo("v1.2.3");
        assertThat(globalVar(summary, "port").getAssigned())
            .as("沿用默认值的变量不标注出来，审批人会以为这个值是本次改的")
            .isFalse();
        assertThat(globalVar(summary, "port").getValue()).isEqualTo("8080");
    }

    @Test
    @DisplayName("密文变量只带变量名与类型，取值一律不进概要")
    void cipherVarValueIsNeverIncluded() {
        ResolvedSummary summary = new ResolvedSummary();
        TaskVariableDTO cipherVar = stringVar("password", "P@ssw0rd");
        cipherVar.setType(TaskVariableTypeEnum.CIPHER);

        builder.fillGlobalVars(summary, Collections.singletonList(cipherVar),
            assignedNames("password"), TENANT_ID);

        ResolvedGlobalVar globalVar = globalVar(summary, "password");
        assertThat(globalVar.getType()).isEqualTo(TaskVariableTypeEnum.CIPHER.name());
        assertThat(globalVar.getValue()).isNull();
    }

    @Test
    @DisplayName("已删除的变量不进概要：它在方案建出来之后并不生效")
    void deletedVarIsSkipped() {
        ResolvedSummary summary = new ResolvedSummary();
        TaskVariableDTO deletedVar = stringVar("obsolete", "x");
        deletedVar.setDelete(true);

        builder.fillGlobalVars(summary, Arrays.asList(deletedVar, stringVar("version", "v1")),
            assignedNames("version"), TENANT_ID);

        assertThat(summary.getGlobalVars()).hasSize(1);
        assertThat(summary.getGlobalVars().get(0).getName()).isEqualTo("version");
    }

    @Test
    @DisplayName("主机变量只传了 bk_host_id 时反查补成「云区域ID:IP」：一串主机ID摆在单据上等于没给信息")
    void hostIdIsResolvedToCloudIp() {
        ResolvedSummary summary = new ResolvedSummary();
        when(tenantHostService.listHosts(anyString(), any()))
            .thenReturn(Collections.singletonList(host(1L, "127.0.0.1")));

        builder.fillGlobalVars(summary, Collections.singletonList(hostVar("target_hosts", 1)),
            assignedNames("target_hosts"), TENANT_ID);

        ResolvedGlobalVar globalVar = globalVar(summary, "target_hosts");
        assertThat(globalVar.getHosts()).containsExactly("0:127.0.0.1");
        assertThat(globalVar.getHostCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("反查失败时退回展示主机ID，不能让预检跟着失败")
    void hostResolveFailureFallsBackToHostId() {
        ResolvedSummary summary = new ResolvedSummary();
        when(tenantHostService.listHosts(anyString(), any()))
            .thenThrow(new IllegalStateException("cmdb unavailable"));

        builder.fillGlobalVars(summary, Collections.singletonList(hostVar("target_hosts", 1)),
            assignedNames("target_hosts"), TENANT_ID);

        assertThat(globalVar(summary, "target_hosts").getHosts()).containsExactly("host_id:1");
    }

    @Test
    @DisplayName("台数超过上限时只报台数、也不再反查IP：那种情况下查回来的IP没有用处")
    void overLimitHostVarReportsCountOnly() {
        ResolvedSummary summary = new ResolvedSummary();
        int hostCount = ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1;

        builder.fillGlobalVars(summary, Collections.singletonList(hostVar("target_hosts", hostCount)),
            assignedNames("target_hosts"), TENANT_ID);

        ResolvedGlobalVar globalVar = globalVar(summary, "target_hosts");
        assertThat(globalVar.getHosts()).isNull();
        assertThat(globalVar.getHostCount()).isEqualTo(hostCount);
        verify(tenantHostService, never()).listHosts(anyString(), any());
    }

    @Test
    @DisplayName("动态分组与拓扑节点只报个数，并把「放行时重新解析」的提示打开")
    void dynamicTargetMarksSummary() {
        ResolvedSummary summary = new ResolvedSummary();
        TaskTargetDTO target = new TaskTargetDTO();
        TaskHostNodeDTO hostNode = new TaskHostNodeDTO();
        hostNode.setDynamicGroupId(Arrays.asList("group-1", "group-2"));
        hostNode.setNodeInfoList(Collections.singletonList(new TaskNodeInfoDTO()));
        target.setHostNodeList(hostNode);
        TaskVariableDTO variable = stringVar("dynamic_hosts", target.toJsonString());
        variable.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);

        builder.fillGlobalVars(summary, Collections.singletonList(variable),
            assignedNames("dynamic_hosts"), TENANT_ID);

        ResolvedGlobalVar globalVar = globalVar(summary, "dynamic_hosts");
        assertThat(globalVar.getDynamicGroupCount()).isEqualTo(2);
        assertThat(globalVar.getTopoNodeCount()).isEqualTo(1);
        assertThat(globalVar.getHostCount()).isNull();
        assertThat(summary.getContainsDynamicTarget())
            .as("台数为0会被读成「不动机器」，动态目标必须给出提示")
            .isTrue();
    }

    @Test
    @DisplayName("变量列表为空时不写入任何变量，也不查主机")
    void emptyVariablesFillNothing() {
        ResolvedSummary summary = new ResolvedSummary();

        builder.fillGlobalVars(summary, Collections.emptyList(), Collections.emptySet(), TENANT_ID);

        assertThat(summary.getGlobalVars()).isNull();
        verify(tenantHostService, never()).listHosts(anyString(), any());
    }

    private Set<String> assignedNames(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    private ResolvedGlobalVar globalVar(ResolvedSummary summary, String name) {
        return summary.getGlobalVars().stream()
            .filter(globalVar -> name.equals(globalVar.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("概要里没有变量：" + name));
    }

    private TaskVariableDTO stringVar(String name, String defaultValue) {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING);
        variable.setDefaultValue(defaultValue);
        return variable;
    }

    /**
     * 主机变量的取值是一串只带 bk_host_id 的主机，这正是调用方最常传的形态
     */
    private TaskVariableDTO hostVar(String name, int hostCount) {
        List<ApplicationHostDTO> hosts = new ArrayList<>(hostCount);
        for (int i = 1; i <= hostCount; i++) {
            ApplicationHostDTO host = new ApplicationHostDTO();
            host.setHostId((long) i);
            hosts.add(host);
        }
        TaskHostNodeDTO hostNode = new TaskHostNodeDTO();
        hostNode.setHostList(hosts);
        TaskTargetDTO target = new TaskTargetDTO();
        target.setHostNodeList(hostNode);

        TaskVariableDTO variable = stringVar(name, target.toJsonString());
        variable.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);
        return variable;
    }

    private ApplicationHostDTO host(Long hostId, String ip) {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(hostId);
        host.setCloudAreaId(0L);
        host.setIp(ip);
        return host;
    }
}
