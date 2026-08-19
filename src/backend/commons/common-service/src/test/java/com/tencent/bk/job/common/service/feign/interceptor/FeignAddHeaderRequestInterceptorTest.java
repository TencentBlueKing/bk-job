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
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务间调用的公共请求头补齐。
 * <p>
 * 下游服务分不清请求来自网关还是兄弟服务，一律从请求头取租户；漏传会让下游把调用者当作无租户用户，
 * 业务归属校验随即判定业务不存在，且报错位置离真正的丢失点很远。
 */
class FeignAddHeaderRequestInterceptorTest {

    private static final String TENANT_ID = "tenant_a";

    private final FeignAddHeaderRequestInterceptor interceptor = new FeignAddHeaderRequestInterceptor();

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("上下文中有租户时补进请求头")
    void givenTenantInContextThenAddHeader() {
        JobContextUtil.setUser(new User(TENANT_ID, "admin", "admin"));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get(JobCommonHeaders.BK_TENANT_ID)).containsExactly(TENANT_ID);
    }

    @Test
    @DisplayName("接口已显式声明租户请求头时不再补，避免一个头带两个值")
    void givenTenantHeaderAlreadySetThenKeepIt() {
        JobContextUtil.setUser(new User(TENANT_ID, "admin", "admin"));
        RequestTemplate template = new RequestTemplate();
        template.header(JobCommonHeaders.BK_TENANT_ID, "tenant_from_param");

        interceptor.apply(template);

        assertThat(template.headers().get(JobCommonHeaders.BK_TENANT_ID))
            .containsExactly("tenant_from_param");
    }

    @Test
    @DisplayName("已声明的请求头大小写不同也认为已设置")
    void givenTenantHeaderInOtherCaseThenKeepIt() {
        JobContextUtil.setUser(new User(TENANT_ID, "admin", "admin"));
        RequestTemplate template = new RequestTemplate();
        template.header("x-bk-tenant-id", "tenant_from_param");

        interceptor.apply(template);

        assertThat(template.headers().get(JobCommonHeaders.BK_TENANT_ID))
            .containsExactly("tenant_from_param");
    }

    @Test
    @DisplayName("后台线程没有租户上下文时不补空值")
    void givenNoContextThenNoTenantHeader() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(JobCommonHeaders.BK_TENANT_ID);
    }

    @Test
    @DisplayName("上下文中租户为空时不补空值")
    void givenBlankTenantThenNoTenantHeader() {
        JobContextUtil.setUser(new User(null, "admin", "admin"));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(JobCommonHeaders.BK_TENANT_ID);
    }

    @Test
    @DisplayName("补租户不影响原有的语言请求头")
    void givenTenantAddedThenLangHeaderKept() {
        JobContextUtil.setUser(new User(TENANT_ID, "admin", "admin"));
        JobContextUtil.setUserLang("en");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().keySet())
            .containsAll(Collections.singletonList(JobCommonHeaders.BK_TENANT_ID));
        assertThat(template.headers()).hasSize(2);
    }
}
