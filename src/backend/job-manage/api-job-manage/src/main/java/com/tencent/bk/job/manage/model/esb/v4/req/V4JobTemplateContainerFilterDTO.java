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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * OpenAPI V4 作业模板容器动态筛选条件。
 * 拓扑路径用 CMDB 数字 ID 表达，与 v4 快速执行接口的 kube_container_filters（集群 UID / namespace 名称）形态不同，
 * 两者不可互换。
 */
@Getter
@Setter
public class V4JobTemplateContainerFilterDTO {

    /**
     * 调用方自定义的条件名称，仅用于页面展示区分多条筛选条件。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("name")
    @Size(max = 60, message = "{validation.constraints.InvalidTemplateContainerFilterName.message}")
    private String name;

    /**
     * 拓扑路径列表，多条路径之间取并集，且共用同一组 prop_conditions。
     */
    @JsonProperty("kube_topo_list")
    @NotEmpty(message = "{validation.constraints.InvalidTemplateKubeTopoList_empty.message}")
    @Valid
    private List<V4KubeTopoDTO> kubeTopoList;

    /**
     * 容器字段级筛选条件，条件之间取交集。
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("prop_conditions")
    @Valid
    private List<V4JobTemplatePropConditionDTO> propConditions;
}
