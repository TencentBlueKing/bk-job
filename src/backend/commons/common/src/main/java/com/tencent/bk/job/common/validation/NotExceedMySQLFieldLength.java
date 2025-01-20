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


import com.tencent.bk.job.common.constant.MySQLDataType;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 通过在MySQL定义的字段类型判断是否过长
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Constraint(validatedBy = NotExceedMySQLFieldLength.FieldLengthValidator.class)
@Documented
@Retention(RUNTIME)
public @interface NotExceedMySQLFieldLength {

    String message() default "{fieldName} {validation.constraints.NotExceedMySQLFieldLength.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String fieldName();

    MySQLDataType fieldType();

    @Slf4j
    class FieldLengthValidator implements ConstraintValidator<NotExceedMySQLFieldLength, String> {

        MySQLDataType fieldType = null;

        @Override
        public void initialize(NotExceedMySQLFieldLength constraintAnnotation) {
            fieldType = constraintAnnotation.fieldType();
        }

        @Override
        public boolean isValid(String scriptContent, ConstraintValidatorContext constraintValidatorContext) {

            log.debug("[Validate MySQLFieldLength] field type: {}, content length: {}, maximum length: {}]",
                fieldType.getValue(), scriptContent.length(), fieldType.getMaximumLength());

            if (fieldType == null || scriptContent == null) {
                return true;
            }

            return scriptContent.length() <= fieldType.getMaximumLength();
        }
    }
}
