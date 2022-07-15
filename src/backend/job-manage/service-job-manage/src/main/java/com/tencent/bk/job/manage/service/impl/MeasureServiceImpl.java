/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.manage.dao.AccountDAO;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPRecordDAO;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.MeasureService;
import com.tencent.bk.job.manage.service.SyncService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

@Slf4j
@Service
public class MeasureServiceImpl implements MeasureService {

    private final MeterRegistry meterRegistry;
    private final AccountDAO accountDAO;
    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final ScriptDAO scriptDAO;
    private final WhiteIPRecordDAO whiteIPRecordDAO;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final SyncService syncService;

    @Autowired
    public MeasureServiceImpl(MeterRegistry meterRegistry, AccountDAO accountDAO,
                              ApplicationDAO applicationDAO, ApplicationHostDAO applicationHostDAO,
                              ScriptDAO scriptDAO, WhiteIPRecordDAO whiteIPRecordDAO,
                              TaskTemplateService taskTemplateService, TaskPlanService taskPlanService,
                              SyncService syncService) {
        this.meterRegistry = meterRegistry;
        this.accountDAO = accountDAO;
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.scriptDAO = scriptDAO;
        this.whiteIPRecordDAO = whiteIPRecordDAO;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.syncService = syncService;
    }

    @Override
    public void init() {
        // 业务
        meterRegistry.gauge(
            MetricsConstants.NAME_APPLICATION_COUNT,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_APPLICATION)),
            this.applicationDAO,
            new ToDoubleFunction<ApplicationDAO>() {
                @Override
                public double applyAsDouble(ApplicationDAO applicationDAO) {
                    return applicationDAO.countApps();
                }
            }
        );
        // 主机
        meterRegistry.gauge(
            MetricsConstants.NAME_HOST_COUNT,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_HOST)),
            this.applicationHostDAO,
            new ToDoubleFunction<ApplicationHostDAO>() {
                @Override
                public double applyAsDouble(ApplicationHostDAO dao) {
                    return dao.countAllHosts();
                }
            }
        );
        // 业务同步延迟
        meterRegistry.gauge(
            MetricsConstants.NAME_APPLICATION_SYNC_AFTER_LAST_SECONDS,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_APPLICATION)),
            this.syncService,
            new ToDoubleFunction<SyncService>() {
                @Override
                public double applyAsDouble(SyncService syncService) {
                    Long lastFinishTime = syncService.getLastFinishTimeSyncApp();
                    if (lastFinishTime == null) {
                        return -1L;
                    } else {
                        return (System.currentTimeMillis() - lastFinishTime) / 1000.0;
                    }
                }
            }
        );
        // 主机同步延迟
        meterRegistry.gauge(
            MetricsConstants.NAME_HOST_SYNC_AFTER_LAST_SECONDS,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_HOST)),
            this.syncService,
            new ToDoubleFunction<SyncService>() {
                @Override
                public double applyAsDouble(SyncService syncService) {
                    Long lastFinishTime = syncService.getLastFinishTimeSyncHost();
                    if (lastFinishTime == null) {
                        return -1L;
                    } else {
                        return (System.currentTimeMillis() - lastFinishTime) / 1000.0;
                    }
                }
            }
        );
        // 主机Agent状态同步延迟
        meterRegistry.gauge(
            MetricsConstants.NAME_HOST_AGENT_STATUS_SYNC_AFTER_LAST_SECONDS,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_HOST)),
            this.syncService,
            new ToDoubleFunction<SyncService>() {
                @Override
                public double applyAsDouble(SyncService syncService) {
                    Long lastFinishTime = syncService.getLastFinishTimeSyncAgentStatus();
                    if (lastFinishTime == null) {
                        return -1L;
                    } else {
                        return (System.currentTimeMillis() - lastFinishTime) / 1000.0;
                    }
                }
            }
        );
        // 账号
        meterRegistry.gauge(
            MetricsConstants.NAME_ACCOUNT_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_ACCOUNT)
            ),
            this.accountDAO,
            new ToDoubleFunction<AccountDAO>() {
                @Override
                public double applyAsDouble(AccountDAO accountDAO) {
                    return accountDAO.countAccounts(null);
                }
            }
        );
        // 脚本
        meterRegistry.gauge(
            MetricsConstants.NAME_SCRIPT_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SCRIPT)
            ),
            this.scriptDAO,
            new ToDoubleFunction<ScriptDAO>() {
                @Override
                public double applyAsDouble(ScriptDAO scriptDAO) {
                    return scriptDAO.countScripts();
                }
            }
        );
        // 作业模板
        meterRegistry.gauge(
            MetricsConstants.NAME_TEMPLATE_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_TEMPLATE)
            ),
            this.taskTemplateService,
            new ToDoubleFunction<TaskTemplateService>() {
                @Override
                public double applyAsDouble(TaskTemplateService taskTemplateService) {
                    return taskTemplateService.countTemplates(null);
                }
            }
        );
        // 执行方案
        meterRegistry.gauge(
            MetricsConstants.NAME_TASK_PLAN_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_TASK_PLAN)
            ),
            this.taskPlanService,
            new ToDoubleFunction<TaskPlanService>() {
                @Override
                public double applyAsDouble(TaskPlanService taskPlanService) {
                    return taskPlanService.countTaskPlans(null);
                }
            }
        );
        // IP白名单
        meterRegistry.gauge(
            MetricsConstants.NAME_WHITE_IP_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_WHITE_IP)
            ),
            this.whiteIPRecordDAO,
            new ToDoubleFunction<WhiteIPRecordDAO>() {
                @Override
                public double applyAsDouble(WhiteIPRecordDAO whiteIPRecordDAO) {
                    return whiteIPRecordDAO.countWhiteIPIP();
                }
            }
        );
        // 同步线程池监控：业务
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_APP_EXECUTOR_POOL_SIZE,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncAppExecutor().getPoolSize()
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_APP_EXECUTOR_QUEUE_SIZE,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncAppExecutor().getQueue().size()
        );
        // 同步线程池监控：主机
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_HOST_EXECUTOR_POOL_SIZE,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncHostExecutor().getPoolSize()
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_HOST_EXECUTOR_QUEUE_SIZE,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncHostExecutor().getQueue().size()
        );
        // 同步线程池监控：Agent状态
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_AGENT_STATUS_EXECUTOR_POOL_SIZE,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncAgentStatusExecutor().getPoolSize()
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_AGENT_STATUS_EXECUTOR_QUEUE_SIZE,
            Arrays.asList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncAgentStatusExecutor().getQueue().size()
        );
    }
}
