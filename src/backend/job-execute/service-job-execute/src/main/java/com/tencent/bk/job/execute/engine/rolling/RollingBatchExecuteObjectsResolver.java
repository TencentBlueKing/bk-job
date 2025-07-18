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

package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 执行对象滚动分批Resolver
 */
@Slf4j
public class RollingBatchExecuteObjectsResolver {

    /**
     * 允许的最大滚动批次
     */
    public static final int MAX_ALLOWED_ROLLING_BATCH_SIZE = 500;

    /**
     * 滚动策略表达式
     */
    private final String rollingExpr;

    /**
     * 滚动表达式解析上下文
     */
    private final RollingExecuteObjectBatchContext context;

    /**
     * Constructor
     *
     * @param executeObjects 滚动执行的执行对象
     * @param rollingExpr    滚动表达式
     */
    public RollingBatchExecuteObjectsResolver(List<ExecuteObject> executeObjects, String rollingExpr) {
        this.context = new RollingExecuteObjectBatchContext(executeObjects);
        this.rollingExpr = rollingExpr;
    }

    /**
     * 解析滚动表达式，滚动分批
     *
     * @return 服务器分批情况
     */
    public List<RollingExecuteObjectBatch> resolve() {
        RollingExpr rollingExpr = new RollingExpr(this.rollingExpr);
        while (context.hasRemainedExecuteObject()) {
            if (context.getExecuteObjectBatches().size() > MAX_ALLOWED_ROLLING_BATCH_SIZE) {
                log.warn("Batch {} size greater than {}", context.getExecuteObjectBatches().size(),
                    MAX_ALLOWED_ROLLING_BATCH_SIZE);
                throw new FailedPreconditionException(ErrorCode.EXCEED_MAX_ALLOWED_BATCH_SIZE,
                    new Integer[]{MAX_ALLOWED_ROLLING_BATCH_SIZE});
            }

            context.increaseBatchCount();

            RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(context.getBatchCount());
            List<ExecuteObject> executeObjectsOnBatch = rollingExprPart.compute(context);
            context.removeResolvedServers(executeObjectsOnBatch);
            RollingExecuteObjectBatch rollingExecuteObjectBatch = new RollingExecuteObjectBatch();
            rollingExecuteObjectBatch.setBatch(context.getBatchCount());
            rollingExecuteObjectBatch. setExecuteObjects(executeObjectsOnBatch);
            rollingExecuteObjectBatch.setRollingExprPart(rollingExprPart);
            context.addExecuteObjectBatch(rollingExecuteObjectBatch);
        }

        return context.getExecuteObjectBatches();
    }

}
