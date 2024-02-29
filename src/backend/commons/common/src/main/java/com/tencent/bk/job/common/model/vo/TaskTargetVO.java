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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Data
@ApiModel("执行目标")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class TaskTargetVO {

    @ApiModelProperty(value = "全局变量名")
    private String variable;

    @ApiModelProperty(value = "主机节点信息, 版本升级之后作废")
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "兼容 API， 发布完成后前端使用 executeObjectsInfo 参数，该参数可删除")
    private TaskHostNodeVO hostNodeInfo;

    @ApiModelProperty(value = "任务执行对象信息")
    private TaskExecuteObjectsInfoVO executeObjectsInfo;

    public void validate() throws InvalidParamException {
        if (StringUtils.isNoneBlank(variable)) {
            hostNodeInfo = null;
            executeObjectsInfo = null;
            return;
        }

        if (executeObjectsInfo != null) {
            executeObjectsInfo.validate();
        } else if (hostNodeInfo != null) {
            hostNodeInfo.validate();
        } else {
            log.warn("TaskTarget is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public TaskExecuteObjectsInfoVO getExecuteObjectsInfoCompatibly() {
        if (executeObjectsInfo != null) {
            return executeObjectsInfo;
        } else if (hostNodeInfo != null) {
            TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = new TaskExecuteObjectsInfoVO();
            taskExecuteObjectsInfoVO.setHostList(hostNodeInfo.getHostList());
            taskExecuteObjectsInfoVO.setNodeList(hostNodeInfo.getNodeList());
            taskExecuteObjectsInfoVO.setDynamicGroupList(hostNodeInfo.getDynamicGroupList());
            return taskExecuteObjectsInfoVO;
        } else {
            return null;
        }
    }

}
