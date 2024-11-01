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
import com.tencent.bk.job.common.sharding.ReadModeEnum;
import com.tencent.bk.job.common.sharding.WriteModeEnum;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategy;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.jooq.DSLContext;

import java.lang.reflect.Method;

@Aspect
@Slf4j
public class ReadWriteLockDbMigrateAspect {

    private final DynamicDSLContextProvider dynamicDSLContextProvider;

    private final DSLContext sourceDSLContext;
    private final DSLContext targetDSLContext;

    private final PropToggleStore propToggleStore;

    private static final String PROP_NAME_MIGRATE_ENABLE = "job_execute_mysql_migrate_trigger";

    private final Object lock = new Object();

    private ReadWriteLockDbMigrateMgr dbMigrationStatusMgr;


    public ReadWriteLockDbMigrateAspect(DynamicDSLContextProvider dynamicDSLContextProvider,
                                        DSLContext sourceDSLContext,
                                        DSLContext targetDSLContext,
                                        PropToggleStore propToggleStore) {
        this.dynamicDSLContextProvider = dynamicDSLContextProvider;
        this.sourceDSLContext = sourceDSLContext;
        this.targetDSLContext = targetDSLContext;
        this.propToggleStore = propToggleStore;
    }

    @Pointcut("@annotation(com.tencent.bk.job.execute.dao.common.DbMigrate)")
    public void dbMigrate() {
    }

    @Around("dbMigrate()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        DbMigrate dbMigrate = method.getAnnotation(DbMigrate.class);
        try {
            dynamicDSLContextProvider.set(dbMigrationStatusMgr.getCurrent());
            return pjp.proceed();
        } finally {
            dynamicDSLContextProvider.unset();
        }

        MigrationStatus status = dbMigrationStatusMgr.getStatus();
        if (status == MigrationStatus.NOT_START || status == MigrationStatus.WAIT) {
            // 只写单库
            try {
                dynamicDSLContextProvider.set(sourceDSLContext);
                return pjp.proceed();
            } finally {
                dynamicDSLContextProvider.unset();
            }
        } else if (status == MigrationStatus.READY || status == MigrationStatus.MIGRATING) {
            lock.wait();
            try {
                dynamicDSLContextProvider.set(targetDSLContext);
                return pjp.proceed();
            } finally {
                dynamicDSLContextProvider.unset();
            }
        } else if (status == MigrationStatus.MIGRATED) {
            try {
                dynamicDSLContextProvider.set(targetDSLContext);
                return pjp.proceed();
            } finally {
                dynamicDSLContextProvider.unset();
            }
        }


        DbOperationEnum op = dbMigrate.op();
        switch (op) {
            case READ:
                return readDB(resourceScope, pjp::proceed);
            case WRITE:
                return writeDB(resourceScope, pjp::proceed);
        }
        // 正常不应该跑到这里
        throw new IllegalStateException("Sharding migration aspect handle error");
    }

    @FunctionalInterface
    public interface Operation {
        Object execute() throws Throwable;
    }

    private Object readDB(ResourceScope resourceScope, Operation operation) throws Throwable {
        String propValue = evaluatePropValue(PROP_NAME_READ_MODE, resourceScope);
        ReadModeEnum readMode = ReadModeEnum.valOf(Integer.parseInt(propValue));
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
            dslContextDynamicProvider.set(noShardingDSLContext);
            return operation.execute();
        } finally {
            dslContextDynamicProvider.unset();
        }
    }

    private Object readFromShardingDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("ReadShardingDb");
            }
            dslContextDynamicProvider.set(shardingDSLContext);
            return operation.execute();
        } finally {
            dslContextDynamicProvider.unset();
        }
    }

    private Object writeDB(ResourceScope resourceScope, Operation operation) throws Throwable {
        String propValue = evaluatePropValue(PROP_NAME_WRITE_MODE, resourceScope);
        WriteModeEnum writeMode = WriteModeEnum.valOf(Integer.parseInt(propValue));
        switch (writeMode) {
            case WRITE_ORIGIN:
                // 写单库
                return writeOriginDB(operation);
            case WRITE_SHARDING:
                // 写分库
                return writeShardingDB(operation);
            case WRITE_BOTH:
                // 双写
                return writeBothDB(operation);
        }
        throw new IllegalStateException("Sharding migration aspect handle error, unexpected write mode");
    }

    private Object writeOriginDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("WriteOriginDb");
            }
            dslContextDynamicProvider.set(noShardingDSLContext);
            return operation.execute();
        } finally {
            dslContextDynamicProvider.unset();
        }
    }

    private Object writeShardingDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("WriteShardingDb");
            }
            dslContextDynamicProvider.set(shardingDSLContext);
            return operation.execute();
        } finally {
            dslContextDynamicProvider.unset();
        }
    }

    private Object writeBothDB(Operation operation) throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("WriteBothOriginAndShardingDb");
            }
            dslContextDynamicProvider.set(noShardingDSLContext);
            Object result = operation.execute();
            try {
                // 双写；切换过程中写分库，如果遇到异常需要忽略，并记录异常信息
                dslContextDynamicProvider.set(shardingDSLContext);
                operation.execute();
            } catch (Throwable e) {
                log.error("WriteShardingDBError", e);
            }
            return result;
        } finally {
            dslContextDynamicProvider.unset();
        }
    }

    private String evaluatePropValue(String propName, ResourceScope resourceScope) {
        String propValue = null;
        PropToggle propToggle = propToggleStore.getPropToggle(propName);
        if (propToggle == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(propToggle.getConditions())) {
            return propToggle.getDefaultValue();
        }

        ToggleEvaluateContext ctx = ToggleEvaluateContext.builder()
            .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, resourceScope);
        for (PropToggle.PropValueCondition condition : propToggle.getConditions()) {
            ToggleStrategy toggleStrategy = condition.getStrategy();
            if (toggleStrategy == null) {
                propValue = condition.getValue();
                break;
            } else {
                if (toggleStrategy.evaluate(propName, ctx)) {
                    propValue = condition.getValue();
                    break;
                }
            }
        }
        if (propValue == null) {
            propValue = propToggle.getDefaultValue();
        }
        return propValue;
    }


}
