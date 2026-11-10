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

import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;

/**
 * 执行类操作的鉴权执行器：只负责「能否执行」的判定，不负责无权限结果与申请信息的组装。
 *
 * <p>v3 与 v4 各有一套实现，由 {@code ExecuteAuthServiceImpl} 按灰度开关选择。之所以单独抽这层，
 * 是因为执行类鉴权的入参形态（一个操作 + 可选的业务内资源 + 一批执行目标）无法用通用的
 * {@code AuthService} 表达：v3 把资源与主机放进同一次调用，v4 则是操作只绑 host、资源维度改由伴随的
 * view 操作单独校验。</p>
 */
public interface ExecuteAuthExecutor {

    /**
     * 执行类操作鉴权。
     *
     * @param user             操作者
     * @param actionId         主操作 ID（如 {@code execute_script}）
     * @param appResourceScope 资源范围
     * @param resource         操作关联的业务内资源（脚本 / 执行方案 / 作业模板），快速执行与分发无此资源，传 null
     * @param executeTarget    执行目标（静态主机 / 拓扑节点 / 动态分组 / 容器）
     * @return 鉴权判定结果
     */
    ExecuteAuthDecision authExecute(User user,
                                    String actionId,
                                    AppResourceScope appResourceScope,
                                    ExecuteAuthResource resource,
                                    ExecuteTargetDTO executeTarget);
}
