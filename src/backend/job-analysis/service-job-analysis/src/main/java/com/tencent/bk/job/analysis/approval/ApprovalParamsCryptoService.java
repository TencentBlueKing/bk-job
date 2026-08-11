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

/**
 * 审批任务参数快照的敏感字段加解密。
 * <p>
 * 参数快照会在库里驻留最长一个 TTL（默认 8 小时），加上清理任务的保留期后可能长达 30 天。
 * 快照里含脚本明文与密码类字段，明文落库等于把这些内容的暴露面从"一次执行"扩大到"一段驻留期"，
 * 因此落库前必须加密、放行前才解密。
 * <p>
 * <b>加密失败一律 fail-closed</b>：发起接口直接报错，绝不降级为明文落库。
 */
public interface ApprovalParamsCryptoService {

    /**
     * 加密参数快照中的敏感字段，其余字段原样保留
     *
     * @param operationType 操作类型，决定哪些字段是敏感字段
     * @param paramsJson    原始参数快照 JSON
     * @return 敏感字段已替换为密文的 JSON
     */
    String encryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson);

    /**
     * 还原参数快照中的敏感字段
     *
     * @param operationType 操作类型
     * @param paramsJson    落库的参数快照 JSON
     * @return 敏感字段已还原为明文的 JSON
     */
    String decryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson);
}
