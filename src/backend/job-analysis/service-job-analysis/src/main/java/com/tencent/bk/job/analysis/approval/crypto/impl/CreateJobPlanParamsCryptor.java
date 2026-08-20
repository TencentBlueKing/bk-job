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

package com.tencent.bk.job.analysis.approval.crypto.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.CipherVarMatcher;
import com.tencent.bk.job.analysis.approval.crypto.CipherVarMatcherResolver;
import com.tencent.bk.job.analysis.approval.crypto.GlobalVarAdapters;
import com.tencent.bk.job.analysis.approval.crypto.GlobalVarCryptor;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 创建执行方案的参数加解密：只保护密文类型的变量覆盖值，变量类型取自作业模板。
 */
@Service
public class CreateJobPlanParamsCryptor implements ApprovalParamsCryptor<V4CreateJobPlanRequest> {

    private final GlobalVarCryptor globalVarCryptor;
    private final CipherVarMatcherResolver matcherResolver;

    public CreateJobPlanParamsCryptor(GlobalVarCryptor globalVarCryptor,
                                      CipherVarMatcherResolver matcherResolver) {
        this.globalVarCryptor = globalVarCryptor;
        this.matcherResolver = matcherResolver;
    }

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.CREATE_JOB_PLAN;
    }

    @Override
    public Class<V4CreateJobPlanRequest> getParamsClass() {
        return V4CreateJobPlanRequest.class;
    }

    @Override
    public void encrypt(V4CreateJobPlanRequest params) {
        if (hasNoVariable(params)) {
            return;
        }
        globalVarCryptor.encrypt(GlobalVarAdapters.ofPlanVariables(params.getVariables()), matcher(params));
    }

    @Override
    public void decrypt(V4CreateJobPlanRequest params) {
        if (hasNoVariable(params)) {
            return;
        }
        globalVarCryptor.decrypt(GlobalVarAdapters.ofPlanVariables(params.getVariables()), matcher(params));
    }

    @Override
    public ApprovalDisplayParams desensitize(V4CreateJobPlanRequest params) {
        if (!hasNoVariable(params)) {
            globalVarCryptor.desensitize(GlobalVarAdapters.ofPlanVariables(params.getVariables()));
        }
        return ApprovalDisplayParams.of(params);
    }

    private CipherVarMatcher matcher(V4CreateJobPlanRequest params) {
        return matcherResolver.ofTemplate(params.getJobTemplateId());
    }

    private boolean hasNoVariable(V4CreateJobPlanRequest params) {
        return CollectionUtils.isEmpty(params.getVariables());
    }
}
