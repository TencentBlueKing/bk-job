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

package com.tencent.bk.job.execute.dao.common;

import com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.toggle.prop.PropChangeEventListener;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.FAIL;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.MIGRATED;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.MIGRATING;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.NOT_START;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.PREPARING;

/**
 * 基于属性动态控制的组件
 */

public abstract class AbstractPropBasedDynamicComponent<C> implements PropChangeEventListener {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    /**
     * 候选的组件。key: 属性名称；value: 组件实例
     */
    private Map<String, C> candidateComponents;

    private volatile MigrationStatus status = NOT_START;

    private final Object lock = new Object();

    private final RedisTemplate<String, Object> redisTemplate;

    private volatile C current;

    private final PropToggleStore propToggleStore;

    /**
     * 动态属性名称 - 迁移目标
     */
    private final String migrateTargetPropName;
    /**
     * 动态属性名称 - 参与迁移的微服务实例数量
     */
    private static final String PROP_NAME_SERVICE_INSTANCE_COUNT = "job_execute_service_instance_count";

    /**
     * 迁移使用的分布式锁 Redis KEY
     */
    private final String migrationLockRedisKey;
    /**
     * 整体迁移状态 Redis KEY
     */
    private final String globalMigrationStatusRedisKey;
    /**
     * 服务实例迁移状态 Redis KEY
     */
    private final String serviceInstanceMigrationStatusRedisKey;
    /**
     * 服务节点 IP
     */
    private final String serviceNodeIp;

    private static final String REDIS_KEY_PREFIX = "job:comp:mig:";

    public AbstractPropBasedDynamicComponent(
        RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore,
        String migrateComponentName) {
        this.redisTemplate = redisTemplate;
        this.propToggleStore = propToggleStore;

        this.serviceNodeIp = IpUtils.getFirstMachineIP();
        this.migrateTargetPropName = migrateComponentName;
        this.migrationLockRedisKey = REDIS_KEY_PREFIX + migrateComponentName;
        this.globalMigrationStatusRedisKey = REDIS_KEY_PREFIX + "global:status:" + migrateComponentName;
        this.serviceInstanceMigrationStatusRedisKey = REDIS_KEY_PREFIX + ":svc:status:" + migrateComponentName;

        // 注册监听属性变化
        this.propToggleStore.addPropChangeEventListener(migrateTargetPropName, this);
    }

    protected void initCandidateComponents(Map<String, C> candidateComponents) {
        this.candidateComponents = candidateComponents;
        initCurrentComponent();
    }

    private void initCurrentComponent() {
        // 从配置文件读取组件名称，并初始化
        String propValue = propToggleStore.getPropToggle(migrateTargetPropName).getDefaultValue();
        log.info("Init default component by prop: {}:{}", migrateTargetPropName, propValue);
        this.current = getComponentByProp(this.candidateComponents, propValue);
        if (this.current == null) {
            log.error("No match component for {}", propValue);
            throw new IllegalStateException("Unsupported component define by prop: " + propValue);
        }
        log.info("Use {} as default component", current.getClass());
    }

    /**
     * 获取当前组件
     * 如果组件处于迁移状态中，会阻塞当前线程直到切换完成
     */
    public C getCurrent(boolean blockWhenMigration) {
        if (status == MigrationStatus.MIGRATING && blockWhenMigration) {
            // 如果组件正在迁移中，并且 blockWhenMigration = true, 需要等待迁移完成；当前线程阻塞
            try {
                synchronized (lock) {
                    log.info("Component is migrating, block until migrated");
                    lock.wait();
                    log.info("Continue after component migrated");
                }
            } catch (InterruptedException e) {
                log.error("Get current component error", e);
            }
        }
        return current;
    }


    @Override
    public void handlePropChangeEvent(String propName, PropToggle currentValue) {
        log.info("Handle prop change event, propName: {}, value: {}", propName, currentValue.getDefaultValue());
        C targetComponent = getComponentByProp(this.candidateComponents, currentValue.getDefaultValue());
        if (targetComponent == null) {
            log.warn("No match component for prop : {}", currentValue.getDefaultValue());
            return;
        }
        if (targetComponent == current) {
            // 迁移目标不变，无需处理
            log.info("Component is not changed, skip migration!");
            return;
        }

        migrateComponent(targetComponent);
    }

    private void migrateComponent(C targetComponent) {
        boolean success = false;
        try {
            long startTime = System.currentTimeMillis();
            log.info("Migrate component from {} to {} start...", current.getClass(), targetComponent.getClass());
            PropToggle migrateServiceInstanceCountProp =
                propToggleStore.getPropToggle(PROP_NAME_SERVICE_INSTANCE_COUNT);
            if (migrateServiceInstanceCountProp == null) {
                log.error("Prop [" + PROP_NAME_SERVICE_INSTANCE_COUNT + "] is required. Skip migration");
                return;
            }
            int migrateServiceInstanceCount = Integer.parseInt(migrateServiceInstanceCountProp.getDefaultValue());
            if (status == NOT_START) {
                synchronized (this) {
                    if (status == NOT_START) {
                        status = PREPARING;
                        initServiceInstanceMigrationStatus();
                        // 为了在同一时间(接近）在多个微服务实例切换到新的组件，需要判断处于 PREPARING 状态的服务实例是否符合预期数量
                        updateServiceInstanceMigrationStatus(PREPARING);
                        while (true) {
                            if (System.currentTimeMillis() - startTime > 60000L) {
                                // 超过一分钟，放弃本次迁移
                                log.info("Prepare migration cost 1min, terminate migration");
                                updateServiceInstanceMigrationStatus(MigrationStatus.FAIL);
                                return;
                            }
                            long prepareServiceInstanceCount =
                                redisTemplate.opsForHash().size(serviceInstanceMigrationStatusRedisKey);
                            if (prepareServiceInstanceCount >= migrateServiceInstanceCount) {
                                log.info("All service node are ready, actualReadyNodeCount: {}, expected: {}",
                                    prepareServiceInstanceCount, migrateServiceInstanceCount);
                                // 所有服务实例都确认收到组件迁移事件，准备启动迁移
                                updateGlobalMigrationStatus(MIGRATING);
                                updateServiceInstanceMigrationStatus(MIGRATING);
                                // 等待 5s，等待当前组件正在执行的操作完成（比如 db 读写请求)
                                log.info("Wait 5s before migration");
                                ThreadUtils.sleep(5000L);
                                break;
                            } else {
                                log.info("Wait all service instance ready, actual: {}, expected: {}",
                                    prepareServiceInstanceCount, migrateServiceInstanceCount);
                                ThreadUtils.sleep(100L);
                            }
                        }
                        this.current = targetComponent;
                        updateServiceInstanceMigrationStatus(MIGRATED);
                        success = true;
                    } else {
                        log.warn("Unexpected migration status {}, skip migration", status);
                    }
                }
            } else {
                log.warn("Unexpected migration status {}, skip migration", status);
            }
        } catch (Throwable e) {
            log.error("Migrate component error", e);
        } finally {
            status = NOT_START;
            clearAfterMigrated();
            synchronized (lock) {
                lock.notifyAll();
            }
            log.info("Migrate component done, isSuccess : {}", success);
        }
    }

    private void clearAfterMigrated() {
        try {
            List<Object> allServiceInstanceMigStatus =
                redisTemplate.opsForHash().values(serviceInstanceMigrationStatusRedisKey);
            List<MigrationStatus> serviceInstancesMigStatusList =
                castList(allServiceInstanceMigStatus, k -> MigrationStatus.valOf((Integer) k));
            if (serviceInstancesMigStatusList.stream().allMatch(status -> status == MIGRATED || status == FAIL)) {
                // 所有服务实例都完成了组件迁移，开始清理
                log.info("All migration done. Delete all migration temporary data");
                redisTemplate.delete(serviceInstanceMigrationStatusRedisKey);
                redisTemplate.delete(globalMigrationStatusRedisKey);
            } else {
                log.info("Some service instance node not yet complete migration");
            }
        } catch (Throwable e) {
            log.error("Clear migration caught exception", e);
        }
    }

    private void updateServiceInstanceMigrationStatus(MigrationStatus migrationStatus) {
        log.info("Update service instance migration status {}", migrationStatus);
        redisTemplate.opsForHash().put(serviceInstanceMigrationStatusRedisKey,
            serviceNodeIp, migrationStatus.getStatus());
        this.status = migrationStatus;
    }

    private void updateGlobalMigrationStatus(MigrationStatus migrationStatus) {
        log.info("Update global migration status {}", migrationStatus);
        redisTemplate.opsForValue().set(globalMigrationStatusRedisKey,
            migrationStatus.getStatus());
    }

    private <T, V> List<V> castList(List<T> source, Function<T, V> mapping) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        List<V> list = new ArrayList<>(source.size());
        list.addAll(source.stream().map(mapping).collect(Collectors.toList()));
        return list;
    }

    private void initServiceInstanceMigrationStatus() {
        MigrationStatus migrationStatus = getGlobalMigrationStatus();
        if (migrationStatus == null) {
            boolean locked = false;
            try {
                locked = lockForUpdate();
                if (locked) {
                    // 二次确认，保证获取锁期间数据没有发生改变
                    migrationStatus = getGlobalMigrationStatus();
                    if (migrationStatus == null) {
                        // 初始化迁移状态
                        log.info("Init migration status");
                        redisTemplate.opsForValue().set(globalMigrationStatusRedisKey, PREPARING.getStatus());
                        redisTemplate.delete(serviceInstanceMigrationStatusRedisKey);
                    }
                }
            } finally {
                if (locked) {
                    unlock();
                }
            }
        }
    }

    private MigrationStatus getGlobalMigrationStatus() {
        Object migrationStatus = redisTemplate.opsForValue().get(globalMigrationStatusRedisKey);
        if (migrationStatus == null) {
            return null;
        }
        return MigrationStatus.valOf((Integer) migrationStatus);
    }

    private boolean lockForUpdate() {
        return LockUtils.lock(migrationLockRedisKey, serviceNodeIp, 60000L, 60);
    }

    private boolean unlock() {
        return LockUtils.releaseDistributedLock(migrationLockRedisKey, serviceNodeIp);
    }

    protected abstract C getComponentByProp(Map<String, C> candidateComponents, String propValue);
}
