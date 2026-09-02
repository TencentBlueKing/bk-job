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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 单元测试 - 加密类注册表。
 * <p>
 * 这里守的是"新增操作类型时不会静默漏加密"：漏登记只会让参数明文落库，运行期不会有任何报错，
 * 所以必须在启动期（构造注册表时）就炸掉。
 */
class ApprovalParamsCryptorRegistryTest {

    private final ApprovalParamsCryptorTestSupport support = new ApprovalParamsCryptorTestSupport();

    @Test
    @DisplayName("每种操作类型都能取到加密类，且请求体类型与执行器一致")
    void givenAllCryptorsThenEveryOperationTypeResolved() {
        ApprovalParamsCryptorRegistry registry = new ApprovalParamsCryptorRegistry(support.allCryptors());

        for (ApprovalOperationTypeEnum operationType : ApprovalOperationTypeEnum.values()) {
            ApprovalParamsCryptor<?> cryptor = registry.getCryptor(operationType);
            assertThat(cryptor.getOperationType()).isEqualTo(operationType);
            assertThat(cryptor.getParamsClass()).isNotNull();
        }
    }

    @Test
    @DisplayName("漏登记某个操作类型时启动即失败，不留下明文落库的可能")
    void givenMissingCryptorThenFailFast() {
        List<ApprovalParamsCryptor<?>> incomplete = new ArrayList<>(support.allCryptors());
        ApprovalParamsCryptor<?> removed = incomplete.remove(0);

        assertThatThrownBy(() -> new ApprovalParamsCryptorRegistry(incomplete))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(removed.getOperationType().name());
    }

    @Test
    @DisplayName("同一操作类型登记两个加密类时启动即失败，避免加解密用的不是同一个实现")
    void givenDuplicatedCryptorThenFailFast() {
        List<ApprovalParamsCryptor<?>> duplicated = new ArrayList<>(support.allCryptors());
        duplicated.add(duplicated.get(0));

        assertThatThrownBy(() -> new ApprovalParamsCryptorRegistry(duplicated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicated");
    }
}
