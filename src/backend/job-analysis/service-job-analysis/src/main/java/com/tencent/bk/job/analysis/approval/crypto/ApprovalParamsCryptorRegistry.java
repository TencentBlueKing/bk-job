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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 操作类型 → {@link ApprovalParamsCryptor} 的注册表。
 * <p>
 * 启动时要求 6 种操作类型全部有实现：缺失意味着该类型的参数会明文落库，而且不会有任何报错。
 * 没有敏感字段的操作类型也必须显式登记一个空实现。
 */
@Slf4j
@Component
public class ApprovalParamsCryptorRegistry {

    private final Map<ApprovalOperationTypeEnum, ApprovalParamsCryptor<?>> cryptorMap =
        new EnumMap<>(ApprovalOperationTypeEnum.class);

    public ApprovalParamsCryptorRegistry(List<ApprovalParamsCryptor<?>> cryptors) {
        for (ApprovalParamsCryptor<?> cryptor : cryptors) {
            ApprovalParamsCryptor<?> previous = cryptorMap.put(cryptor.getOperationType(), cryptor);
            if (previous != null) {
                throw new IllegalStateException("Duplicated ApprovalParamsCryptor for operationType "
                    + cryptor.getOperationType() + ": " + previous.getClass().getName()
                    + " and " + cryptor.getClass().getName());
            }
        }
        for (ApprovalOperationTypeEnum operationType : ApprovalOperationTypeEnum.values()) {
            if (!cryptorMap.containsKey(operationType)) {
                throw new IllegalStateException("Missing ApprovalParamsCryptor for operationType " + operationType);
            }
        }
        log.info("Approval params cryptors registered: {}", cryptorMap.keySet());
    }

    public ApprovalParamsCryptor<?> getCryptor(ApprovalOperationTypeEnum operationType) {
        ApprovalParamsCryptor<?> cryptor = cryptorMap.get(operationType);
        if (cryptor == null) {
            throw new IllegalStateException("No ApprovalParamsCryptor for operationType " + operationType);
        }
        return cryptor;
    }
}
