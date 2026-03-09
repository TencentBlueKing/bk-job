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
import com.tencent.bk.job.manage.background.ha.BackGroundTask;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskRegistryImpl;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskDispatcher;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskListenerController;
import com.tencent.bk.job.manage.common.constants.SmartLifecycleOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CMDB事件管理器，负责处理CMDB事件监听相关逻辑
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class CmdbEventManagerImpl implements CmdbEventManager, SmartLifecycle {

    RedisTemplate<String, String> redisTemplate;
    private final EventWatcherFactory eventWatcherFactory;
    private final ThreadPoolExecutor shutdownEventWatchExecutor;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;
    private final BackGroundTaskListenerController backGroundTaskListenerController;
    private final BackGroundTaskRegistryImpl backGroundTaskRegistry;
    private volatile boolean running = false;

    @Autowired
    public CmdbEventManagerImpl(RedisTemplate<String, String> redisTemplate,
                                EventWatcherFactory eventWatcherFactory,
                                ThreadPoolExecutor shutdownEventWatchExecutor,
                                BackGroundTaskDispatcher backGroundTaskDispatcher,
                                BackGroundTaskListenerController backGroundTaskListenerController,
                                BackGroundTaskRegistryImpl backGroundTaskRegistryImpl) {
        this.redisTemplate = redisTemplate;
        this.eventWatcherFactory = eventWatcherFactory;
        this.shutdownEventWatchExecutor = shutdownEventWatchExecutor;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.backGroundTaskListenerController = backGroundTaskListenerController;
        this.backGroundTaskRegistry = backGroundTaskRegistryImpl;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        log.debug("CmdbEventManager stop");
        running = false;
        gracefulShutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return SmartLifecycleOrder.CMDB_EVENT_MANAGER;
    }

    /**
     * 判断业务事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchBizEventRunning(String tenantId) {
        return BizEventWatcher.hasRunningInstance(redisTemplate, TaskEntity.ofWatchBiz(tenantId));
    }

    /**
     * 判断业务集事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchBizSetEventRunning(String tenantId) {
        return BizSetEventWatcher.hasRunningInstance(redisTemplate, TaskEntity.ofWatchBizSet(tenantId));
    }

    /**
     * 判断业务集事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchBizSetRelationEventRunning(String tenantId) {
        return BizSetRelationEventWatcher.hasRunningInstance(redisTemplate, TaskEntity.ofWatchBizSetRelation(tenantId));
    }

    /**
     * 判断主机事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchHostEventRunning(String tenantId) {
        return HostEventWatcher.hasRunningInstance(redisTemplate, TaskEntity.ofWatchHost(tenantId));
    }

    /**
     * 判断主机关系事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    @Override
    public boolean isWatchHostRelationEventRunning(String tenantId) {
        return HostRelationEventWatcher.hasRunningInstance(redisTemplate, TaskEntity.ofWatchHostRelation(tenantId));
    }

    /**
     * 监听业务相关的事件
     */
    @Override
    public boolean startWatchBizEvent(String tenantId) {
        if (isWatchBizEventRunning(tenantId)) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        BizEventWatcher bizEventWatcher = eventWatcherFactory.getOrCreateBizEventWatcher(tenantId);
        return registerAndStartTask(bizEventWatcher);
    }

    /**
     * 监听业务集相关的事件
     */
    @Override
    public boolean startWatchBizSetEvent(String tenantId) {
        if (isWatchBizSetEventRunning(tenantId)) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        BizSetEventWatcher bizSetEventWatcher = eventWatcherFactory.getOrCreateBizSetEventWatcher(tenantId);
        return registerAndStartTask(bizSetEventWatcher);
    }

    /**
     * 监听业务集相关的事件
     */
    @Override
    public boolean startWatchBizSetRelationEvent(String tenantId) {
        if (isWatchBizSetRelationEventRunning(tenantId)) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        BizSetRelationEventWatcher bizSetRelationEventWatcher =
            eventWatcherFactory.getOrCreateBizSetRelationEventWatcher(tenantId);
        return registerAndStartTask(bizSetRelationEventWatcher);
    }

    /**
     * 监听主机相关的事件
     */
    @Override
    public boolean startWatchHostEvent(String tenantId) {
        if (isWatchHostEventRunning(tenantId)) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        HostEventWatcher hostEventWatcher = eventWatcherFactory.getOrCreateHostEventWatcher(tenantId);
        return registerAndStartTask(hostEventWatcher);
    }

    /**
     * 监听主机关系相关的事件
     */
    @Override
    public boolean startWatchHostRelationEvent(String tenantId) {
        if (isWatchHostRelationEventRunning(tenantId)) {
            // 已经有在运行的实例就不再启动新的实例
            return false;
        }
        HostRelationEventWatcher hostRelationEventWatcher =
            eventWatcherFactory.getOrCreateHostRelationEventWatcher(tenantId);
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

    /**
     * 优雅关闭，停止所有已注册的后台任务并将其重调度到其他实例
     */
    private void gracefulShutdown() {
        log.info("GracefulShutdown: shutdown all tasks and re-schedule them");
        // 1.关闭任务接收通道
        backGroundTaskListenerController.stop();
        // 2.停止所有任务，并将其重新调度至其他实例
        List<Future<?>> futureList = new ArrayList<>(backGroundTaskRegistry.getTaskMap().size());
        for (BackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            Future<?> future = shutdownEventWatchExecutor.submit(() -> {
                TaskEntity taskEntity = task.getTaskEntity();
                task.shutdownGracefully();
                backGroundTaskDispatcher.dispatch(taskEntity);
                log.info("task {} rescheduled", taskEntity.getUniqueCode());
            });
            futureList.add(future);
        }
        // 3.等待所有停止任务完成
        for (Future<?> future : futureList) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.warn("Fail to wait all stop tasks finish", e);
            }
        }
        List<Runnable> remainTaskList = shutdownEventWatchExecutor.shutdownNow();
        log.info("GracefulShutdown end, {} task remain", remainTaskList.size());
    }

}
