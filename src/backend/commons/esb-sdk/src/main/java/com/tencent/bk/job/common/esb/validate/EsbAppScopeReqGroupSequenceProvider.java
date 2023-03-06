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

package com.tencent.bk.job.common.esb.validate;

import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * EsbAppScopeReq 参数联合校验
 */
@Slf4j
public class EsbAppScopeReqGroupSequenceProvider implements DefaultGroupSequenceProvider<EsbAppScopeReq> {

    @Override
    public List<Class<?>> getValidationGroups(EsbAppScopeReq req) {
        List<Class<?>> validationGroups = new ArrayList<>();
        validationGroups.add(EsbAppScopeReq.class);
        if (req != null) {
            // 如果不兼容bk_biz_id，那么使用bk_scope_type+bk_scope_id参数校验方式
            if (!FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE, null)) {
                validationGroups.add(EsbAppScopeReq.UseScopeParam.class);
                return validationGroups;
            }

            // 如果参数中包含bk_scope_type/bk_scope_id,那么优先使用bk_scope_type+bk_scope_id
            if (StringUtils.isNotEmpty(req.getScopeType()) || StringUtils.isNotEmpty(req.getScopeId())) {
                validationGroups.add(EsbAppScopeReq.UseScopeParam.class);
            } else if (req.getBizId() != null) {
                validationGroups.add(EsbAppScopeReq.UseBkBizIdParam.class);
            }
        }
        return validationGroups;
    }
}
