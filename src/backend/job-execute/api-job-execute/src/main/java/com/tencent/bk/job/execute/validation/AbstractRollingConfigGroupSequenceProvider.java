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

import com.tencent.bk.job.common.constant.RollingTypeEnum;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v3.EsbRollingConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象的滚动配置联合校验分组提供者
 */
@Slf4j
public abstract class AbstractRollingConfigGroupSequenceProvider<T> implements DefaultGroupSequenceProvider<T> {

    @Override
    public List<Class<?>> getValidationGroups(T rollingConfig) {
        List<Class<?>> validationGroups = new ArrayList<>();
        validationGroups.add(EsbRollingConfigDTO.class);
        if (rollingConfig == null) {
            return validationGroups;
        }
        Integer type = getRollingType(rollingConfig);
        if (RollingTypeEnum.TARGET_EXECUTE_OBJECT.getValue().equals(type)) {
            validationGroups.add(ValidationGroups.RollingType.TargetExecuteObject.class);
        } else if (RollingTypeEnum.FILE_SOURCE.getValue().equals(type)) {
            validationGroups.add(ValidationGroups.RollingType.FileSource.class);
        }
        return validationGroups;
    }

    abstract Integer getRollingType(T t);
}
