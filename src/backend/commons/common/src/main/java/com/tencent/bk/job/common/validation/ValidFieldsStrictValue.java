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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 校验属性列表的值是否有效
 * ConditionType.OR: 有一个属性满足条件，则校验通过
 * ConditionType.AND：所有属性都满足条件，则校验通过
 */
@Documented
@Constraint(validatedBy = ValidFieldsStrictValue.Validator.class)
@Target({TYPE, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidFieldsStrictValueContainer.class)
public @interface ValidFieldsStrictValue {
    String message() default "{fieldNames}{validation.constraints.NotBlankAtLeastOneField.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] fieldNames();
    boolean notNull() default false;
    ConditionType condition() default ConditionType.OR;

    @Slf4j
    class Validator implements ConstraintValidator<ValidFieldsStrictValue, Object> {
        private String[] fieldNames;
        private boolean notNull;
        private ConditionType condition;

        @Override
        public void initialize(ValidFieldsStrictValue constraintAnnotation) {
            this.fieldNames = constraintAnnotation.fieldNames();
            notNull = constraintAnnotation.notNull();
            condition = constraintAnnotation.condition();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isValid = false;
            if (value == null) {
                return !notNull;
            }
            for (String fieldName : fieldNames) {
                try {
                    Field field = value.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    isValid = ValidationUtil.isValuePositiveOrNonEmpty(fieldValue);
                    if ((isValid && condition == ConditionType.OR)
                        || (!isValid && condition == ConditionType.AND)) {
                        break;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error("valid fields strict value exception={}, fieldNames={}, condition={}",
                        e.getMessage(), fieldNames, condition);
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
            }
            return isValid;
        }
    }
}
