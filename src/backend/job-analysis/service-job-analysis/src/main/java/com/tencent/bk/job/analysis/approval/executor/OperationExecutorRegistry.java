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

package com.tencent.bk.job.analysis.approval.executor;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 操作类型 → {@link OperationExecutor} 的注册表。
 * <p>
 * 启动时要求 6 种操作类型全部有实现：缺失意味着该类型的审批任务能建、放行时才发现执行不了 ——
 * 用户白批一次。这种错误必须在部署期暴露，不能等到放行时。
 */
@Slf4j
@Component
public class OperationExecutorRegistry {

    private final Map<ApprovalOperationTypeEnum, OperationExecutor<?>> executorMap =
        new EnumMap<>(ApprovalOperationTypeEnum.class);

    public OperationExecutorRegistry(List<OperationExecutor<?>> executors) {
        for (OperationExecutor<?> executor : executors) {
            OperationExecutor<?> previous = executorMap.put(executor.getOperationType(), executor);
            if (previous != null) {
                throw new IllegalStateException("Duplicated OperationExecutor for operationType "
                    + executor.getOperationType() + ": " + previous.getClass().getName()
                    + " and " + executor.getClass().getName());
            }
        }
        for (ApprovalOperationTypeEnum operationType : ApprovalOperationTypeEnum.values()) {
            if (!executorMap.containsKey(operationType)) {
                throw new IllegalStateException("Missing OperationExecutor for operationType " + operationType);
            }
        }
        log.info("Approval operation executors registered: {}", executorMap.keySet());
    }

    public OperationExecutor<?> getExecutor(ApprovalOperationTypeEnum operationType) {
        OperationExecutor<?> executor = executorMap.get(operationType);
        if (executor == null) {
            throw new IllegalStateException("No OperationExecutor for operationType " + operationType);
        }
        return executor;
    }
}
