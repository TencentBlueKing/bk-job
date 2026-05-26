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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ValidCallbackUrl} 注解对应的 Validator。
 * <p>
 * 通过 Spring 的 {@code SpringConstraintValidatorFactory}（{@code LocalValidatorFactoryBean} 默认启用）
 * 注入 {@link CallbackUrlValidateService}；用 {@link ObjectProvider} 软引用 Service Bean，
 * 在 Bean 尚未就绪或被关闭时 fail-open（放行），避免因 validator 启动顺序导致请求中断。
 */
@Slf4j
public class ValidCallbackUrlValidator implements ConstraintValidator<ValidCallbackUrl, String> {

    private final ObjectProvider<CallbackUrlValidateService> serviceProvider;

    @Autowired
    public ValidCallbackUrlValidator(ObjectProvider<CallbackUrlValidateService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            // 空值由 @NotBlank 等独立控制
            return true;
        }
        CallbackUrlValidateService service = serviceProvider.getIfAvailable();
        if (service == null) {
            log.warn("CallbackUrlValidateService not available, skip callback url validation, value={}", value);
            return true;
        }
        return service.isValid(value);
    }
}
