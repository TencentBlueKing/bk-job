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

import com.tencent.bk.job.common.mysql.dynamic.ds.MigrateDynamicDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

@Aspect
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ReadWriteLockDbMigrateAspect {

    private final MigrateDynamicDSLContextProvider migrateDynamicDSLContextProvider;

    private final PropBasedDynamicDataSource propBasedDynamicDataSource;


    public ReadWriteLockDbMigrateAspect(MigrateDynamicDSLContextProvider migrateDynamicDSLContextProvider,
                                        PropBasedDynamicDataSource propBasedDynamicDataSource) {
        this.migrateDynamicDSLContextProvider = migrateDynamicDSLContextProvider;
        this.propBasedDynamicDataSource = propBasedDynamicDataSource;
    }

    @Pointcut("@annotation(com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation)")
    public void mysqlOperation() {
    }

    @Around("mysqlOperation()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            MySQLOperation operation = method.getAnnotation(MySQLOperation.class);
            migrateDynamicDSLContextProvider.setProvider(propBasedDynamicDataSource.getCurrent(operation));
            return pjp.proceed();
        } finally {
            migrateDynamicDSLContextProvider.setProvider(null);
        }
    }
}
