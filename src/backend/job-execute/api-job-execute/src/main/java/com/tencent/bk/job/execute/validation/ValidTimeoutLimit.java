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

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.config.ResourceScopeTaskTimeoutParser;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ElementType.FIELD, ElementType.TYPE})
@Documented
@Constraint(validatedBy = ValidTimeoutLimit.TimeoutValidator.class)
@Retention(RUNTIME)
public @interface ValidTimeoutLimit {

    String message() default "{validation.constraints.InvalidJobTimeout_outOfRange.message}";

    int min() default JobConstants.MIN_JOB_TIMEOUT_SECONDS;

    int max() default JobConstants.MAX_JOB_TIMEOUT_SECONDS;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class TimeoutValidator implements ConstraintValidator<ValidTimeoutLimit, Integer> {

        private ResourceScopeTaskTimeoutParser resourceScopeTaskTimeoutParser;
        private ValidTimeoutLimit validTimeoutLimit;

        @Override
        public void initialize(ValidTimeoutLimit constraintAnnotation) {
            resourceScopeTaskTimeoutParser = ApplicationContextRegister.getBean(ResourceScopeTaskTimeoutParser.class);
            validTimeoutLimit = constraintAnnotation;
        }

        @Override
        public boolean isValid(Integer target, ConstraintValidatorContext context) {
            if (target == null) {
                return true;
            }

            // unwrap后可以添加messageParameter，给message渲染出实际timeout上限
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(
                HibernateConstraintValidatorContext.class
            );

            // 超时时间小于最小值
            if (target < validTimeoutLimit.min()) {
                hibernateContext.disableDefaultConstraintViolation();
                hibernateContext
                    .buildConstraintViolationWithTemplate(
                        "{validation.constraints.InvalidJobTimeout_smallerThanMin.message}")
                    .addConstraintViolation();
                return false;
            }

            AppResourceScope appResourceScope = JobContextUtil.getAppResourceScope();
            int maxTimeoutInConfiguration = resourceScopeTaskTimeoutParser.getMaxTimeoutOrDefault(
                appResourceScope.getAppId(),
                JobConstants.MAX_JOB_TIMEOUT_SECONDS);
            // 超时时间超过配置或默认的最大值
            if (target > maxTimeoutInConfiguration) {
                hibernateContext.disableDefaultConstraintViolation();
                hibernateContext.addMessageParameter("max", maxTimeoutInConfiguration);
                hibernateContext
                    .buildConstraintViolationWithTemplate(
                        "{validation.constraints.InvalidJobTimeout_outOfRange.message}")
                    .addConstraintViolation();
                return false;
            }

            return true;
        }

    }

}
