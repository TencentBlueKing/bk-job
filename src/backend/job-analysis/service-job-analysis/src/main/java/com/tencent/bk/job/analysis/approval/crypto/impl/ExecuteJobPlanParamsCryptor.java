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
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 启动执行方案的参数加解密：只保护密文类型的全局变量取值。
 */
@Service
public class ExecuteJobPlanParamsCryptor implements ApprovalParamsCryptor<V4ExecuteJobPlanRequest> {

    private final GlobalVarCryptor globalVarCryptor;
    private final CipherVarMatcherResolver matcherResolver;

    public ExecuteJobPlanParamsCryptor(GlobalVarCryptor globalVarCryptor,
                                       CipherVarMatcherResolver matcherResolver) {
        this.globalVarCryptor = globalVarCryptor;
        this.matcherResolver = matcherResolver;
    }

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN;
    }

    @Override
    public Class<V4ExecuteJobPlanRequest> getParamsClass() {
        return V4ExecuteJobPlanRequest.class;
    }

    @Override
    public void encrypt(V4ExecuteJobPlanRequest params) {
        if (hasNoGlobalVar(params)) {
            return;
        }
        globalVarCryptor.encrypt(GlobalVarAdapters.ofGlobalVars(params.getGlobalVars()), matcher(params));
    }

    @Override
    public void decrypt(V4ExecuteJobPlanRequest params) {
        if (hasNoGlobalVar(params)) {
            return;
        }
        globalVarCryptor.decrypt(GlobalVarAdapters.ofGlobalVars(params.getGlobalVars()), matcher(params));
    }

    @Override
    public ApprovalDisplayParams desensitize(V4ExecuteJobPlanRequest params) {
        if (!hasNoGlobalVar(params)) {
            globalVarCryptor.desensitize(GlobalVarAdapters.ofGlobalVars(params.getGlobalVars()));
        }
        return ApprovalDisplayParams.of(params);
    }

    private CipherVarMatcher matcher(V4ExecuteJobPlanRequest params) {
        return matcherResolver.ofPlan(params.getPlanId());
    }

    private boolean hasNoGlobalVar(V4ExecuteJobPlanRequest params) {
        return CollectionUtils.isEmpty(params.getGlobalVars());
    }
}
