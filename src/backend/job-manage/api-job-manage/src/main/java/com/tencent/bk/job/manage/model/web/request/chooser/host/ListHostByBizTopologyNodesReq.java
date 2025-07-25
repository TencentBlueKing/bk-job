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

package com.tencent.bk.job.manage.model.web.request.chooser.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ApiModel("通过拓扑节点集合获取主机集合")
public class ListHostByBizTopologyNodesReq {

    @ApiModelProperty(value = "拓扑节点列表", required = true)
    List<BizTopoNode> nodeList;

    @ApiModelProperty(value = "搜索内容（同时对主机IP/IPv6/主机名称/操作系统名称/云区域名称进行模糊搜索）")
    String searchContent;

    @ApiModelProperty(value = "筛选条件：ip关键字列表，ip与列表中任意一个关键字相似即命中")
    List<String> ipKeyList;

    @ApiModelProperty(value = "筛选条件：ipv6关键字列表，ipv6与列表中任意一个关键字相似即命中")
    List<String> ipv6KeyList;

    @ApiModelProperty(value = "筛选条件：主机名称关键字列表，主机名称与列表中任意一个关键字相似即命中")
    List<String> hostNameKeyList;

    @ApiModelProperty(value = "筛选条件：操作系统名称关键字列表，操作系统名称与列表中任意一个关键字相似即命中")
    List<String> osNameKeyList;

    @ApiModelProperty(value = "筛选条件：alive：0为Agent异常，1为Agent正常，不传则不筛选")
    Integer alive;

    @ApiModelProperty(value = "数据起始位置")
    Long start;

    @ApiModelProperty(value = "拉取数量")
    Long pageSize;

    /**
     * 获取清洗后的ip关键字列表
     *
     * @return 清洗后的ip关键字列表
     */
    public List<String> getCleanIpKeyList() {
        return getCleanStringList(ipKeyList);
    }

    /**
     * 获取清洗后的ipv6关键字列表
     *
     * @return 清洗后的ipv6关键字列表
     */
    public List<String> getCleanIpv6KeyList() {
        return getCleanStringList(ipv6KeyList);
    }

    /**
     * 获取清洗后的主机名称关键字列表
     *
     * @return 清洗后的主机名称关键字列表
     */
    public List<String> getCleanHostNameKeyList() {
        return getCleanStringList(hostNameKeyList);
    }

    /**
     * 获取清洗后的操作系统名称关键字列表
     *
     * @return 清洗后的操作系统名称关键字列表
     */
    public List<String> getCleanOsNameKeyList() {
        return getCleanStringList(osNameKeyList);
    }

    /**
     * 获取清洗后的字符串列表，去除null值与空字符串
     *
     * @param rawList 原始字符串列表
     * @return 清洗后的字符串列表
     */
    private List<String> getCleanStringList(List<String> rawList) {
        if (CollectionUtils.isEmpty(rawList)) {
            return rawList;
        }
        return rawList.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }
}
