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

package com.tencent.bk.job.manage.model.web.request.chooser.container;

import com.tencent.bk.job.manage.model.web.vo.chooser.container.ContainerTopologyNodeVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@NoArgsConstructor
@AllArgsConstructor
@Data
@ApiModel("通过拓扑节点集合获取容器集合")
public class ListContainerByTopologyNodesReq {

    @ApiModelProperty(value = "拓扑节点列表", required = true)
    private List<ContainerTopologyNodeVO> nodeList;

    @ApiModelProperty(value = "筛选条件：容器ID列表")
    private List<String> containerUidList;

    @ApiModelProperty(value = "筛选条件：容器名称列表, 支持模糊检索")
    private List<String> containerNameKeywordList;

    @ApiModelProperty(value = "筛选条件：Pod名称列表, 支持模糊检索")
    private List<String> podNameKeywordList;

    @ApiModelProperty(value = "筛选条件：Pod label")
    private Map<String, String> podLabels;

    @ApiModelProperty(value = "数据起始位置")
    private Integer start;

    @ApiModelProperty(value = "拉取数量")
    private Integer pageSize;
}
