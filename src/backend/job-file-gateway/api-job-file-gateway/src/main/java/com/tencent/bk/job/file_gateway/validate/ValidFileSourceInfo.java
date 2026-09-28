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
import com.tencent.bk.job.file_gateway.consts.FileSourceTypeEnum;
import com.tencent.bk.job.file_gateway.model.req.esb.v3.EsbCreateOrUpdateFileSourceV3Req;
import com.tencent.bk.job.file_gateway.model.req.web.FileSourceCreateUpdateReq;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 文件源接入参数格式校验：仅对蓝鲸制品库类型检查 base_url 必须为 http(s)
 */
@Target({TYPE, ANNOTATION_TYPE})
@Constraint(validatedBy = ValidFileSourceInfo.Validator.class)
@Documented
@Retention(RUNTIME)
public @interface ValidFileSourceInfo {

    String message() default "{validation.constraints.BkRepoBaseUrlInvalid.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidFileSourceInfo, Object> {

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
            if (value instanceof FileSourceCreateUpdateReq) {
                FileSourceCreateUpdateReq req = (FileSourceCreateUpdateReq) value;
                return isValid(req.getFileSourceTypeCode(), req.getFileSourceInfoMap());
            }
            if (value instanceof EsbCreateOrUpdateFileSourceV3Req) {
                EsbCreateOrUpdateFileSourceV3Req req = (EsbCreateOrUpdateFileSourceV3Req) value;
                return isValid(req.getType(), req.getAccessParams());
            }
            return true;
        }

        private boolean isValid(String fileSourceTypeCode, Map<String, Object> fileSourceInfoMap) {
            if (!FileSourceTypeEnum.isBlueKingArtifactory(fileSourceTypeCode)) {
                return true;
            }
            if (fileSourceInfoMap == null || fileSourceInfoMap.isEmpty()) {
                return true;
            }
            Object baseUrlObj = fileSourceInfoMap.get(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL);
            if (!(baseUrlObj instanceof String)) {
                return false;
            }
            String baseUrl = (String) baseUrlObj;
            if (StringUtils.isBlank(baseUrl)) {
                return false;
            }
            return baseUrl.startsWith("http://") || baseUrl.startsWith("https://");
        }
    }
}
