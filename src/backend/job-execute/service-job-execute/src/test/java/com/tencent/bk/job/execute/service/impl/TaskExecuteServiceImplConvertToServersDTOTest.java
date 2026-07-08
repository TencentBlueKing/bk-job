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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.cache.CustomPasswordCache;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobResourceQuotaManager;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.api.inner.ServiceUserResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTargetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 {@link TaskExecuteServiceImpl#convertToServersDTO(ServiceTaskTargetDTO)} 把动态条件过滤器
 * 完整透传到 {@link ExecuteTargetDTO}：
 * <ul>
 *   <li>覆盖「方案/模板步骤直接目标」与「EXECUTE_OBJECT_LIST 变量默认值」两条入口（共用此方法）</li>
 *   <li>透传时做防御性 clone，避免运行时 mutate 反向污染上游 ServiceTaskTargetDTO</li>
 *   <li>调用时传入的覆盖值（executeVariableValues）走 ExecuteTargetDTO 形态、不经本方法，故无需此处兜底</li>
 * </ul>
 * <p>
 * 历史 bug：旧版本本方法只搬 hostList/dynamicGroupId/topo/staticContainerList，{@code containerFilters}
 * 在「保存有动态条件的方案 → 执行方案」链路上整段丢失，本测试是该修复的回归网。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskExecuteServiceImpl.convertToServersDTO: containerFilters 透传与 clone 防御")
class TaskExecuteServiceImplConvertToServersDTOTest {

    @Mock private AccountService accountService;
    @Mock private TaskInstanceService taskInstanceService;
    @Mock private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    @Mock private TaskPlanService taskPlanService;
    @Mock private TaskInstanceVariableService taskInstanceVariableService;
    @Mock private TaskOperationLogService taskOperationLogService;
    @Mock private ScriptService scriptService;
    @Mock private StepInstanceService stepInstanceService;
    @Mock private ServiceUserResource userResource;
    @Mock private ExecuteAuthService executeAuthService;
    @Mock private DangerousScriptCheckService dangerousScriptCheckService;
    @Mock private JobExecuteConfig jobExecuteConfig;
    @Mock private TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    @Mock private RollingConfigService rollingConfigService;
    @Mock private ServiceTaskTemplateResource taskTemplateResource;
    @Mock private TaskInstanceExecuteObjectProcessor taskInstanceExecuteObjectProcessor;
    @Mock private RunningJobResourceQuotaManager runningJobResourceQuotaManager;
    @Mock private HostService hostService;
    @Mock private CustomPasswordCache customPasswordCache;
    @Mock private TenantService tenantService;

    private TaskExecuteServiceImpl service;
    private Method convertMethod;

    @BeforeEach
    void setUp() throws Exception {
        service = new TaskExecuteServiceImpl(
            accountService,
            taskInstanceService,
            taskExecuteMQEventDispatcher,
            taskPlanService,
            taskInstanceVariableService,
            taskOperationLogService,
            scriptService,
            stepInstanceService,
            userResource,
            executeAuthService,
            dangerousScriptCheckService,
            jobExecuteConfig,
            taskEvictPolicyExecutor,
            rollingConfigService,
            taskTemplateResource,
            taskInstanceExecuteObjectProcessor,
            runningJobResourceQuotaManager,
            hostService,
            customPasswordCache,
            tenantService
        );

        convertMethod = TaskExecuteServiceImpl.class
            .getDeclaredMethod("convertToServersDTO", ServiceTaskTargetDTO.class);
        convertMethod.setAccessible(true);
    }

    @Test
    @DisplayName("入参 null → 返回 null（与现状一致，不引入新行为）")
    void nullInputReturnsNull() throws Exception {
        ExecuteTargetDTO result = invoke(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("仅 containerFilters：完整透传，propConditions 不丢")
    void containerFiltersPropagated() throws Exception {
        ServiceTaskTargetDTO source = baseTarget();
        source.setContainerFilters(Collections.singletonList(buildFullFilter()));

        ExecuteTargetDTO result = invoke(source);

        assertThat(result.getContainerFilters()).hasSize(1);
        KubeContainerFilter cf = result.getContainerFilters().get(0);
        assertThat(cf.getClusterNodes()).hasSize(1);
        assertThat(cf.getClusterNodes().get(0).getId()).isEqualTo(1000L);
        assertThat(cf.getClusterNodes().get(0).getId()).isEqualTo(1000L);
        assertThat(cf.getPropConditions()).hasSize(2);
        assertThat(cf.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
        assertThat(cf.getPropConditions().get(1).getValue()).isEqualTo("pod-a");
    }

    @Test
    @DisplayName("containerFilters 为 null：result.containerFilters = null，行为与现状一致")
    void nullContainerFiltersPreserved() throws Exception {
        ServiceTaskTargetDTO source = baseTarget();
        source.setContainerFilters(null);

        ExecuteTargetDTO result = invoke(source);

        assertThat(result.getContainerFilters()).isNull();
    }

    @Test
    @DisplayName("containerFilters 为空集合：result.containerFilters = null（不引入空集合污染下游）")
    void emptyContainerFiltersStaysNull() throws Exception {
        ServiceTaskTargetDTO source = baseTarget();
        source.setContainerFilters(Collections.emptyList());

        ExecuteTargetDTO result = invoke(source);

        assertThat(result.getContainerFilters()).isNull();
    }

    @Test
    @DisplayName("透传时做防御性 clone：mutate 输出不影响上游 ServiceTaskTargetDTO")
    void defensiveCloneOnPropagation() throws Exception {
        ServiceTaskTargetDTO source = baseTarget();
        KubeContainerFilter sourceFilter = buildFullFilter();
        source.setContainerFilters(Collections.singletonList(sourceFilter));

        ExecuteTargetDTO result = invoke(source);
        KubeContainerFilter outputFilter = result.getContainerFilters().get(0);

        outputFilter.getClusterNodes().clear();
        outputFilter.getPropConditions().clear();

        assertThat(sourceFilter.getClusterNodes()).hasSize(1);
        assertThat(sourceFilter.getClusterNodes().get(0).getId()).isEqualTo(1000L);
        assertThat(sourceFilter.getPropConditions()).hasSize(2);
    }

    @Test
    @DisplayName("containerFilters 与 staticContainerList / staticIpList 共存：各字段互不干扰")
    void coexistsWithOtherFields() throws Exception {
        ServiceTaskTargetDTO source = baseTarget();
        source.setContainerFilters(Collections.singletonList(buildFullFilter()));
        // staticContainerList 与 hostList 由 baseTarget 不设置；本用例只关心 containerFilters 不会
        // 因为其他字段缺失而被忽略

        ExecuteTargetDTO result = invoke(source);

        assertThat(result.getContainerFilters()).hasSize(1);
        assertThat(result.getStaticContainerList()).isNull();
        assertThat(result.getStaticIpList()).isNull();
    }

    private ExecuteTargetDTO invoke(ServiceTaskTargetDTO target) throws Exception {
        return (ExecuteTargetDTO) convertMethod.invoke(service, target);
    }

    private static ServiceTaskTargetDTO baseTarget() {
        ServiceTaskTargetDTO source = new ServiceTaskTargetDTO();
        // targetServer 必须非 null：源代码 line 1998 直接 getTargetServer().getHostList()
        source.setTargetServer(new ServiceTaskHostNodeDTO());
        return source;
    }

    private static KubeContainerFilter buildFullFilter() {
        KubeContainerFilter cf = new KubeContainerFilter();
        cf.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(1000L)));
        cf.setPropConditions(Arrays.asList(
            new KubePropCondition("container_container_uid", "contains", "docker://abcdefg"),
            new KubePropCondition("pod_name", "equal", "pod-a")
        ));
        return cf;
    }
}
