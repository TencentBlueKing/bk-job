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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.execute.common.cache.WhiteHostCache;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.metrics.ExecuteObjectSampler;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceExecuteObjects;
import com.tencent.bk.job.execute.service.ContainerService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.remote.RemoteAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskInstanceExecuteObjectProcessor 容器条件变量解析")
class TaskInstanceExecuteObjectProcessorTest {

    @Mock private TenantService tenantService;
    @Mock private HostService hostService;
    @Mock private RemoteAppService remoteAppService;
    @Mock private ContainerService containerService;
    @Mock private AppScopeMappingService appScopeMappingService;
    @Mock private WhiteHostCache whiteHostCache;
    @Mock private AgentStateClient agentStateClient;
    @Mock private IBizCmdbClient bizCmdbClient;
    @Mock private ExecuteObjectSampler executeObjectSampler;

    private TaskInstanceExecuteObjectProcessor processor;
    private Method acquireByContainerFiltersMethod;

    @BeforeEach
    void setUp() throws Exception {
        processor = new TaskInstanceExecuteObjectProcessor(
            tenantService,
            hostService,
            remoteAppService,
            containerService,
            appScopeMappingService,
            whiteHostCache,
            agentStateClient,
            bizCmdbClient,
            executeObjectSampler
        );
        acquireByContainerFiltersMethod = TaskInstanceExecuteObjectProcessor.class.getDeclaredMethod(
            "acquireAndSetContainersByContainerFilters",
            TaskInstanceExecuteObjects.class,
            TaskInstanceDTO.class,
            List.class,
            Collection.class
        );
        acquireByContainerFiltersMethod.setAccessible(true);
    }

    @Test
    @DisplayName("EXECUTE_OBJECT_LIST 变量中的 containerFilters 会解析为静态容器并合并进 executeObjects")
    void variableContainerFiltersResolvedAndMergedToExecuteObjects() throws Exception {
        long appId = 100L;
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(appId);
        taskInstance.setId(200L);

        KubeContainerFilter containerFilter = new KubeContainerFilter();
        containerFilter.setName("变量容器条件");

        ExecuteTargetDTO executeTarget = new ExecuteTargetDTO();
        executeTarget.setContainerFilters(Collections.singletonList(containerFilter));

        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType());
        variable.setExecuteTarget(executeTarget);

        Container container = buildContainer(300L);
        when(containerService.listContainerByContainerFilter(eq(appId), eq(containerFilter)))
            .thenReturn(Collections.singletonList(container));

        TaskInstanceExecuteObjects taskInstanceExecuteObjects = new TaskInstanceExecuteObjects();
        invokeAcquireByContainerFilters(
            taskInstanceExecuteObjects,
            taskInstance,
            Collections.emptyList(),
            Collections.singletonList(variable)
        );
        executeTarget.buildMergedExecuteObjects(true);

        assertThat(containerFilter.getContainers()).containsExactly(container);
        assertThat(taskInstanceExecuteObjects.getValidContainers()).containsExactly(container);
        assertThat(executeTarget.getExecuteObjects()).hasSize(1);
        assertThat(executeTarget.getExecuteObjects().get(0).isContainerExecuteObject()).isTrue();
        assertThat(executeTarget.getExecuteObjects().get(0).getContainer()).isEqualTo(container);
        verify(containerService).listContainerByContainerFilter(appId, containerFilter);
        verify(executeObjectSampler).tryToRecordContainerFilterResolvedNum(taskInstance, containerFilter, 1);
    }

    private void invokeAcquireByContainerFilters(TaskInstanceExecuteObjects taskInstanceExecuteObjects,
                                                 TaskInstanceDTO taskInstance,
                                                 List<?> stepInstances,
                                                 Collection<TaskVariableDTO> variables) throws Exception {
        acquireByContainerFiltersMethod.invoke(processor, taskInstanceExecuteObjects, taskInstance, stepInstances,
            variables);
    }

    private static Container buildContainer(long id) {
        Container container = new Container();
        container.setId(id);
        container.setContainerId("container-" + id);
        container.setName("nginx");
        container.setNodeAgentId("agent-" + id);
        container.setClusterId(1L);
        container.setNamespaceId(2L);
        container.setPodName("pod-a");
        return container;
    }
}
