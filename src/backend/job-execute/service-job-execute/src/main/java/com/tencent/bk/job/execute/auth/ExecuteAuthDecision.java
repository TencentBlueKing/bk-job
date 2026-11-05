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

package com.tencent.bk.job.execute.auth;

import lombok.Getter;
import lombok.ToString;

/**
 * 执行类鉴权的判定结果。除「是否通过」外，还告知失败落在哪个操作、哪个维度，
 * 供上层按维度组装无权限详情与申请信息。
 */
@Getter
@ToString
public class ExecuteAuthDecision {

    /**
     * 失败维度
     */
    public enum FailureScope {
        /**
         * 执行目标（主机 / 拓扑节点 / 动态分组 / 容器）维度未通过
         */
        EXECUTE_TARGET,
        /**
         * 关联的业务内资源（脚本 / 方案 / 模板）维度未通过
         */
        RESOURCE,
        /**
         * 无法区分是哪一维度导致的失败。v3 把资源与执行目标放进同一次鉴权调用，
         * 只能拿到一个整体结论，故 v3 失败时恒为该值
         */
        BOTH
    }

    private static final ExecuteAuthDecision PASS = new ExecuteAuthDecision(true, null, null);

    private final boolean pass;
    private final String failedActionId;
    private final FailureScope failureScope;

    private ExecuteAuthDecision(boolean pass, String failedActionId, FailureScope failureScope) {
        this.pass = pass;
        this.failedActionId = failedActionId;
        this.failureScope = failureScope;
    }

    public static ExecuteAuthDecision pass() {
        return PASS;
    }

    public static ExecuteAuthDecision fail(String failedActionId, FailureScope failureScope) {
        return new ExecuteAuthDecision(false, failedActionId, failureScope);
    }
}
