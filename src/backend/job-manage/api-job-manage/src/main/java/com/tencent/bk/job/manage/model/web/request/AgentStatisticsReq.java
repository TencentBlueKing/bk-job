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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostIdWithMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 主机状态批量查询请求
 */
@Data
@ApiModel("主机状态批量查询请求报文")
public class AgentStatisticsReq {

    @ApiModelProperty(value = "主机列表")
    List<HostIdWithMeta> hostList = new ArrayList<>();
    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty(value = "hostId列表")
    List<Long> hostIdList = new ArrayList<>();

    @CompatibleImplementation(explain = "兼容IPv6版本发布过程接口调用", version = "3.8.0")
    @ApiModelProperty(value = "IP列表")
    List<String> ipList;

    @ApiModelProperty(value = "业务拓扑节点列表(传 objectId 与 instanceId ，其余字段置空即可)")
    List<BizTopoNode> nodeList;
    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty(value = "业务拓扑节点列表(传 objectId 与 instanceId ，其余字段置空即可)")
    List<BizTopoNode> appTopoNodeList;

    @ApiModelProperty(value = "动态分组列表")
    List<DynamicGroupIdWithMeta> dynamicGroupList;
    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty(value = "动态分组Id列表")
    List<String> dynamicGroupIds;

    public List<Long> getHostIdList() {
        return hostList.stream()
            .map(HostIdWithMeta::getHostId)
            .collect(Collectors.toList());
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setHostIdList(List<Long> hostIdList) {
        this.hostIdList = hostIdList;
        if (hostIdList != null) {
            this.hostList = hostIdList.stream().map(hostId ->
                new HostIdWithMeta(hostId, null)
            ).collect(Collectors.toList());
        }
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setAppTopoNodeList(
        List<BizTopoNode> appTopoNodeList) {
        this.appTopoNodeList = appTopoNodeList;
        this.nodeList = appTopoNodeList;
    }

    public List<String> getDynamicGroupIds() {
        return dynamicGroupList.stream()
            .map(DynamicGroupIdWithMeta::getId)
            .collect(Collectors.toList());
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setDynamicGroupIds(List<String> dynamicGroupIds) {
        this.dynamicGroupIds = dynamicGroupIds;
        if (dynamicGroupIds != null) {
            this.dynamicGroupList = dynamicGroupIds.stream().map(id ->
                new DynamicGroupIdWithMeta(id, null)
            ).collect(Collectors.toList());
        }
    }
}
