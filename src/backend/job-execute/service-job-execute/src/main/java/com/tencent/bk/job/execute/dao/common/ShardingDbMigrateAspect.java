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

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.mysql.MySQLProperties;
import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.HorizontalShardingDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.MigrateDynamicDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import com.tencent.bk.job.common.sharding.ReadModeEnum;
import com.tencent.bk.job.common.sharding.WriteModeEnum;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.dao.sharding.ShardingMigrationRwModeMgr;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * Job 水平分库分表 db 无损迁移处理
 */
@Aspect
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ShardingDbMigrateAspect {

    /**
     * 需要被迁移的数据源 DSLContextProvider
     */
    private DSLContextProvider sourceDSLContextProvider;

    /**
     * 迁移目标数据源 DSLContextProvider
     */
    private final HorizontalShardingDSLContextProvider targetDSLContextProvider;

    private final MigrateDynamicDSLContextProvider migrateDynamicDSLContextProvider;

    private final ShardingMigrationRwModeMgr shardingMigrationRwModeMgr;

    public ShardingDbMigrateAspect(MigrateDynamicDSLContextProvider migrateDynamicDSLContextProvider,
                                   StandaloneDSLContextProvider standaloneDSLContextProvider,
                                   VerticalShardingDSLContextProvider verticalShardingDSLContextProvider,
                                   HorizontalShardingDSLContextProvider horizontalShardingDSLContextProvider,
                                   MySQLProperties mySQLProperties,
                                   ShardingMigrationRwModeMgr shardingMigrationRwModeMgr) {
        this.migrateDynamicDSLContextProvider = migrateDynamicDSLContextProvider;
        this.shardingMigrationRwModeMgr = shardingMigrationRwModeMgr;
        initSourceDSLContextProvider(mySQLProperties,
            standaloneDSLContextProvider, verticalShardingDSLContextProvider);
        this.targetDSLContextProvider = horizontalShardingDSLContextProvider;
    }

    private void initSourceDSLContextProvider(MySQLProperties mySQLProperties,
                                              StandaloneDSLContextProvider standaloneDSLContextProvider,
                                              VerticalShardingDSLContextProvider verticalShardingDSLContextProvider) {
        DataSourceMode dataSourceMode = DataSourceMode.valOf(mySQLProperties.getDataSourceMode());
        switch (dataSourceMode) {
            case STANDALONE:
                if (standaloneDSLContextProvider == null) {
                    log.error("StandaloneDSLContextProvider not found");
                    throw new IllegalStateException("StandaloneDSLContextProvider not found");
                }
                sourceDSLContextProvider = standaloneDSLContextProvider;
                log.info("Use StandaloneDSLContextProvider as migration source");
                break;
            case VERTICAL_SHARDING:
                if (verticalShardingDSLContextProvider == null) {
                    log.error("VerticalShardingDSLContextProvider not found");
                    throw new IllegalStateException("VerticalShardingDSLContextProvider not found");
                }
                sourceDSLContextProvider = verticalShardingDSLContextProvider;
                log.info("Use VerticalShardingDSLContextProvider as migration source");
                break;
            default:
                log.error("DataSource do not support migration, dataSourceMode: {}", dataSourceMode);
                throw new IllegalStateException("DataSource do not support migration");
        }
    }

    @Pointcut("@annotation(com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation)")
    public void shardingDbMigrate() {
    }

    @Around("shardingDbMigrate()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MySQLOperation mySQLOperation = method.getAnnotation(MySQLOperation.class);
        DbOperationEnum op = mySQLOperation.op();
        switch (op) {
            case READ:
                return readDB(pjp::proceed);
            case WRITE:
                return writeDB(mySQLOperation, pjp::proceed);
        }
        // 正常不应该跑到这里
        throw new IllegalStateException("Sharding migration aspect handle error");
    }

    @FunctionalInterface
    public interface Operation {
        Object execute() throws Throwable;
    }

    private Object readDB(Operation operation) throws Throwable {
        ReadModeEnum readMode = shardingMigrationRwModeMgr.evaluateReadMode();
        switch (readMode) {
            case READ_ORIGIN:
                // 读单库
                return readFromOriginDB(operation);
            case READ_SHARDING:
                // 读分库
                return readFromShardingDB(operation);
        }
        throw new IllegalStateException("Sharding migration aspect handle error, unexpected read mode");
    }

    private Object readFromOriginDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("ReadOriginDb");
            }
            migrateDynamicDSLContextProvider.setProvider(sourceDSLContextProvider);
            return operation.execute();
        } finally {
            migrateDynamicDSLContextProvider.unsetProvider();
        }
    }

    private Object readFromShardingDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("ReadShardingDb");
            }
            migrateDynamicDSLContextProvider.setProvider(targetDSLContextProvider);
            return operation.execute();
        } finally {
            migrateDynamicDSLContextProvider.unsetProvider();
        }
    }

    private Object writeDB(MySQLOperation mySQLOperation, Operation operation) throws Throwable {
        WriteModeEnum writeMode = shardingMigrationRwModeMgr.evaluateWriteMode();
        switch (writeMode) {
            case WRITE_ORIGIN:
                // 写单库
                return writeOriginDB(operation);
            case WRITE_SHARDING:
                // 写分库
                return writeShardingDB(operation);
            case WRITE_BOTH:
                // 双写
                return writeBothDB(mySQLOperation.table(), operation);
        }
        throw new IllegalStateException("Sharding migration aspect handle error, unexpected write mode");
    }

    private Object writeOriginDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("WriteOriginDb");
            }
            migrateDynamicDSLContextProvider.setProvider(sourceDSLContextProvider);
            return operation.execute();
        } finally {
            migrateDynamicDSLContextProvider.unsetProvider();
        }
    }

    private Object writeShardingDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("WriteShardingDb");
            }
            migrateDynamicDSLContextProvider.setProvider(targetDSLContextProvider);
            return operation.execute();
        } finally {
            migrateDynamicDSLContextProvider.unsetProvider();
        }
    }

    private Object writeBothDB(String tableName, Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("WriteBothOriginAndShardingDb");
            }
            Object result = null;
            if (!tableName.equals("task_instance_app")) {
                // task_instance_app 表为分库分表下独有的（使用 app_id 作为分片键)，是 task_instance 的数据冗余表;所以不需要写入源数据源
                migrateDynamicDSLContextProvider.setProvider(sourceDSLContextProvider);
                result = operation.execute();
            }
            try {
                // 双写；切换过程中写分库，如果遇到异常需要忽略，并记录异常信息
                migrateDynamicDSLContextProvider.setProvider(targetDSLContextProvider);
                operation.execute();
            } catch (Throwable e) {
                log.error("WriteShardingDBError", e);
            }
            return result;
        } finally {
            migrateDynamicDSLContextProvider.unsetProvider();
        }
    }
}
