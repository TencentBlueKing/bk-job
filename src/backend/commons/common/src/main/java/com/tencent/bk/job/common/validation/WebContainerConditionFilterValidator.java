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
import com.tencent.bk.job.common.constant.KubeTopoNodeTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import com.tencent.bk.job.common.model.vo.WebKubeNamespaceObject;
import com.tencent.bk.job.common.model.vo.WebKubeTopo;
import com.tencent.bk.job.common.model.vo.WebKubeWorkloadObject;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.XssChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 动态条件过滤器 Web 入参的程序式校验：在 WebResource 入口、转换为内部 KubeContainerFilter 之前调用。
 * <p>
 * 统一在此处完成 kubeTopoList 与 propConditions 的全部校验（不再依赖字段级 Bean Validation 注解，列表非空
 * 由 DTO 上的 {@code @NotEmpty} 额外兜底）：
 * <ul>
 *   <li>name（用户自定义过滤条件名）：可不传；传了才校验长度 &le; 60、不含 XSS 字符（{@code < > " '}）</li>
 *   <li>kubeTopoList 必填非空</li>
 *   <li>每条 topo：cluster 必填且 id 非空；namespace 若填则 id 非空；workloads 若非空则必须先选 namespace，
 *       且每个 workload 的 kind 非空且为合法 workload 类型（见 {@link KubeTopoNodeTypeEnum}）、id 非空</li>
 *   <li>propConditions 逐条委托 {@link KubePropConditionValidator}（字段白名单 / 运算符派发 / value 形态）</li>
 * </ul>
 * 校验失败抛 {@link InvalidParamException}，由全局异常处理转为标准错误响应。
 */
public final class WebContainerConditionFilterValidator {

    /**
     * 自定义过滤条件名的最大长度，与脚本名/执行方案名等资源名保持一致
     */
    private static final int NAME_MAX_LENGTH = 60;

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
        validateName(filter.getName());
        if (CollectionUtils.isEmpty(filter.getKubeTopoList())) {
            throwInvalid("kubeTopoList is required");
        }
        for (WebKubeTopo topo : filter.getKubeTopoList()) {
            validateKubeTopo(topo);
        }
        KubePropConditionValidator.validate(filter.getPropConditions());
    }

    /**
     * 校验用户自定义过滤条件名：name 可不传，传了才校验长度不超限、不含 XSS 字符（{@code < > " '}）。
     */
    private static void validateName(String name) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throwInvalid("container condition filter name length can not exceed " + NAME_MAX_LENGTH);
        }
        try {
            new StringCheckHelper(new XssChecker()).checkAndGetResult(name);
        } catch (Exception e) {
            throwInvalid("container condition filter name contains illegal char");
        }
    }

    private static void validateKubeTopo(WebKubeTopo topo) {
        if (topo == null || topo.getCluster() == null) {
            throwInvalid("cluster is required in each kube topo");
        }
        if (topo.getCluster().getId() == null) {
            throwInvalid("cluster.id can not be null in kube topo");
        }
        WebKubeNamespaceObject namespace = topo.getNamespace();
        if (namespace != null && namespace.getId() == null) {
            throwInvalid("namespace.id can not be null when namespace is specified");
        }
        List<WebKubeWorkloadObject> workloads = topo.getWorkloads();
        if (CollectionUtils.isNotEmpty(workloads)) {
            if (namespace == null) {
                throwInvalid("namespace is required when workloads are specified");
            }
            for (WebKubeWorkloadObject workload : workloads) {
                validateWorkload(workload);
            }
        }
    }

    private static void validateWorkload(WebKubeWorkloadObject workload) {
        if (workload == null) {
            throwInvalid("workload can not be null");
        }
        if (StringUtils.isBlank(workload.getKind())) {
            throwInvalid("workload.kind can not be blank");
        }
        if (!KubeTopoNodeTypeEnum.isValidWorkloadKind(workload.getKind())) {
            throwInvalid("unsupported workload.kind [" + workload.getKind() + "]");
        }
        if (workload.getId() == null) {
            throwInvalid("workload.id can not be null");
        }
    }

    private static void throwInvalid(String reason) {
        throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON, new Object[]{reason});
    }
}
