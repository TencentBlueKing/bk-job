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
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final ThreadPoolExecutor syncAppExecutor;

    @Autowired
    public MeasureServiceImpl(MeterRegistry meterRegistry, AccountDAO accountDAO,
                              ApplicationDAO applicationDAO, ApplicationHostDAO applicationHostDAO,
                              ScriptDAO scriptDAO, WhiteIPRecordDAO whiteIPRecordDAO,
                              TaskTemplateService taskTemplateService, TaskPlanService taskPlanService,
                              SyncService syncService, ThreadPoolExecutor syncAppExecutor) {
        this.meterRegistry = meterRegistry;
        this.accountDAO = accountDAO;
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.scriptDAO = scriptDAO;
        this.whiteIPRecordDAO = whiteIPRecordDAO;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.syncService = syncService;
        this.syncAppExecutor = syncAppExecutor;
    }

    @Override
    public void init() {
        measureAppAndHostCount();
        measureAppSyncDelay();
        measureHostSyncDelay();
        measureAgentStatusSyncDelay();
        measureAccountCount();
        measureScriptCount();
        measureTemplateCount();
        measurePlanCount();
        measureWhiteIpCount();
        measureSyncAppExecutor();
        measureSyncHostExecutor();
        measureSyncAgentStatusExecutor();
    }

    private void measureAppAndHostCount() {
        // 业务
        meterRegistry.gauge(
            MetricsConstants.NAME_APPLICATION_COUNT,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE,
                MetricsConstants.TAG_VALUE_MODULE_APPLICATION)),
            this.applicationDAO,
            ApplicationDAO::countApps
        );
        // 主机
        meterRegistry.gauge(
            MetricsConstants.NAME_HOST_COUNT,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_HOST)),
            this.applicationHostDAO,
            ApplicationHostDAO::countAllHosts
        );
    }

    private void measureAppSyncDelay() {
        // 业务同步延迟
        meterRegistry.gauge(
            MetricsConstants.NAME_APPLICATION_SYNC_AFTER_LAST_SECONDS,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE,
                MetricsConstants.TAG_VALUE_MODULE_APPLICATION)),
            this.syncService,
            syncService -> {
                Long lastFinishTime = syncService.getLastFinishTimeSyncApp();
                if (lastFinishTime == null) {
                    return -1L;
                } else {
                    return (System.currentTimeMillis() - lastFinishTime) / 1000.0;
                }
            }
        );
    }

    private void measureHostSyncDelay() {
        // 主机同步延迟
        meterRegistry.gauge(
            MetricsConstants.NAME_HOST_SYNC_AFTER_LAST_SECONDS,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_HOST)),
            this.syncService,
            syncService -> {
                Long lastFinishTime = syncService.getLastFinishTimeSyncHost();
                if (lastFinishTime == null) {
                    return -1L;
                } else {
                    return (System.currentTimeMillis() - lastFinishTime) / 1000.0;
                }
            }
        );
    }

    private void measureAgentStatusSyncDelay() {
        // 主机Agent状态同步延迟
        meterRegistry.gauge(
            MetricsConstants.NAME_HOST_AGENT_STATUS_SYNC_AFTER_LAST_SECONDS,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_HOST)),
            this.syncService,
            syncService -> {
                Long lastFinishTime = syncService.getLastFinishTimeSyncAgentStatus();
                if (lastFinishTime == null) {
                    return -1L;
                } else {
                    return (System.currentTimeMillis() - lastFinishTime) / 1000.0;
                }
            }
        );
    }

    private void measureAccountCount() {
        // 账号
        meterRegistry.gauge(
            MetricsConstants.NAME_ACCOUNT_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_ACCOUNT)
            ),
            this.accountDAO,
            accountDAO -> accountDAO.countAccounts(null)
        );
    }

    private void measureScriptCount() {
        // 脚本
        meterRegistry.gauge(
            MetricsConstants.NAME_SCRIPT_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SCRIPT)
            ),
            this.scriptDAO,
            ScriptDAO::countScripts
        );
    }

    private void measureTemplateCount() {
        // 作业模板
        meterRegistry.gauge(
            MetricsConstants.NAME_TEMPLATE_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_TEMPLATE)
            ),
            this.taskTemplateService,
            taskTemplateService -> taskTemplateService.countTemplates(null)
        );
    }

    private void measurePlanCount() {
        // 执行方案
        meterRegistry.gauge(
            MetricsConstants.NAME_TASK_PLAN_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_TASK_PLAN)
            ),
            this.taskPlanService,
            taskPlanService -> taskPlanService.countTaskPlans(null)
        );
    }

    private void measureWhiteIpCount() {
        // IP白名单
        meterRegistry.gauge(
            MetricsConstants.NAME_WHITE_IP_COUNT_ALL,
            Arrays.asList(
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_RESOURCE),
                Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_WHITE_IP)
            ),
            this.whiteIPRecordDAO,
            WhiteIPRecordDAO::countWhiteIPIP
        );
    }

    private void measureSyncAppExecutor() {
        // 同步线程池监控：业务
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_APP_EXECUTOR_POOL_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncAppExecutor,
            ThreadPoolExecutor::getPoolSize
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_APP_EXECUTOR_QUEUE_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncAppExecutor,
            syncAppExecutor -> syncAppExecutor.getQueue().size()
        );
    }

    private void measureSyncHostExecutor() {
        // 同步线程池监控：主机
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_HOST_EXECUTOR_POOL_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncHostExecutor().getPoolSize()
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_HOST_EXECUTOR_QUEUE_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncHostExecutor().getQueue().size()
        );
    }

    private void measureSyncAgentStatusExecutor() {
        // 同步线程池监控：Agent状态
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_AGENT_STATUS_EXECUTOR_POOL_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncAgentStatusExecutor().getPoolSize()
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_SYNC_AGENT_STATUS_EXECUTOR_QUEUE_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_SYNC)),
            this.syncService,
            syncService1 -> syncService1.getSyncAgentStatusExecutor().getQueue().size()
        );
    }
}
