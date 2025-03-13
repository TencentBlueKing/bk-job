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


import com.tencent.bk.job.common.constant.MySQLTextDataType;
import com.tencent.bk.job.common.util.Base64Util;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 通过在MySQL定义的TEXT一族字段类型（TINYTEXT/TEXT/MEDIUMTEXT/LONGTEXT）判断是否过长
 * 因为在MySQL中，TEXT类型的存储是以字节流的长度为限制的，而char/varchar则是以字符串的长度为限制的，所以校验长度合法的逻辑是不太一样的
 * 因此这个校验器只校验TEXT类型的字节流长度，若需要校验char/varchar类型的字段长度，请用str.length()校验
 *
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Constraint(validatedBy = NotExceedMySQLTextFieldLength.FieldLengthValidator.class)
@Documented
@Retention(RUNTIME)
public @interface NotExceedMySQLTextFieldLength {

    String message() default "{fieldName} {validation.constraints.NotExceedMySQLFieldLength.message} limit:{fieldType}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String fieldName();

    MySQLTextDataType fieldType();

    boolean base64();

    @Slf4j
    class FieldLengthValidator implements ConstraintValidator<NotExceedMySQLTextFieldLength, String> {

        MySQLTextDataType fieldType = null;
        boolean useBase64 = false;

        @Override
        public void initialize(NotExceedMySQLTextFieldLength constraintAnnotation) {
            fieldType = constraintAnnotation.fieldType();
            useBase64 = constraintAnnotation.base64();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
            if (value == null || fieldType == null) {
                return true;
            }

            int currentLength;
            if (useBase64) {
                currentLength = Base64Util.calcOriginBytesLength(value);
            } else {
                currentLength = value.getBytes(StandardCharsets.UTF_8).length;
            }

            log.debug("[Validate MySQLFieldLength] field type: {}, current length: {}, maximum length: {}]",
                fieldType.getValue(), currentLength, fieldType.getMaximumLength());

            return currentLength <= fieldType.getMaximumLength();
        }
    }
}
