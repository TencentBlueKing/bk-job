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
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @since 1/11/2019 12:15
 */
@Data
@ApiModel("主机节点信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskHostNodeVO {

    @ApiModelProperty("机器 IP 列表")
    private List<HostInfoVO> ipList;

    @ApiModelProperty("节点 ID")
    private List<TargetNodeVO> topoNodeList;

    @ApiModelProperty("动态分组 ID")
    private List<String> dynamicGroupList;

    public boolean validate(boolean isCreate) {
        boolean allEmpty = true;
        if (CollectionUtils.isNotEmpty(topoNodeList)) {
            allEmpty = false;
            for (TargetNodeVO targetNodeVO : topoNodeList) {
                if (!targetNodeVO.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Host node info validate failed!");
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(dynamicGroupList)) {
            allEmpty = false;
            for (String dynamicGroup : dynamicGroupList) {
                if (!StringUtils.isNoneBlank(dynamicGroup)) {
                    JobContextUtil.addDebugMessage("Host dynamic group id is empty!");
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(ipList)) {
            allEmpty = false;
            for (HostInfoVO hostInfoVO : ipList) {
                if (!hostInfoVO.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Host info validate failed!");
                    return false;
                }
            }
        }
        return !allEmpty;
    }
}
