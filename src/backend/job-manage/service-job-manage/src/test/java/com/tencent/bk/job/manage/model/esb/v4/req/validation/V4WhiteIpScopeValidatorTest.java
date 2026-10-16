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

package com.tencent.bk.job.manage.model.esb.v4.req.validation;

import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateWhiteIpRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4WhiteIpScopeReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintValidatorContext;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class V4WhiteIpScopeValidatorTest {

    private V4WhiteIpScopeValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new V4WhiteIpScopeValidator();
        context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    @DisplayName("未选择全部业务且没有业务列表时校验失败")
    void rejectEmptyScopeWhenNotAllScope() {
        V4CreateWhiteIpRequest request = new V4CreateWhiteIpRequest();
        request.setAllScope(false);
        request.setScopeList(null);
        assertThat(validator.isValid(request, context)).isFalse();

        request.setScopeList(Collections.emptyList());
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    @DisplayName("全部业务或已提供业务列表时校验通过")
    void acceptAllScopeOrScopeList() {
        V4CreateWhiteIpRequest allScope = new V4CreateWhiteIpRequest();
        allScope.setAllScope(true);
        assertThat(validator.isValid(allScope, context)).isTrue();

        V4CreateWhiteIpRequest scoped = new V4CreateWhiteIpRequest();
        scoped.setAllScope(false);
        V4WhiteIpScopeReq scope = new V4WhiteIpScopeReq();
        scope.setScopeType("biz");
        scope.setScopeId("2");
        scoped.setScopeList(Collections.singletonList(scope));
        assertThat(validator.isValid(scoped, context)).isTrue();
    }
}
