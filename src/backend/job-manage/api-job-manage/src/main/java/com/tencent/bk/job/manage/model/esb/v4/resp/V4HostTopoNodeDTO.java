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

package com.tencent.bk.job.manage.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * OpenAPI V4 业务主机拓扑树节点。
 * <p>
 * 树形结构，默认全部展开（通过 child 递归返回完整层级），不包含懒加载(lazy)相关字段。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class V4HostTopoNodeDTO {

    /**
     * 节点类型 ID，如 biz/set/module。
     */
    @JsonProperty("object_id")
    private String objectId;

    /**
     * 节点类型名称。
     */
    @JsonProperty("object_name")
    private String objectName;

    /**
     * 节点实例 ID。
     */
    @JsonProperty("instance_id")
    private Long instanceId;

    /**
     * 节点实例名称。
     */
    @JsonProperty("instance_name")
    private String instanceName;

    /**
     * 该节点（含子节点去重）下的主机数量。
     */
    @JsonProperty("host_count")
    private Integer hostCount;

    /**
     * 子节点列表；叶子节点不返回该字段。
     */
    @JsonProperty("child")
    private List<V4HostTopoNodeDTO> child;
}
