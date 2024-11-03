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
import com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.toggle.prop.PropChangeEventListener;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.IDLE;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.MIGRATED;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.MIGRATING;
import static com.tencent.bk.job.common.mysql.dynamic.ds.MigrationStatus.PREPARING;

/**
 * 基于属性动态控制的数据源，可以根据属性值切换到不同的数据源
 */
@Slf4j
public class PropBasedDynamicDataSource implements PropChangeEventListener {

    private final StandaloneDSLContextProvider standaloneDSLContextProvider;

    private final VerticalShardingDSLContextProvider verticalShardingDSLContextProvider;

    private volatile MigrationStatus status = IDLE;

    private final Object lock = new Object();

    private final RedisTemplate<String, Object> redisTemplate;


    private volatile DataSourceMode currentDataSourceMode;

    private volatile DSLContextProvider currentContextProvider;

    private final PropToggleStore propToggleStore;

    private static final String DB_MIGRATE_READY_SERVICE_INSTANCE_KEY = "job:execute:db:migrate:ready:service:instance";
    private static final String MIGRATE_TARGET_DATASOURCE_MODE_PROP_NAME =
        "job_execute_mysql_migration_target_datasource_mode";
    private static final String SERVICE_INSTANCE_COUNT = "job_execute_mysql_migration_service_instance_count";

    public PropBasedDynamicDataSource(
        StandaloneDSLContextProvider standaloneDSLContextProvider,
        VerticalShardingDSLContextProvider verticalShardingDSLContextProvider,
        RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore) {
        this.standaloneDSLContextProvider = standaloneDSLContextProvider;
        this.verticalShardingDSLContextProvider = verticalShardingDSLContextProvider;
        this.redisTemplate = redisTemplate;
        this.propToggleStore = propToggleStore;
        // 设置当前数据源
        DataSourceMode dataSourceMode = DataSourceMode.valOf(
            propToggleStore.getPropToggle(MIGRATE_TARGET_DATASOURCE_MODE_PROP_NAME).getDefaultValue());
        this.currentDataSourceMode = dataSourceMode;
        this.currentContextProvider = chooseDSLContextProviderByMode(dataSourceMode);
        // 注册监听属性变化
        this.propToggleStore.addPropChangeEventListener(MIGRATE_TARGET_DATASOURCE_MODE_PROP_NAME, this);
    }

    private DSLContextProvider chooseDSLContextProviderByMode(DataSourceMode mode) {
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
            throw new IllegalArgumentException("Unsupported DataSourceMode");
        }
    }


    public DSLContextProvider getCurrent() {
        if (status == MigrationStatus.MIGRATING) {
            try {
                synchronized (lock) {
                    log.info("Wait datasource migration");
                    lock.wait();
                    log.info("Continue after datasource migrated");
                }
            } catch (InterruptedException e) {
                log.error("Get current DSLContext error", e);
            }
        }
        return currentContextProvider;
    }

    @Override
    public void handlePropChangeEvent(String propName, PropToggle currentValue) {
        String targetDataSourceModeValue = currentValue.getDefaultValue();
        if (!DataSourceMode.checkValid(targetDataSourceModeValue)) {
            log.error("Invalid target datasource mode : {}, skip migration", targetDataSourceModeValue);
            return;
        }
        DataSourceMode targetDataSourceMode = DataSourceMode.valOf(targetDataSourceModeValue);
        if (targetDataSourceMode == currentDataSourceMode) {
            log.info("DataSourceMode is not changed, skip migration!");
            return;
        }

        migrateDataSource(targetDataSourceMode);
    }

    private void migrateDataSource(DataSourceMode targetDataSourceMode) {
        DSLContextProvider targetDSLContextProvider = chooseDSLContextProviderByMode(targetDataSourceMode);
        log.info("Migrate datasource from {} to {} start...", currentDataSourceMode, targetDataSourceMode);
        boolean success = false;
        try {
            int expectedReadyServiceInstanceCount = Integer.parseInt(
                propToggleStore.getPropToggle(SERVICE_INSTANCE_COUNT).getDefaultValue());
            if (status == IDLE) {
                synchronized (this) {
                    if (status == IDLE) {
                        status = PREPARING;
                        // 为了在同一时间(接近）在多个微服务实例切换到新的数据源，需要判断处于 READY 状态的服务实例是否符合预期数量
                        redisTemplate.opsForSet().add(DB_MIGRATE_READY_SERVICE_INSTANCE_KEY,
                            IpUtils.getFirstMachineIP());
                        while (true) {
                            long readyServiceInstanceCount =
                                redisTemplate.opsForSet().size(DB_MIGRATE_READY_SERVICE_INSTANCE_KEY);
                            if (readyServiceInstanceCount >= expectedReadyServiceInstanceCount) {
                                // 所有服务实例都确认收到DB数据源迁移事件，准备启动迁移
                                status = MIGRATING;
                                // 等待 5s，等待当前DB数据源正在跑的读写请求都完成
                                log.info("Wait 5s before migration");
                                ThreadUtils.sleep(5000L);
                                break;
                            } else {
                                log.info("Wait all service instance ready, actual: {}, expected: {}",
                                    readyServiceInstanceCount, expectedReadyServiceInstanceCount);
                                ThreadUtils.sleep(100L);
                            }
                        }
                        this.currentDataSourceMode = targetDataSourceMode;
                        this.currentContextProvider = targetDSLContextProvider;
                        this.status = MIGRATED;
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
            redisTemplate.delete(DB_MIGRATE_READY_SERVICE_INSTANCE_KEY);
            status = IDLE;
            synchronized (lock) {
                lock.notifyAll();
            }
            log.info("Migrate datasource finish, isSuccess : {}", success);
        }
    }
}
