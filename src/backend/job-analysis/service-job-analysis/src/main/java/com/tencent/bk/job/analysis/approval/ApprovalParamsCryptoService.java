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

package com.tencent.bk.job.analysis.approval;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams;

/**
 * 审批任务的参数快照与请求对象之间的转换，敏感字段在转换过程中加密 / 还原。
 * <p>
 * 加密在序列化之前、还原在反序列化之后完成，敏感字段由各操作类型的
 * {@link com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptor} 按字段语义决定。
 */
public interface ApprovalParamsCryptoService {

    /**
     * 加密敏感字段并序列化为参数快照
     */
    String encryptToSnapshot(ApprovalOperationTypeEnum operationType, Object params);

    /**
     * 从参数快照还原出请求对象，还原出的对象即下发给下游服务的请求体
     */
    Object decryptFromSnapshot(ApprovalOperationTypeEnum operationType, String snapshot);

    /**
     * 从参数快照还原出脱敏后的请求对象，供审批内容展示
     */
    ApprovalDisplayParams desensitizeFromSnapshot(ApprovalOperationTypeEnum operationType, String snapshot);
}
