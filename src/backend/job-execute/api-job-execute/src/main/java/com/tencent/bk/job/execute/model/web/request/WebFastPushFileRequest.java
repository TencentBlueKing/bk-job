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

package com.tencent.bk.job.execute.model.web.request;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileDestinationInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileSourceInfoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 分发文件请求报文
 */
@Data
@ApiModel("分发文件请求报文")
public class WebFastPushFileRequest {
    /**
     * 文件分发任务名称
     */
    @ApiModelProperty(value = "文件分发作业名称", required = true)
    private String name;
    /**
     * 文件来源
     */
    @ApiModelProperty(value = "源文件", required = true)
    private List<ExecuteFileSourceInfoVO> fileSourceList;

    /**
     * 传输目标
     */
    private ExecuteFileDestinationInfoVO fileDestination;

    /**
     * 上传限速-MB
     */
    @ApiModelProperty(value = "上传限速-MB")
    private Integer uploadSpeedLimit;

    /**
     * 下载限速-MB
     */
    @ApiModelProperty(value = "下载限速-MB")
    private Integer downloadSpeedLimit;

    /**
     * 超时时间，单位秒
     */
    @ApiModelProperty(value = "超时时间，单位秒", required = true)
    @NotNull(message = "{validation.constraints.InvalidJobTimeout_empty.message}")
    @Range(min = JobConstants.MIN_JOB_TIMEOUT_SECONDS, max= JobConstants.MAX_JOB_TIMEOUT_SECONDS,
        message = "{validation.constraints.InvalidJobTimeout_outOfRange.message}")
    private Integer timeout;

    /**
     * 传输模式
     */
    @ApiModelProperty(value = "传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 保险模式(FILESRCIP)； 4 - 保险模式(YYYY-MM-DD)", required = true)
    private Integer transferMode;

    public Integer getDuplicateHandler() {
        FileTransferModeEnum mode = FileTransferModeEnum.getFileTransferModeEnum(this.transferMode);
        if (mode == null) {
            return null;
        }
        switch (mode) {
            case STRICT:
            case FORCE:
                return DuplicateHandlerEnum.OVERWRITE.getId();
            case SAFETY_IP_PREFIX:
                return DuplicateHandlerEnum.GROUP_BY_IP.getId();
            case SAFETY_DATE_PREFIX:
                return DuplicateHandlerEnum.GROUP_BY_DATE_AND_IP.getId();
            default:
                return null;
        }
    }

    public Integer getNotExistPathHandler() {
        if (transferMode.equals(FileTransferModeEnum.STRICT.getValue())) {
            return NotExistPathHandlerEnum.STEP_FAIL.getValue();
        } else {
            return NotExistPathHandlerEnum.CREATE_DIR.getValue();
        }
    }

}


