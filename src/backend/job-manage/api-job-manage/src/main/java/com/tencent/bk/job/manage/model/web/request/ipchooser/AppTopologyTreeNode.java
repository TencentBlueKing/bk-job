/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.manage.model.web.request.ipchooser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Date 2020/3/19
 * @Version 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ApiModel("业务拓扑节点信息")
public class AppTopologyTreeNode {
    @ApiModelProperty(value = "节点类型Id", required = true)
    private String objectId;
    @ApiModelProperty(value = "节点类型名称")
    private String objectName;
    @ApiModelProperty(value = "节点实例Id", required = true)
    private Long instanceId;
    @ApiModelProperty(value = "节点实例名称")
    private String instanceName;
    @ApiModelProperty(value = "子节点列表")
    private List<AppTopologyTreeNode> childs;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppTopologyTreeNode)) return false;
        AppTopologyTreeNode that = (AppTopologyTreeNode) o;
        return Objects.equals(objectId, that.objectId) &&
            Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId, instanceId);
    }

    public String getSimpleDesc() {
        return "(" + objectId + "," + instanceId + ")";
    }

    @Override
    public String toString() {
        return "AppTopologyTreeNode{" +
            "objectId='" + objectId + '\'' +
            ", instanceId=" + instanceId +
            '}';
    }
}
