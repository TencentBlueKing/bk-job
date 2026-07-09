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

package com.tencent.bk.job.common.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 动态条件过滤器 Web 层入参 DTO。
 * <p>
 * 该 DTO 仅在 Web 层短暂存活：WebResource 接收并校验后，通过
 * {@link com.tencent.bk.job.common.util.converter.WebContainerConditionFilterConverter}
 * 转换为内部统一的 {@link com.tencent.bk.job.common.model.dto.KubeContainerFilter}，
 * 之后所有后端逻辑都只处理 KubeContainerFilter。
 * <p>
 * 结构为「多条拓扑路径 + 一组共享条件」：{@code kubeTopoList} 里每条 {@link WebKubeTopo} 是一个精确的
 * cluster→namespace→workload 路径（cluster 必填、其余可选），拓扑节点直接取前端从
 * {@code /topology/container} 拉到的 {@code (instanceId, instanceName)}；{@code propConditions} 是所有
 * 拓扑路径共用的字段级 AND 条件。
 * <p>
 * Web 入口语义：kubeTopoList 必填非空（Web 不存在「全业务执行」入口），由字段级 Bean Validation 兜底；
 * propConditions 字段白名单/运算符派发/value 形态等业务校验依赖 OperatorDispatcher，由程序式校验器
 * {@code WebContainerConditionFilterValidator} 在 WebResource 内完成。
 */
@Data
@Schema(description = "动态条件过滤器（Web 层入参）")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebContainerConditionFilter {

    @Schema(description = "拓扑路径列表，至少一项；多条路径共用同一组 propConditions")
    @NotEmpty(message = "{validation.constraints.WebContainerConditionFilter_kubeTopoListEmpty.message}")
    @Valid
    private List<WebKubeTopo> kubeTopoList;

    @Schema(description = "字段级 AND 条件，承载产品上「动态条件」的 field/operator/value 三元组列表")
    private List<KubePropCondition> propConditions;
}
