/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted; free of charge; to any person obtaining a copy of this software and associated
 * documentation files (the "Software"); to deal in the Software without restriction; including without limitation
 * the rights to use; copy; modify; merge; publish; distribute; sublicense; and/or sell copies of the Software; and
 * to permit persons to whom the Software is furnished to do so; subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS"; WITHOUT WARRANTY OF ANY KIND; EXPRESS OR IMPLIED; INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM; DAMAGES OR OTHER LIABILITY; WHETHER IN AN ACTION OF
 * CONTRACT; TORT OR OTHERWISE; ARISING FROM; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.engine;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.execute.config.ScheduleStrategyProperties;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobKeepaliveManager;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.variable.JobBuildInVariableResolver;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.Getter;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

/**
 * 执行引擎依赖的服务的托管类。用于简化一些较为复杂的类构造函数的参数列表
 */
@Component
@Getter
public class EngineDependentServiceHolder {
    private final ResultHandleManager resultHandleManager;
    private final TaskInstanceService taskInstanceService;
    private final GseTaskService gseTaskService;
    private final AccountService accountService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;
    private final AgentService agentService;
    private final LogService logService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    private final ExecuteMonitor executeMonitor;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final GseTasksExceptionCounter gseTasksExceptionCounter;
    private final StepInstanceService stepInstanceService;
    private final Tracer tracer;
    private final GseClient gseClient;
    private final RunningJobKeepaliveManager runningJobKeepaliveManager;
    private final JobBuildInVariableResolver jobBuildInVariableResolver;
    private final ScheduleStrategyProperties scheduleStrategyProperties;

    public EngineDependentServiceHolder(ResultHandleManager resultHandleManager,
                                        TaskInstanceService taskInstanceService,
                                        GseTaskService gseTaskService,
                                        AccountService accountService,
                                        TaskInstanceVariableService taskInstanceVariableService,
                                        StepInstanceVariableValueService stepInstanceVariableValueService,
                                        AgentService agentService,
                                        LogService logService,
                                        TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                        ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                        ExecuteMonitor executeMonitor,
                                        TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                        GseTasksExceptionCounter gseTasksExceptionCounter,
                                        StepInstanceService stepInstanceService,
                                        Tracer tracer,
                                        GseClient gseClient,
                                        RunningJobKeepaliveManager runningJobKeepaliveManager,
                                        JobBuildInVariableResolver jobBuildInVariableResolver,
                                        ScheduleStrategyProperties scheduleStrategyProperties) {
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceService = taskInstanceService;
        this.gseTaskService = gseTaskService;
        this.accountService = accountService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.agentService = agentService;
        this.logService = logService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.executeMonitor = executeMonitor;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
        this.stepInstanceService = stepInstanceService;
        this.tracer = tracer;
        this.gseClient = gseClient;
        this.runningJobKeepaliveManager = runningJobKeepaliveManager;
        this.jobBuildInVariableResolver = jobBuildInVariableResolver;
        this.scheduleStrategyProperties = scheduleStrategyProperties;
    }
}
