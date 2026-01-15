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

package com.tencent.bk.job.file_gateway.validate;

import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * spring validation java 文件源信息合法校验
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Constraint(validatedBy = ValidFileSourceInfo.Validator.class)
@Documented
@Retention(RUNTIME)
public @interface ValidFileSourceInfo {

    String message() default "{validation.constraints.BkRepoBaseUrlInvalid.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidFileSourceInfo, Map<String, Object>> {

        @Override
        public boolean isValid(Map<String, Object> value, ConstraintValidatorContext constraintValidatorContext) {
            if (value == null || value.isEmpty()) {
                return true;
            }
            Object baseUrlObj = value.get(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL);
            if (!(baseUrlObj instanceof String)) {
                return false;
            }
            String baseUrl = (String) baseUrlObj;
            if (StringUtils.isBlank(baseUrl)) {
                return false;
            }
            // 限制文件源根地址只能是 http(s)://xxx 形式
            return isHttpOrHttpsUrl(baseUrl);
        }

        private boolean isHttpOrHttpsUrl(String url) {
            return url.startsWith("http://") || url.startsWith("https://");
        }

    }
}
