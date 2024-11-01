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

import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.toggle.prop.PropChangeEventListener;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.bk.job.execute.dao.common.MigrationStatus.NOT_START;
import static com.tencent.bk.job.execute.dao.common.MigrationStatus.READY;
import static com.tencent.bk.job.execute.dao.common.MigrationStatus.WAIT;

@Component
@Slf4j
public class ReadWriteLockDbMigrateMgr implements PropChangeEventListener {
    private volatile MigrationStatus status;

    private final Object lock = new Object();

    private final RedisTemplate<String, Object> redisTemplate;

    private String DB_MIGRATE_READY_SERVICE_INSTANCE_KEY = "job:execute:db:migrate:ready:service:instance";

    private Map<String, DSLContext> dslContextMap = new HashMap<>();

    private volatile String currentDataSourceName = null;

    private DSLContext current;

    private final PropToggleStore propToggleStore;

    private static final String PROP_NAME = "db_migration_job_execute_primary_db";
    private static final String SERVICE_INSTANCE_COUNT = "db_migration_job_execute_service_instance_count";

    @Autowired
    public ReadWriteLockDbMigrateMgr(
        @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore) {
        this.redisTemplate = redisTemplate;
        this.propToggleStore = propToggleStore;
        propToggleStore.addPropChangeEventListener(PROP_NAME, this);
    }

    public void onDone() {

    }

    public DSLContext getCurrent() {
        if (status == MigrationStatus.NOT_START || status == MigrationStatus.WAIT || status == MigrationStatus.MIGRATED) {
            return current;
        } else if (status == MigrationStatus.READY || status == MigrationStatus.MIGRATING) {
            try {
                lock.wait();
            } catch (InterruptedException e) {

            }
        }
        return current;
    }

    @Override
    public void handlePropChangeEvent(String propName, PropToggle currentValue) {
        String targetDataSourceName = currentValue.getDefaultValue();
        if (targetDataSourceName.equals(currentDataSourceName)) {
            log.info("DataSource is not changed, skip migration!");
            return;
        }
        migrateDataSource();
    }

    private void migrateDataSource() {
        try {
            int serviceInstanceCount = Integer.parseInt(
                propToggleStore.getPropToggle(SERVICE_INSTANCE_COUNT).getDefaultValue());
            if (status == NOT_START) {
                synchronized (lock) {
                    if (status == NOT_START) {
                        redisTemplate.opsForSet().add(DB_MIGRATE_READY_SERVICE_INSTANCE_KEY, IpUtils.getFirstMachineIP());
                        status = WAIT;
                        while (true) {
                            long count = redisTemplate.opsForSet().size(DB_MIGRATE_READY_SERVICE_INSTANCE_KEY);
                            if (count >= serviceInstanceCount) {
                                status = READY;
                            } else {
                                ThreadUtils.sleep(100L);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Migrate datasource error", e);
        } finally {
            redisTemplate.delete(DB_MIGRATE_READY_SERVICE_INSTANCE_KEY);
        }
    }
}
