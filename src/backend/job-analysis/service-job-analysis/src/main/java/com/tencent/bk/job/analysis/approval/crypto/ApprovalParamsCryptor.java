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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;

/**
 * 某个操作类型的参数快照加解密与展示脱敏。
 * <p>
 * 实现<b>直接操作强类型的请求对象</b>，加密在序列化之前、解密在反序列化之后完成，
 * 哪个字段敏感由字段本身决定，不依赖对 JSON 树的路径匹配。
 * <p>
 * <b>加密与展示脱敏由同一个实现负责</b>：拆开会漂移成"库里加密了、审批内容里明文展示"，
 * 且不会有任何编译或运行期报错。
 *
 * @param <T> 该操作类型的请求体类型
 */
public interface ApprovalParamsCryptor<T> {

    ApprovalOperationTypeEnum getOperationType();

    Class<T> getParamsClass();

    /**
     * 落库前就地加密敏感字段。加密失败向上抛出，绝不降级为明文落库
     */
    void encrypt(T params);

    /**
     * 放行执行前就地还原敏感字段，还原出的对象即下发给下游服务的请求体
     */
    void decrypt(T params);

    /**
     * 渲染审批内容前就地还原并脱敏。
     * <p>
     * <b>{@link #encrypt} 加密过的值一律不以原文出现在审批内容里</b>。未加密的字段则相反：
     * 它们是审批人判断风险的依据，须原样展示，篇幅较大的（如脚本内容）摘进
     * {@link ApprovalDisplayParams#getPlainTextBlocks()} 单独成块。
     */
    ApprovalDisplayParams desensitize(T params);
}
