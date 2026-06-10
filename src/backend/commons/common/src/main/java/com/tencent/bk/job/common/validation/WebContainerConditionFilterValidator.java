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

package com.tencent.bk.job.common.validation;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 动态条件过滤器 Web 入参的程序式校验：在 WebResource 入口、转换为内部 KubeContainerFilter 之前调用。
 * <p>
 * 跨字段 / 依赖 OperatorDispatcher 的校验放在此处：
 * <ul>
 *   <li>clusterList 必填（双重兜底：Bean Validation 失败时仍能在程序式调用路径上拦住）</li>
 *   <li>propConditions 逐条委托 {@link KubePropConditionValidator}（字段白名单 / 运算符派发 / value 形态）</li>
 * </ul>
 * 拓扑对象（cluster/namespace/workload）的 id/name 字段级非空已由 {@code WebKubeXxxObject} 上的
 * Bean Validation 注解兜底；namespace / workload 是否必填由产品决定（当前 namespace 与 workload 都可选）。
 */
public final class WebContainerConditionFilterValidator {

    private WebContainerConditionFilterValidator() {
    }

    public static void validate(List<WebContainerConditionFilter> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return;
        }
        for (WebContainerConditionFilter filter : filters) {
            validate(filter);
        }
    }

    public static void validate(WebContainerConditionFilter filter) {
        if (filter == null) {
            throwInvalid("container condition filter can not be null");
        }
        if (CollectionUtils.isEmpty(filter.getClusterList())) {
            throwInvalid("clusterList is required");
        }
        KubePropConditionValidator.validate(filter.getPropConditions());
    }

    private static void throwInvalid(String reason) {
        throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON, new Object[]{reason});
    }
}
