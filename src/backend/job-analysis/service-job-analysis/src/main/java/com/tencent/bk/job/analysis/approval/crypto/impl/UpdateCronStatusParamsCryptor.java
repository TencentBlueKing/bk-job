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
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import org.springframework.stereotype.Service;

/**
 * 启停定时任务的参数只有定时任务 ID 与目标状态，没有敏感字段。
 * <p>
 * 仍然显式登记一个空实现，而不是让注册表在查不到时放行：
 * 每个操作类型都必须有人明确回答过"这里有没有要加密的字段"。
 */
@Service
public class UpdateCronStatusParamsCryptor implements ApprovalParamsCryptor<V4UpdateCronStatusRequest> {

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.UPDATE_CRON_STATUS;
    }

    @Override
    public Class<V4UpdateCronStatusRequest> getParamsClass() {
        return V4UpdateCronStatusRequest.class;
    }

    @Override
    public void encrypt(V4UpdateCronStatusRequest params) {
    }

    @Override
    public void decrypt(V4UpdateCronStatusRequest params) {
    }

    @Override
    public ApprovalDisplayParams desensitize(V4UpdateCronStatusRequest params) {
        return ApprovalDisplayParams.of(params);
    }
}
