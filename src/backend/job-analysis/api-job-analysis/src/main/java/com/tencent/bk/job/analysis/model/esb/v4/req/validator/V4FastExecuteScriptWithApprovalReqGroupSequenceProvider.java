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

package com.tencent.bk.job.analysis.model.esb.v4.req.validator;

import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastExecuteScriptWithApprovalRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.validator.V4ExecScriptReqGroupSequenceProvider;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 带审批的快速执行脚本请求的分组校验序列。
 * <p>
 * <b>分组判定逻辑全部委托给直接执行接口的 provider</b>，本类只做一件事：把序列里代表 Default 组的
 * 那个类换成被校验的子类。Hibernate Validator 要求重定义的默认分组序列必须包含 bean 自身类，
 * 直接复用父类的 provider 会在校验时抛 GroupDefinitionException。
 * <p>
 * 不复制判定逻辑是刻意的：分组决定了脚本与账号相关的约束在什么条件下生效，抄一份就会与直接执行接口漂移。
 */
public class V4FastExecuteScriptWithApprovalReqGroupSequenceProvider
    implements DefaultGroupSequenceProvider<V4FastExecuteScriptWithApprovalRequest> {

    private final V4ExecScriptReqGroupSequenceProvider delegate = new V4ExecScriptReqGroupSequenceProvider();

    @Override
    public List<Class<?>> getValidationGroups(V4FastExecuteScriptWithApprovalRequest request) {
        List<Class<?>> groups = new ArrayList<>();
        for (Class<?> group : delegate.getValidationGroups(request)) {
            groups.add(group == V4FastExecuteScriptRequest.class
                ? V4FastExecuteScriptWithApprovalRequest.class : group);
        }
        return groups;
    }
}
