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

package com.tencent.bk.job.execute.validation;

import com.tencent.bk.job.common.constant.RollingExecutionModeEnum;
import com.tencent.bk.job.common.constant.RollingTypeEnum;
import com.tencent.bk.job.common.validation.ValidationGroups;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象的滚动配置联合校验分组提供者
 * <p>
 * 按 type × executionMode 联合分组：
 * - type=1(传输目标)：加入 {@link ValidationGroups.RollingType.TargetExecuteObject}
 * - type=2(源文件)：加入 {@link ValidationGroups.RollingType.FileSource}
 * - executionMode=1(串行,默认)：加入 {@link ValidationGroups.RollingExecutionMode.Serial}
 * - executionMode=2(并行)：加入 {@link ValidationGroups.RollingExecutionMode.Parallel}
 * <p>
 * 注意："type=2 + 并行" 与 "批次总数超上限" 属于跨字段/上下文约束，无法通过分组表达，需在服务层显式校验拒绝。
 */
@Slf4j
public abstract class AbstractRollingConfigGroupSequenceProvider<T> implements DefaultGroupSequenceProvider<T> {

    @Override
    public List<Class<?>> getValidationGroups(T rollingConfig) {
        List<Class<?>> validationGroups = new ArrayList<>();
        // 默认分组必须包含被校验的 Bean 类本身
        validationGroups.add(getBeanClass());
        if (rollingConfig == null) {
            return validationGroups;
        }
        Integer type = getRollingType(rollingConfig);
        if (RollingTypeEnum.TARGET_EXECUTE_OBJECT.getValue().equals(type)) {
            validationGroups.add(ValidationGroups.RollingType.TargetExecuteObject.class);
        } else if (RollingTypeEnum.FILE_SOURCE.getValue().equals(type)) {
            validationGroups.add(ValidationGroups.RollingType.FileSource.class);
        }

        Integer executionMode = getExecutionMode(rollingConfig);
        if (RollingExecutionModeEnum.isParallel(executionMode)) {
            validationGroups.add(ValidationGroups.RollingExecutionMode.Parallel.class);
        } else {
            // 缺省或串行
            validationGroups.add(ValidationGroups.RollingExecutionMode.Serial.class);
        }
        return validationGroups;
    }

    abstract Integer getRollingType(T t);

    abstract Integer getExecutionMode(T t);

    abstract Class<?> getBeanClass();
}
