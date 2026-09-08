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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.Valid;
import java.util.List;

/**
 * OpenAPI V4 作业模板执行目标。
 * 与 get_job_template_detail 响应中的 execute_target 同形状，响应可原样回传。
 */
@Getter
@Setter
public class V4JobTemplateExecuteTargetReq {

    /**
     * 引用模板中主机类型全局变量的名称。与下面三个具体目标字段可同时使用。
     */
    @JsonProperty("variable")
    private String variable;

    @JsonProperty("host_list")
    @Valid
    private List<OpenApiV4HostDTO> hostList;

    @JsonProperty("dynamic_group_list")
    @Valid
    private List<EsbDynamicGroupDTO> dynamicGroups;

    @JsonProperty("topo_node_list")
    @Valid
    private List<EsbCmdbTopoNodeDTO> topoNodes;

    /**
     * 静态容器列表。
     */
    @JsonProperty("container_list")
    @Valid
    private List<V4JobTemplateContainerDTO> containerList;

    /**
     * 容器动态筛选条件列表，多条之间取并集。
     */
    @JsonProperty("container_filter_list")
    @Valid
    private List<V4JobTemplateContainerFilterDTO> containerFilters;

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isBlank(variable)
            && CollectionUtils.isEmpty(hostList)
            && CollectionUtils.isEmpty(dynamicGroups)
            && CollectionUtils.isEmpty(topoNodes)
            && CollectionUtils.isEmpty(containerList)
            && CollectionUtils.isEmpty(containerFilters);
    }
}
