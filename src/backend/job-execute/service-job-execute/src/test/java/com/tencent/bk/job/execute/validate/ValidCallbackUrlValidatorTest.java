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

package com.tencent.bk.job.execute.validate;

import com.tencent.bk.job.execute.service.validation.CallbackUrlValidateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidCallbackUrlValidatorTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<CallbackUrlValidateService> provider(CallbackUrlValidateService service) {
        ObjectProvider<CallbackUrlValidateService> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(service);
        return p;
    }

    @Test
    @DisplayName("空值放行，不调用 Service")
    void blankBypassesService() {
        CallbackUrlValidateService service = mock(CallbackUrlValidateService.class);
        ValidCallbackUrlValidator validator = new ValidCallbackUrlValidator(provider(service));

        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
        verify(service, never()).isValid(anyString());
    }

    @Test
    @DisplayName("Service 不可用时 fail-open 放行（避免启动顺序导致请求中断）")
    void serviceMissingFailsOpen() {
        ValidCallbackUrlValidator validator = new ValidCallbackUrlValidator(provider(null));
        assertThat(validator.isValid("http://anyone.com/cb", null)).isTrue();
    }

    @Test
    @DisplayName("Service 可用时委托其结果")
    void delegatesToService() {
        CallbackUrlValidateService service = mock(CallbackUrlValidateService.class);
        when(service.isValid("http://good.com/cb")).thenReturn(true);
        when(service.isValid("http://bad.com/cb")).thenReturn(false);
        ValidCallbackUrlValidator validator = new ValidCallbackUrlValidator(provider(service));

        assertThat(validator.isValid("http://good.com/cb", null)).isTrue();
        assertThat(validator.isValid("http://bad.com/cb", null)).isFalse();
        verify(service, times(1)).isValid("http://good.com/cb");
        verify(service, times(1)).isValid("http://bad.com/cb");
    }
}
