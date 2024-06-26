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

import org.springframework.beans.BeanWrapperImpl;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 校验Collection集合的大小，
 * ConditionType.OR: 属性列表只要有一个集合超过最大限制，则校验不通过
 * ConditionType.AND: 属性列表所有集合超过最大限制，则校验不通过
 */
@Target({TYPE, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCollectionSizeOutOfLimit.Validator.class)
public @interface ValidCollectionSizeOutOfLimit {
    String message() default "{fieldNames}{validation.constraints.InvalidCollectionSize_outOfLimit.message}{max}";

    int max() default ValidationConstants.MAX_BATCH_SEARCH_LOGS_HOSTS_LIMIT;

    String[] fieldNames();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean notNull() default false;

    ConditionType condition() default ConditionType.OR;

    class Validator implements ConstraintValidator<ValidCollectionSizeOutOfLimit, Object> {

        private int max;
        private String[] fieldNames;
        private boolean notNull;
        private ConditionType condition;

        @Override
        public void initialize(ValidCollectionSizeOutOfLimit constraintAnnotation) {
            this.max = constraintAnnotation.max();
            this.fieldNames = constraintAnnotation.fieldNames();
            this.notNull = constraintAnnotation.notNull();
            this.condition = constraintAnnotation.condition();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return !notNull;
            }
            boolean isValid = true;
            int count = 0;
            for (String fieldName : fieldNames) {
                Object fieldValue = new BeanWrapperImpl(value).getPropertyValue(fieldName);
                if (fieldValue instanceof Collection) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    if (collection.size() > max && condition == ConditionType.OR) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                            .addPropertyNode(fieldName)
                            .addConstraintViolation();
                        isValid = false;
                        break;
                    }
                    if (collection.size() > max && condition == ConditionType.AND) {
                        count++;
                    }
                }
            }
            if (count >= fieldNames.length) {
                isValid = false;
            }
            return isValid;
        }
    }
}
