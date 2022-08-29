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

package com.tencent.bk.job.common.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 1/11/2019 12:15
 */
@Data
@ApiModel("主机节点信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskHostNodeVO {

    @ApiModelProperty("机器列表")
    private List<HostInfoVO> hostList;
    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty("机器IP列表")
    private List<HostInfoVO> ipList;

    @ApiModelProperty("节点 ID")
    private List<TargetNodeVO> nodeList;
    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty("节点 ID")
    private List<TargetNodeVO> topoNodeList;

    @ApiModelProperty("动态分组")
    private List<Object> dynamicGroupList;

    public boolean validate(boolean isCreate) {
        boolean allEmpty = true;
        if (CollectionUtils.isNotEmpty(nodeList)) {
            allEmpty = false;
            for (TargetNodeVO targetNodeVO : nodeList) {
                if (!targetNodeVO.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Host node info validate failed!");
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(dynamicGroupList)) {
            allEmpty = false;
            for (Object dynamicGroup : dynamicGroupList) {
                if (dynamicGroup == null) {
                    JobContextUtil.addDebugMessage("Host dynamic group id is empty!");
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(hostList)) {
            allEmpty = false;
            for (HostInfoVO hostInfoVO : hostList) {
                if (!hostInfoVO.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Host info validate failed!");
                    return false;
                }
            }
        }
        return !allEmpty;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setHostList(List<HostInfoVO> hostList) {
        this.hostList = hostList;
        this.ipList = hostList;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setIpList(List<HostInfoVO> ipList) {
        this.ipList = ipList;
        this.hostList = ipList;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setNodeList(List<TargetNodeVO> nodeList) {
        this.nodeList = nodeList;
        this.topoNodeList = nodeList;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    public void setTopoNodeList(List<TargetNodeVO> topoNodeList) {
        this.topoNodeList = topoNodeList;
        this.nodeList = topoNodeList;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本修改实现", version = "3.8.0")
    public List<String> getDynamicGroupIdList() {
        if (org.springframework.util.CollectionUtils.isEmpty(dynamicGroupList)) {
            return Collections.emptyList();
        }
        List<String> dynamicGroupIdList = new ArrayList<>();
        for (Object dynamicGroup : dynamicGroupList) {
            if (dynamicGroup instanceof String) {
                dynamicGroupIdList.add((String) dynamicGroup);
            } else {
                DynamicGroupIdWithMeta dynamicGroupIdWithMeta = JsonUtils.fromJson(
                    JsonUtils.toJson(dynamicGroup),
                    new TypeReference<DynamicGroupIdWithMeta>() {
                    }
                );
                dynamicGroupIdList.add(dynamicGroupIdWithMeta.getId());
            }
        }
        return dynamicGroupIdList;
    }

    @CompatibleImplementation(name = "ipv6", explain = "兼容方法，保证发布过程中无损变更，下个版本修改实现", version = "3.8.0")
    public void setDynamicGroupIdList(List<String> dynamicGroupIdList) {
        List<Object> dynamicGroupList = new ArrayList<>();
        for (String id : dynamicGroupIdList) {
            dynamicGroupList.add(new DynamicGroupIdWithMeta(id, null));
        }
        this.dynamicGroupList = dynamicGroupList;
    }
}
