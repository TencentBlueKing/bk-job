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

import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.toggle.prop.PropChangeEventListener;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.FAIL;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.MIGRATED;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.MIGRATING;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.NOT_START;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.PREPARING;

/**
 * 基于属性动态控制的数据源，可以根据属性值切换到不同的数据源
 */
@Slf4j
public class PropBasedDynamicDataSource implements PropChangeEventListener {

    private final StandaloneDSLContextProvider standaloneDSLContextProvider;

    private final VerticalShardingDSLContextProvider verticalShardingDSLContextProvider;

    private volatile MigrationStatus status = NOT_START;

    private final Object lock = new Object();

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 当前数据源模式
     */
    private volatile DataSourceMode currentDataSourceMode;

    /**
     * 当前 DSLContextProvider
     */
    private volatile DSLContextProvider currentContextProvider;

    private final PropToggleStore propToggleStore;

    private volatile boolean isInitial = false;

    /**
     * 动态属性名称 - 迁移目标数据源类型
     */
    private static final String PROP_NAME_MIGRATE_TARGET_DATASOURCE_MODE =
        "job_execute_mysql_migration_target_datasource_mode";
    /**
     * 动态属性名称 - 参与 db 迁移的微服务实例数量
     */
    private static final String PROP_NAME_SERVICE_INSTANCE_COUNT = "job_execute_mysql_migration_service_instance_count";

    /**
     * 迁移使用的分布式锁 Redis KEY
     */
    private static final String DB_MIGRATION_LOCK_KEY = "job:execute:mysql:migration";
    /**
     * 整体迁移状态 Redis KEY
     */
    private static final String REDIS_KEY_GLOBAL_MIGRATION_STATUS = "job:execute:mysql:migration:global:status";
    /**
     * 服务实例迁移状态 Redis KEY
     */
    private static final String REDIS_KEY_SERVICE_INSTANCE_MIGRATION_STATUS =
        "job:execute:mysql:migration:service:instance:status";
    /**
     * 服务节点 IP
     */
    private final String serviceNodeIp;

    public PropBasedDynamicDataSource(
        StandaloneDSLContextProvider standaloneDSLContextProvider,
        VerticalShardingDSLContextProvider verticalShardingDSLContextProvider,
        RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore) {
        this.standaloneDSLContextProvider = standaloneDSLContextProvider;
        this.verticalShardingDSLContextProvider = verticalShardingDSLContextProvider;
        this.redisTemplate = redisTemplate;
        this.propToggleStore = propToggleStore;
        this.serviceNodeIp = IpUtils.getFirstMachineIP();
        // 注册监听属性变化
        this.propToggleStore.addPropChangeEventListener(PROP_NAME_MIGRATE_TARGET_DATASOURCE_MODE, this);
    }

    /**
     * 获取当前 DSLContextProvider。
     * 如果处于db 数据源切换中，会阻塞当前线程直到切换完成
     *
     * @return DSLContextProvider
     */
    public DSLContextProvider getCurrent(MySQLOperation op) {
        checkInit();
        // 迁移时阻塞写请求，读请求不影响
        if (op.op() == DbOperationEnum.WRITE) {
            if (status == MigrationStatus.MIGRATING) {
                // 如果数据源正在迁移中，需要等待迁移完成；当前线程阻塞
                try {
                    synchronized (lock) {
                        log.debug("Datasource is migrating, wait unit migrated");
                        lock.wait();
                        log.debug("Continue after datasource migrated");
                    }
                } catch (InterruptedException e) {
                    log.error("Get current DSLContext error", e);
                }
            }
        }
        return currentContextProvider;
    }

    private void checkInit() {
        if (!isInitial) {
            synchronized (this) {
                if (!isInitial) {
                    // 设置当前数据源
                    DataSourceMode dataSourceMode = DataSourceMode.valOf(
                        propToggleStore.getPropToggle(PROP_NAME_MIGRATE_TARGET_DATASOURCE_MODE).getDefaultValue());
                    log.info("Init default DSLContextProvider, dataSourceMode: {}", dataSourceMode);
                    this.currentDataSourceMode = dataSourceMode;
                    this.currentContextProvider = chooseDefaultDSLContextProviderByMode(dataSourceMode);
                    log.info("Use {} as default DSLContextProvider", currentContextProvider.getClass());
                    this.isInitial = true;
                }
            }
        }
    }

    private DSLContextProvider chooseDefaultDSLContextProviderByMode(DataSourceMode mode) {
        if (mode == DataSourceMode.STANDALONE) {
            if (standaloneDSLContextProvider == null) {
                throw new IllegalStateException("StandaloneDSLContextProvider not exist");
            }
            return standaloneDSLContextProvider;
        } else if (mode == DataSourceMode.VERTICAL_SHARDING) {
            if (verticalShardingDSLContextProvider == null) {
                throw new IllegalStateException("VerticalShardingDSLContextProvider not exist");
            }
            return verticalShardingDSLContextProvider;
        } else {
            log.error("Unsupported DataSourceMode");
            throw new IllegalArgumentException("Unsupported DataSourceMode");
        }
    }

    @Override
    public void handlePropChangeEvent(String propName, PropToggle currentValue) {
        log.info("Handle prop change event, propName: {}, value: {}", propName, currentValue != null ?
            currentValue.getDefaultValue() : null);
        String targetDataSourceModeValue = currentValue.getDefaultValue();
        if (!DataSourceMode.checkValid(targetDataSourceModeValue)) {
            log.error("Invalid target datasource mode : {}, skip migration", targetDataSourceModeValue);
            return;
        }
        DataSourceMode targetDataSourceMode = DataSourceMode.valOf(targetDataSourceModeValue);
        if (targetDataSourceMode == currentDataSourceMode) {
            // 迁移目标不变，无需处理
            log.info("DataSourceMode is not changed, skip migration!");
            return;
        }

        migrateDataSource(targetDataSourceMode);
    }

    private void migrateDataSource(DataSourceMode targetDataSourceMode) {
        long startTime = System.currentTimeMillis();
        DSLContextProvider targetDSLContextProvider = chooseDefaultDSLContextProviderByMode(targetDataSourceMode);
        log.info("Migrate datasource from {} to {} start...", currentDataSourceMode, targetDataSourceMode);
        boolean success = false;
        try {
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
                        // 为了在同一时间(接近）在多个微服务实例切换到新的数据源，需要判断处于 PREPARING 状态的服务实例是否符合预期数量
                        updateServiceInstanceMigrationStatus(PREPARING);
                        while (true) {
                            if (System.currentTimeMillis() - startTime > 60000L) {
                                // 超过一分钟，放弃本次迁移
                                log.info("Prepare migration cost 1min, terminate migration");
                                updateServiceInstanceMigrationStatus(MigrationStatus.FAIL);
                                return;
                            }
                            long prepareServiceInstanceCount =
                                redisTemplate.opsForHash().size(REDIS_KEY_SERVICE_INSTANCE_MIGRATION_STATUS);
                            if (prepareServiceInstanceCount >= migrateServiceInstanceCount) {
                                log.info("All service node are ready, actualReadyNodeCount: {}, expected: {}",
                                    prepareServiceInstanceCount, migrateServiceInstanceCount);
                                // 所有服务实例都确认收到DB数据源迁移事件，准备启动迁移
                                updateGlobalMigrationStatus(MIGRATING);
                                updateServiceInstanceMigrationStatus(MIGRATING);
                                // 等待 5s，等待当前DB数据源正在执行的读写请求都完成
                                log.info("Wait 5s before migration");
                                ThreadUtils.sleep(5000L);
                                break;
                            } else {
                                log.info("Wait all service instance ready, actual: {}, expected: {}",
                                    prepareServiceInstanceCount, migrateServiceInstanceCount);
                                ThreadUtils.sleep(100L);
                            }
                        }
                        this.currentDataSourceMode = targetDataSourceMode;
                        this.currentContextProvider = targetDSLContextProvider;
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
            log.error("Migrate datasource error", e);
        } finally {
            status = NOT_START;
            clearAfterMigrated();
            synchronized (lock) {
                lock.notifyAll();
            }
            log.info("Migrate datasource finish, isSuccess : {}", success);
        }
    }

    private void clearAfterMigrated() {
        boolean locked = false;
        try {
            locked = lockForUpdate();
            if (locked) {
                List<Object> allServiceInstanceMigStatus =
                    redisTemplate.opsForHash().values(REDIS_KEY_SERVICE_INSTANCE_MIGRATION_STATUS);
                List<MigrationStatus> serviceInstancesMigStatusList =
                    castList(allServiceInstanceMigStatus, k -> MigrationStatus.valOf((Integer) k));
                if (serviceInstancesMigStatusList.stream().allMatch(status -> status == MIGRATED || status == FAIL)) {
                    // 所有服务实例都完成了 db 迁移，开始清理
                    log.info("All migration done. Delete all migration temporary data");
                    redisTemplate.delete(REDIS_KEY_SERVICE_INSTANCE_MIGRATION_STATUS);
                    redisTemplate.delete(REDIS_KEY_GLOBAL_MIGRATION_STATUS);
                } else {
                    log.info("Some service instance node not yet complete migration");
                }
            }
        } catch (Throwable e) {
            log.error("Clear migration caught exception", e);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    private void updateServiceInstanceMigrationStatus(MigrationStatus migrationStatus) {
        log.info("Update service instance migration status {}", migrationStatus);
        redisTemplate.opsForHash().put(REDIS_KEY_SERVICE_INSTANCE_MIGRATION_STATUS,
            serviceNodeIp, migrationStatus.getStatus());
        this.status = migrationStatus;
    }

    private void updateGlobalMigrationStatus(MigrationStatus migrationStatus) {
        log.info("Update global migration status {}", migrationStatus);
        redisTemplate.opsForValue().set(REDIS_KEY_GLOBAL_MIGRATION_STATUS,
            migrationStatus.getStatus());
    }

    private <T, V> List<V> castList(List<T> source, Function<T, V> mapping) {
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
                        redisTemplate.opsForValue().set(REDIS_KEY_GLOBAL_MIGRATION_STATUS, PREPARING.getStatus());
                        redisTemplate.delete(REDIS_KEY_SERVICE_INSTANCE_MIGRATION_STATUS);
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
        Object migrationStatus = redisTemplate.opsForValue().get(REDIS_KEY_GLOBAL_MIGRATION_STATUS);
        if (migrationStatus == null) {
            return null;
        }
        return MigrationStatus.valOf((Integer) migrationStatus);
    }

    private boolean lockForUpdate() {
        return LockUtils.lock(DB_MIGRATION_LOCK_KEY, serviceNodeIp, 60000L, 60);
    }

    private boolean unlock() {
        return LockUtils.releaseDistributedLock(DB_MIGRATION_LOCK_KEY, serviceNodeIp);
    }
}
