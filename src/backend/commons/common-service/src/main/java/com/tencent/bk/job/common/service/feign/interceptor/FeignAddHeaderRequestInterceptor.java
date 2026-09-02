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

package com.tencent.bk.job.common.service.feign.interceptor;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 服务间调用时补齐由网关注入、下游又依赖的公共请求头。
 * <p>
 * 这些请求头对下游而言与外部请求带来的没有区别：租户缺失会让下游把用户当作无租户处理，
 * 业务归属校验直接判定业务不存在。放在这里统一补，接口就不必各自声明。
 */
@Slf4j
public class FeignAddHeaderRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String lang = JobContextUtil.getUserLang();
        template.header(LocaleUtils.COMMON_LANG_HEADER, lang);
        addTenantIdIfAbsent(template);
    }

    /**
     * 已由接口显式声明并传值的不再补，否则 Feign 会把两个值一并发出去
     */
    private void addTenantIdIfAbsent(RequestTemplate template) {
        if (template.headers().containsKey(JobCommonHeaders.BK_TENANT_ID)) {
            return;
        }
        // 后台线程发起的调用没有租户上下文，此时留空由下游按各自规则处理
        String tenantId = JobContextUtil.findTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            template.header(JobCommonHeaders.BK_TENANT_ID, tenantId);
        }
    }
}
