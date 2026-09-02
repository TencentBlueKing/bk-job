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

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;

/**
 * 把审批任务渲染成交给审批渠道展示的审批内容。
 * <p>
 * <b>只读库，不重跑 dryRun</b>：内容取自落库的 resolved_summary 与 operation_params，
 * 每次重新解析会让"用户看到的"与"当初批准的"产生新的差异。
 */
public interface ApprovalContentRenderer {

    /**
     * 渲染审批内容。
     * <p>
     * <b>敏感字段一律只输出占位符</b>（脚本内容例外，单独成章展示），
     * 明文与密文都不得出现在返回值里。
     */
    ApprovalContent render(ApprovalTaskDTO task);
}
