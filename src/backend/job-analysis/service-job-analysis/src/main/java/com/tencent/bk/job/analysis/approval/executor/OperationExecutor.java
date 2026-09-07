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

package com.tencent.bk.job.analysis.approval.executor;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;

/**
 * 按操作类型分发到下游对外 OpenAPI 的出站 SPI。
 * <p>
 * <b>创建审批任务与审批放行走的是同一个 {@link #invoke}，只有 dryRun 取值不同</b> ——
 * 这是"预检与执行不漂移"的结构保证。任何"为了方便"给预检单开一条代码路径的改动都会破掉这条性质：
 * 预检与执行一旦不一致，用户批准的就不再是实际会执行的操作。
 *
 * @param <T> 该操作类型对应的原始 v4 请求体类型
 */
public interface OperationExecutor<T> {

    ApprovalOperationTypeEnum getOperationType();

    /**
     * 参数快照的目标类型，供反序列化用（由类型系统而非注释保证反序列化到哪个 Request 类）
     */
    Class<T> getParamsClass();

    /**
     * 调用下游对外 OpenAPI。
     * <p>
     * operator <b>只能取 {@link ApprovalTaskDTO#getCreator()}</b>，不得取当次请求的调用者 ——
     * 放行请求的发起者与审批任务的发起人不是同一个概念，用错就等于允许他人借已批准的任务执行操作。
     * <p>
     * <b>不得传 skipAuth</b>：放行时 IAM 鉴权必须在完整链路中真实发生。
     * <p>
     * 下游的失败一律以异常形态到达（微服务下由 FeignErrorDecoder 还原成 OpenApiPropagatedException，
     * 轻量化部署下是被调服务原样抛出），因此<b>能正常返回就意味着下游已经成功</b>。实现<b>不得</b>把失败
     * 响应翻译成某种"失败结果对象"返回：调用方据此区分"确定未执行"与"结果未知"，
     * 谎报会让同一个操作被执行两次。
     *
     * @param params 从 DB 参数快照解密反序列化出的请求体，不接受任何外部覆盖
     * @param task   审批任务；创建阶段传入的是尚未落库的任务对象
     * @param dryRun true 为创建阶段预检，false 为放行执行
     * @return 预检响应只有 dry_run_summary，执行响应只有 data
     */
    EsbV4Response<?> invoke(T params, ApprovalTaskDTO task, boolean dryRun);
}
