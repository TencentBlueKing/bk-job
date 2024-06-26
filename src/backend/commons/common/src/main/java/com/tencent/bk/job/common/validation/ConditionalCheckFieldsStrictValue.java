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

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 校验属性值是否有效
 * primaryField校验通过，不校验dependentFields
 * primaryField校验不通过，校验dependentFields所有属性
 */
@Documented
@Constraint(validatedBy = ConditionalCheckFieldsStrictValue.Validator.class)
@Target({TYPE, FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalCheckFieldsStrictValue {
    String message() default "{primaryField}{validation.constraints.NotBlankField.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String primaryField();

    String[] dependentFields();

    boolean notNull() default false;

    class Validator implements ConstraintValidator<ConditionalCheckFieldsStrictValue, Object> {
        private ValidFieldsStrictValue.Validator leastOneFieldValidator = new ValidFieldsStrictValue.Validator();
        private String primaryField;
        private String[] dependentFields;
        private boolean notNull;

        @Override
        public void initialize(ConditionalCheckFieldsStrictValue constraintAnnotation) {
            this.primaryField = constraintAnnotation.primaryField();
            this.dependentFields = constraintAnnotation.dependentFields();
            this.notNull = constraintAnnotation.notNull();

            leastOneFieldValidator.initialize(new ValidFieldsStrictValue() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return ValidFieldsStrictValue.class;
                }

                @Override
                public String message() {
                    return Arrays.toString(constraintAnnotation.dependentFields())
                        + "{validation.constraints.NotBlankAtLeastOneField.message}";
                }

                @Override
                public Class<?>[] groups() {
                    return constraintAnnotation.groups();
                }

                @Override
                public Class<? extends Payload>[] payload() {
                    return constraintAnnotation.payload();
                }

                @Override
                public String[] fieldNames() {
                    return constraintAnnotation.dependentFields();
                }

                @Override
                public boolean notNull() {
                    return constraintAnnotation.notNull();
                }

                @Override
                public ConditionType condition() {
                    return ConditionType.AND;
                }
            });
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            try {
                if (value == null) {
                    return !notNull;
                }
                Field baseFieldObj = value.getClass().getDeclaredField(primaryField);
                baseFieldObj.setAccessible(true);
                Object baseFieldValue = baseFieldObj.get(value);
                if (ValidationUtil.isValuePositiveOrNonEmpty(baseFieldValue)) {
                    return true;
                }
                return leastOneFieldValidator.isValid(value, context);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
        }
    }
}
