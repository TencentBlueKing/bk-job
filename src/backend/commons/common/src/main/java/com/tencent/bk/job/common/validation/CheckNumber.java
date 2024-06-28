/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * spring validation数值校验注解，非空、最大值、最小值校验
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Constraint(validatedBy = CheckNumber.Validator.class)
@Documented
@Retention(RUNTIME)
public @interface CheckNumber {
    String message() default "{fieldName}{validation.constraints.InvalidNumber.message}";

    boolean notNull() default false;

    String min() default "";

    String max() default "";

    Class<?>[] groups() default {};

    String fieldName() default "";

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class Validator implements ConstraintValidator<CheckNumber, Number> {
        private BigDecimal min;
        private BigDecimal max;
        private boolean notNull;
        private boolean hasMin;
        private boolean hasMax;

        @Override
        public void initialize(CheckNumber constraintAnnotation) {
            this.notNull = constraintAnnotation.notNull();
            this.hasMin = !constraintAnnotation.min().isEmpty();
            this.hasMax = !constraintAnnotation.max().isEmpty();
            if (hasMin) {
                this.min = new BigDecimal(constraintAnnotation.min());
            }
            if (hasMax) {
                this.max = new BigDecimal(constraintAnnotation.max());
            }
        }

        @Override
        public boolean isValid(Number value, ConstraintValidatorContext context) {
            if (value == null) {
                return !notNull;
            }
            BigDecimal decimalValue = new BigDecimal(value.toString());
            if (hasMin && decimalValue.compareTo(min) < 0) {
                return false;
            }
            if (hasMax && decimalValue.compareTo(max) > 0) {
                return false;
            }
            return true;
        }
    }
}
