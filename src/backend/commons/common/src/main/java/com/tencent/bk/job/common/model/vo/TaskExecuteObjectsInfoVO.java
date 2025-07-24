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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
@ApiModel("任务执行对象信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class TaskExecuteObjectsInfoVO {

    @ApiModelProperty("主机列表")
    private List<HostInfoVO> hostList;

    @ApiModelProperty("主机拓扑节点 ID")
    private List<TargetNodeVO> nodeList;

    @ApiModelProperty("主机动态分组")
    private List<DynamicGroupIdWithMeta> dynamicGroupList;

    @ApiModelProperty("容器列表")
    private List<ContainerVO> containerList;

    public void validate() throws InvalidParamException {
        boolean allEmpty = true;
        if (CollectionUtils.isNotEmpty(nodeList)) {
            allEmpty = false;
            for (TargetNodeVO targetNodeVO : nodeList) {
                targetNodeVO.validate();
            }
        }
        if (CollectionUtils.isNotEmpty(dynamicGroupList)) {
            allEmpty = false;
            for (Object dynamicGroup : dynamicGroupList) {
                if (dynamicGroup == null) {
                    log.warn("Host dynamic group id is empty!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(hostList)) {
            allEmpty = false;
            for (HostInfoVO hostInfoVO : hostList) {
                hostInfoVO.validate();
            }
        }
        if (CollectionUtils.isNotEmpty(containerList)) {
            allEmpty = false;
        }
        if (allEmpty) {
            log.warn("TaskExecuteObjects is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }
}
