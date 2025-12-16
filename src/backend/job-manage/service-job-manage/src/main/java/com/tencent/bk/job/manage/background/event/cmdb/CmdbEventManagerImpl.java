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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.manage.api.common.constants.EventWatchTaskTypeEnum;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.factory.EventWatcherFactory;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskRegistryImpl;
import com.tencent.bk.job.manage.background.ha.BackGroundTask;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskDispatcher;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskListenerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * CMDB事件管理器，负责处理CMDB事件监听相关逻辑
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class CmdbEventManagerImpl implements CmdbEventManager, DisposableBean {

    private final EventWatcherFactory eventWatcherFactory;
    private final ThreadPoolExecutor shutdownEventWatchExecutor;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;
    private final BackGroundTaskListenerController backGroundTaskListenerController;
    private final BackGroundTaskRegistryImpl backGroundTaskRegistry;

    @Autowired
    public CmdbEventManagerImpl(EventWatcherFactory eventWatcherFactory,
                                ThreadPoolExecutor shutdownEventWatchExecutor,
                                BackGroundTaskDispatcher backGroundTaskDispatcher,
                                BackGroundTaskListenerController backGroundTaskListenerController,
                                BackGroundTaskRegistryImpl backGroundTaskRegistryImpl) {
        this.eventWatcherFactory = eventWatcherFactory;
        this.shutdownEventWatchExecutor = shutdownEventWatchExecutor;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.backGroundTaskListenerController = backGroundTaskListenerController;
        this.backGroundTaskRegistry = backGroundTaskRegistryImpl;
    }

    /**
     * 判断业务事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchBizEventRunning(String tenantId) {
        BizEventWatcher tenantBizEventWatcher = eventWatcherFactory.getOrCreateBizEventWatcher(tenantId);
        return tenantBizEventWatcher.hasRunningInstance();
    }

    /**
     * 判断业务集事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchBizSetEventRunning(String tenantId) {
        BizSetEventWatcher bizSetEventWatcher = eventWatcherFactory.getOrCreateBizSetEventWatcher(tenantId);
        return bizSetEventWatcher.hasRunningInstance();
    }

    /**
     * 判断业务集事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchBizSetRelationEventRunning(String tenantId) {
        BizSetRelationEventWatcher bizSetRelationEventWatcher =
            eventWatcherFactory.getOrCreateBizSetRelationEventWatcher(tenantId);
        return bizSetRelationEventWatcher.hasRunningInstance();
    }

    /**
     * 判断主机事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchHostEventRunning(String tenantId) {
        HostEventWatcher hostEventWatcher = eventWatcherFactory.getOrCreateHostEventWatcher(tenantId);
        return hostEventWatcher.hasRunningInstance();
    }

    /**
     * 判断主机关系事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchHostRelationEventRunning(String tenantId) {
        HostRelationEventWatcher hostRelationEventWatcher =
            eventWatcherFactory.getOrCreateHostRelationEventWatcher(tenantId);
        return hostRelationEventWatcher.hasRunningInstance();
    }

    /**
     * 监听业务相关的事件
     */
    @Override
    public boolean startWatchBizEvent(String tenantId) {
        BizEventWatcher bizEventWatcher = eventWatcherFactory.getOrCreateBizEventWatcher(tenantId);
        if (bizEventWatcher.hasRunningInstance()) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        return registerAndStartTask(bizEventWatcher);
    }

    /**
     * 监听业务集相关的事件
     */
    @Override
    public boolean startWatchBizSetEvent(String tenantId) {
        BizSetEventWatcher bizSetEventWatcher = eventWatcherFactory.getOrCreateBizSetEventWatcher(tenantId);
        if (bizSetEventWatcher.hasRunningInstance()) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        return registerAndStartTask(bizSetEventWatcher);
    }

    /**
     * 监听业务集相关的事件
     */
    @Override
    public boolean startWatchBizSetRelationEvent(String tenantId) {
        BizSetRelationEventWatcher bizSetRelationEventWatcher =
            eventWatcherFactory.getOrCreateBizSetRelationEventWatcher(tenantId);
        if (bizSetRelationEventWatcher.hasRunningInstance()) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        return registerAndStartTask(bizSetRelationEventWatcher);
    }

    /**
     * 监听主机相关的事件
     */
    @Override
    public boolean startWatchHostEvent(String tenantId) {
        HostEventWatcher hostEventWatcher = eventWatcherFactory.getOrCreateHostEventWatcher(tenantId);
        if (hostEventWatcher.hasRunningInstance()) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        return registerAndStartTask(hostEventWatcher);
    }

    /**
     * 监听主机关系相关的事件
     */
    @Override
    public boolean startWatchHostRelationEvent(String tenantId) {
        HostRelationEventWatcher hostRelationEventWatcher =
            eventWatcherFactory.getOrCreateHostRelationEventWatcher(tenantId);
        if (hostRelationEventWatcher.hasRunningInstance()) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        return registerAndStartTask(hostRelationEventWatcher);
    }

    /**
     * 注册并启动一个后台任务
     *
     * @param task 后台任务
     * @return 是否启动成功
     */
    private boolean registerAndStartTask(BackGroundTask task) {
        String uniqueCode = task.getUniqueCode();
        if (backGroundTaskRegistry.existsTask(uniqueCode)) {
            log.warn("task {} already exists in registry, ignore", uniqueCode);
            return false;
        }
        boolean registerSuccess = backGroundTaskRegistry.registerTask(uniqueCode, task);
        if (registerSuccess) {
            task.startTask();
            return true;
        } else {
            log.warn("Fail to register task {}, ignore", uniqueCode);
            return false;
        }
    }

    /**
     * 从已注册的后台任务中找出所有指定类型的监听器并开启
     *
     * @param taskType 任务类型
     * @return 开启的事件监听器数量
     */
    @Override
    public Integer enableWatch(EventWatchTaskTypeEnum taskType) {
        int watcherCount = 0;
        for (BackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof CmdbEventWatcher && task.getTaskEntity().getTaskType() == taskType) {
                CmdbEventWatcher watcherTask = (CmdbEventWatcher) task;
                watcherTask.setWatchEnabled(true);
                watcherCount++;
            }
        }
        log.info("Watchers of {} enabled, watcherCount={}", taskType, watcherCount);
        return watcherCount;
    }

    /**
     * 从已注册的后台任务中找出所有指定类型的监听器并禁用
     *
     * @param taskType 任务类型
     * @return 禁用的事件监听器数量
     */
    @Override
    public Integer disableWatch(EventWatchTaskTypeEnum taskType) {
        int watcherCount = 0;
        for (BackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof CmdbEventWatcher && task.getTaskEntity().getTaskType() == taskType) {
                CmdbEventWatcher watcherTask = (CmdbEventWatcher) task;
                watcherTask.setWatchEnabled(false);
                watcherCount++;
            }
        }
        log.info("Watchers of {} disabled, watcherCount={}", taskType, watcherCount);
        return watcherCount;
    }

    @Override
    public void destroy() {
        log.info("On destroy, shutdown all tasks and re-schedule them");
        // 1.关闭任务接收通道
        backGroundTaskListenerController.stop();
        // 2.停止所有任务，并将其重新调度至其他实例
        for (BackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            shutdownEventWatchExecutor.submit(() -> {
                TaskEntity taskEntity = task.getTaskEntity();
                task.shutdownGracefully();
                backGroundTaskDispatcher.dispatch(taskEntity);
                log.info("task {} rescheduled", taskEntity.getUniqueCode());
            });
        }
    }
}
