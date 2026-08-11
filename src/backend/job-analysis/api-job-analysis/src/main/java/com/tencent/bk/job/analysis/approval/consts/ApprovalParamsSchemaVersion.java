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

package com.tencent.bk.job.analysis.approval.consts;

/**
 * 审批任务参数快照的结构版本。
 * <p>
 * 审批任务 TTL 为 8 小时，必然跨滚动升级窗口。若 6 个操作对应的 Request DTO 结构发生变化，
 * 旧快照反序列化可能<b>静默丢字段</b>（Jackson 默认 FAIL_ON_UNKNOWN_PROPERTIES=false），
 * 导致"用户批的"与"实际执行的"不一致 —— 这比直接失败更危险。
 * <p>
 * <b>什么时候必须升版本号</b>：改动 FAST_EXECUTE_SCRIPT / FAST_TRANSFER_FILE / EXECUTE_JOB_PLAN /
 * CREATE_JOB_PLAN / SAVE_CRON / UPDATE_CRON_STATUS 对应的 Request DTO 中<b>任何影响执行语义</b>的字段
 * （新增必填字段、改名、改类型、删除、改默认值语义）时，必须把 {@link #CURRENT} 加一。
 * 纯注释/文档改动不需要。
 * <p>
 * 放行时严格比对：版本不匹配即 fail-closed 拒绝本次放行（<b>不改变任务状态</b>，任务留在 PENDING
 * 直至自然过期），不采用"尽力兼容旧字段"的做法 —— 兼容逻辑本身就是漂移风险的来源。
 */
public final class ApprovalParamsSchemaVersion {

    /**
     * 当前参数快照结构版本
     */
    public static final int CURRENT = 1;

    private ApprovalParamsSchemaVersion() {
    }
}
