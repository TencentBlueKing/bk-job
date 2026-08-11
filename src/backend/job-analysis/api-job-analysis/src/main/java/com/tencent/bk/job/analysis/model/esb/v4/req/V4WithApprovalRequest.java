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

/**
 * 发起审批的请求体共有契约。
 * <p>
 * 6 个发起接口按操作类型拆分（网关的权限、限流、授权都是资源粒度，统一入口做不到"只许发起脚本执行审批"），
 * 各自的请求体<b>继承对应操作原有的 v4 请求体</b>，只多一个 approval_channel。有了这个接口，
 * 接口层读取渠道时不必对 6 个类型分别取值。
 * <p>
 * <b>渠道只能用枚举指定</b>：地址、appCode、密钥一律来自服务端配置，请求体中不得出现任何
 * URL / host / token 类字段 —— 否则调用方就能把回查目标指向自己控制的服务。
 */
public interface V4WithApprovalRequest {

    /**
     * 审批渠道枚举值，为空表示使用服务端默认渠道
     */
    String getApprovalChannel();
}
