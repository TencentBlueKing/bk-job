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

package com.tencent.bk.job.common.cc.model.req;

import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import lombok.Data;

/**
 * cmdb-sdk 入参：根据动态条件过滤器分页查询业务下的容器。
 * <p>
 * 与 {@link ListKubeContainerByTopoReq} 区别：本入参以高层的 {@link KubeContainerFilter}
 * 描述「拓扑过滤 + 字段级 propConditions」一体化条件，由 cmdb-sdk 内部翻译为 cmdb 的
 * PropertyFilterDTO；调用方无需感知 cmdb 协议细节。
 * <p>
 * 翻译实现位于 BizCmdbClient（Layer 2 补齐）；Layer 1 仅提供签名和入参形态。
 */
@Data
public class KubeContainerQueryReq {

    /**
     * 业务 ID
     */
    private Long bizId;

    /**
     * 动态条件过滤器（拓扑 filter + 字段级 propConditions）
     */
    private KubeContainerFilter filter;

    /**
     * 分页起始位置；为 null 时由 sdk 使用默认值或全量返回
     */
    private Integer start;

    /**
     * 分页大小；为 null 时由 sdk 使用默认值或全量返回
     */
    private Integer pageSize;
}
