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

package com.tencent.bk.job.config.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 白名单与配置归一化行为。核心是「白名单为空默认全拒」，不允许退化为默认放行。
 */
class WebAuthPropertiesTest {

    private WebAuthProperties newProperties(String whitelist, boolean allowWildcard) {
        WebAuthProperties properties = new WebAuthProperties();
        properties.getAuth().setWhitelist(whitelist);
        properties.getAuth().setAllowWildcard(allowWildcard);
        properties.init();
        return properties;
    }

    @Test
    @DisplayName("白名单为空时任何已登录用户都被拒绝")
    void shouldRejectAllWhenWhitelistIsEmpty() {
        WebAuthProperties properties = newProperties("", false);

        assertThat(properties.isUserAllowed("admin")).isFalse();
        assertThat(properties.isUserAllowed("anyone")).isFalse();
    }

    @Test
    @DisplayName("白名单只有分隔符与空白时同样全拒")
    void shouldRejectAllWhenWhitelistContainsOnlySeparators() {
        WebAuthProperties properties = newProperties(" , , ", false);

        assertThat(properties.getAuth().getNormalizedWhitelist()).isEmpty();
        assertThat(properties.isUserAllowed("admin")).isFalse();
    }

    @Test
    @DisplayName("白名单为空时即使开启通配开关也全拒")
    void shouldRejectAllWhenWhitelistIsEmptyEvenIfWildcardAllowed() {
        WebAuthProperties properties = newProperties("", true);

        assertThat(properties.isUserAllowed("admin")).isFalse();
    }

    @Test
    @DisplayName("用户名比对大小写不敏感且忽略两端空白")
    void shouldMatchWhitelistIgnoringCaseAndBlank() {
        WebAuthProperties properties = newProperties(" Admin , userB ", false);

        assertThat(properties.isUserAllowed("admin")).isTrue();
        assertThat(properties.isUserAllowed("ADMIN")).isTrue();
        assertThat(properties.isUserAllowed(" userb ")).isTrue();
        assertThat(properties.isUserAllowed("userC")).isFalse();
    }

    @Test
    @DisplayName("用户名为空时拒绝")
    void shouldRejectBlankUsername() {
        WebAuthProperties properties = newProperties("admin", false);

        assertThat(properties.isUserAllowed(null)).isFalse();
        assertThat(properties.isUserAllowed(" ")).isFalse();
    }

    @Test
    @DisplayName("通配符只有在显式开启allow-wildcard后才生效")
    void shouldHonourWildcardOnlyWhenAllowed() {
        assertThat(newProperties("*", false).isUserAllowed("anyone")).isFalse();
        assertThat(newProperties("*", true).isUserAllowed("anyone")).isTrue();
    }

    @Test
    @DisplayName("附加参数按k=v切分，格式错误时启动即失败")
    void shouldParseExtraParamsAndFailFastOnBadFormat() {
        WebAuthProperties properties = new WebAuthProperties();
        WebAuthProperties.Verify verify = properties.getAuth().getLogin().getVerify();
        verify.setExtraQueryParams("bk_app_code=bk-job-op, bk_app_secret=s3cret");
        verify.setExtraHeaders("X-Bk-Tenant-Id=default");
        properties.init();

        assertThat(verify.getNormalizedExtraQueryParams())
            .containsEntry("bk_app_code", "bk-job-op")
            .containsEntry("bk_app_secret", "s3cret");
        assertThat(verify.getNormalizedExtraHeaders()).containsEntry("X-Bk-Tenant-Id", "default");
        assertThat(verify.getNormalizedInvalidHttpStatuses()).containsExactlyInAnyOrder(400, 401, 403);

        WebAuthProperties badProperties = new WebAuthProperties();
        badProperties.getAuth().getLogin().getVerify().setExtraHeaders("no-equals-sign");
        assertThatThrownBy(badProperties::init).isInstanceOf(IllegalStateException.class);
    }
}
