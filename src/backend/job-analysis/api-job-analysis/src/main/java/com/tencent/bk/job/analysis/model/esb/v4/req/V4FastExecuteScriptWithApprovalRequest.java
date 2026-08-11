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

package com.tencent.bk.job.analysis.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.model.esb.v4.req.validator.V4FastExecuteScriptWithApprovalReqGroupSequenceProvider;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

/**
 * 发起「快速执行脚本」审批。
 * <p>
 * <b>继承而非内嵌原请求体</b>：审批放行时执行的必须是同一组参数，继承让"字段与直接执行接口一致"
 * 由类型系统保证，不需要人工对账；而且 scope 字段留在请求体顶层 ——
 * {@code EsbAppResourceScopeReqAspect} 与 {@code BasicAppInterceptor} 都只看顶层，
 * scope 一旦嵌套进子对象，fillAppResourceScope 与审计补 scope 就都失效了。
 * <p>
 * 分组校验序列必须在本类上重新声明：Hibernate Validator 只读取被校验 bean <b>自身类</b>上的
 * {@code @GroupSequenceProvider}，父类上的那个对子类不生效，漏了这一条会让脚本/账号相关的
 * 分组约束在本接口上全部不校验。
 */
@Getter
@Setter
@GroupSequenceProvider(V4FastExecuteScriptWithApprovalReqGroupSequenceProvider.class)
public class V4FastExecuteScriptWithApprovalRequest extends V4FastExecuteScriptRequest
    implements V4WithApprovalRequest {

    /**
     * 审批渠道。不传时使用服务端默认渠道
     */
    @JsonProperty("approval_channel")
    @CheckEnum(
        enumClass = ApprovalChannelEnum.class,
        message = "{validation.constraints.ApprovalChannel_illegal.message}"
    )
    private String approvalChannel;
}
