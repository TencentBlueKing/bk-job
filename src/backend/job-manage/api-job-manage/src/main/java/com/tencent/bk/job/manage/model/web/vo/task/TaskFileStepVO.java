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

package com.tencent.bk.job.manage.model.web.vo.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @since 16/10/2019 14:46
 */
@Data
@ApiModel("步骤文件信息")
public class TaskFileStepVO {

    @ApiModelProperty("源文件列表")
    private List<TaskFileSourceInfoVO> fileSourceList;

    @ApiModelProperty("目标信息")
    private TaskFileDestinationInfoVO fileDestination;

    @ApiModelProperty("超时")
    private Long timeout;

    @ApiModelProperty("上传文件限速")
    @JsonProperty("uploadSpeedLimit")
    private Long originSpeedLimit;

    @ApiModelProperty("下载文件限速")
    @JsonProperty("downloadSpeedLimit")
    private Long targetSpeedLimit;

    /**
     * 传输模式
     */
    @ApiModelProperty(
        value = "传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 安全模式(FILESRCIP)；4 - 安全模式(YYYY-MM-DD)",
        required = true)
    private Integer transferMode;

    @ApiModelProperty("忽略错误 0 - 不忽略 1 - 忽略")
    private Integer ignoreError;

    public static Integer getTransferMode(DuplicateHandlerEnum duplicateHandlerEnum,
                                          NotExistPathHandlerEnum notExistPathHandlerEnum) {
        if (duplicateHandlerEnum == null) {
            // 默认覆盖
            duplicateHandlerEnum = DuplicateHandlerEnum.OVERWRITE;
        }
        if (notExistPathHandlerEnum == null) {
            // 默认直接创建
            notExistPathHandlerEnum = NotExistPathHandlerEnum.CREATE_DIR;
        }
        if (DuplicateHandlerEnum.OVERWRITE == duplicateHandlerEnum
            && NotExistPathHandlerEnum.STEP_FAIL == notExistPathHandlerEnum) {
            return 1;
        } else if (DuplicateHandlerEnum.OVERWRITE == duplicateHandlerEnum
            && NotExistPathHandlerEnum.CREATE_DIR == notExistPathHandlerEnum) {
            return 2;
        } else if (DuplicateHandlerEnum.GROUP_BY_IP == duplicateHandlerEnum
            && NotExistPathHandlerEnum.CREATE_DIR == notExistPathHandlerEnum) {
            return 3;
        } else if (DuplicateHandlerEnum.GROUP_BY_DATE_AND_IP == duplicateHandlerEnum
            && NotExistPathHandlerEnum.CREATE_DIR == notExistPathHandlerEnum) {
            return 4;
        } else {
            return 1;
        }
    }

    public boolean validate(boolean isCreate) {
        if (CollectionUtils.isEmpty(fileSourceList)) {
            JobContextUtil.addDebugMessage("Empty origin file list!");
            return false;
        }
        for (TaskFileSourceInfoVO taskFileSourceInfoVO : fileSourceList) {
            if (!taskFileSourceInfoVO.validate(isCreate)) {
                JobContextUtil.addDebugMessage("Invalid file info!");
                return false;
            }
        }
        if (fileDestination == null) {
            JobContextUtil.addDebugMessage("Empty destination info!");
            return false;
        }
        if (StringUtils.isBlank(fileDestination.getPath())) {
            JobContextUtil.addDebugMessage("Empty destination location!");
            return false;
        }
        if (fileDestination.getAccount() == null || fileDestination.getAccount() <= 0) {
            JobContextUtil.addDebugMessage("Empty account!");
            return false;
        }
        if (!fileDestination.getServer().validate(isCreate)) {
            JobContextUtil.addDebugMessage("Invalid destination!");
            return false;
        }
        if (transferMode == null || transferMode < 1 || transferMode > 4) {
            JobContextUtil.addDebugMessage("Invalid transferMode setting!");
            return false;
        }
        if (timeout == null || timeout < 0) {
            timeout = 0L;
        }
        if (originSpeedLimit == null || originSpeedLimit <= 0) {
            originSpeedLimit = null;
        }
        if (targetSpeedLimit == null || targetSpeedLimit <= 0) {
            targetSpeedLimit = null;
        }
        if (ignoreError == null || ignoreError < 0) {
            ignoreError = 0;
        }
        return true;
    }

    public Integer getNotExistPathHandler() {
        if (transferMode == 1) {
            return NotExistPathHandlerEnum.STEP_FAIL.getValue();
        } else {
            return NotExistPathHandlerEnum.CREATE_DIR.getValue();
        }
    }

    public Integer getDuplicateHandler() {
        switch (transferMode) {
            case 1:
            case 2:
                return DuplicateHandlerEnum.OVERWRITE.getId();
            case 3:
                return DuplicateHandlerEnum.GROUP_BY_IP.getId();
            case 4:
                return DuplicateHandlerEnum.GROUP_BY_DATE_AND_IP.getId();
            default:
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }
}
