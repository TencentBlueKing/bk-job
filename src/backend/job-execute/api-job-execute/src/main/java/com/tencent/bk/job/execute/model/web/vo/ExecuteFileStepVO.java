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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

import static com.tencent.bk.job.common.constant.DuplicateHandlerEnum.GROUP_BY_IP;
import static com.tencent.bk.job.common.constant.DuplicateHandlerEnum.OVERWRITE;
import static com.tencent.bk.job.common.constant.NotExistPathHandlerEnum.CREATE_DIR;
import static com.tencent.bk.job.common.constant.NotExistPathHandlerEnum.STEP_FAIL;

@Data
@ApiModel("步骤文件信息")
public class ExecuteFileStepVO {

    @ApiModelProperty("源文件列表")
    @JsonProperty("fileSourceList")
    private List<ExecuteFileSourceInfoVO> fileSourceList;

    @ApiModelProperty("目标信息")
    @JsonProperty("fileDestination")
    private ExecuteFileDestinationInfoVO fileDestination;

    @ApiModelProperty("超时")
    private Integer timeout;

    @ApiModelProperty("上传文件限速")
    @JsonProperty("uploadSpeedLimit")
    private Integer originSpeedLimit;

    @ApiModelProperty("下载文件限速")
    @JsonProperty("downloadSpeedLimit")
    private Integer targetSpeedLimit;

    /**
     * 传输模式
     */
    @ApiModelProperty(value = "传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 安全模式", required = true)
    private Integer transferMode;

    @ApiModelProperty("忽略错误 0 - 不忽略 1 - 忽略")
    private Integer ignoreError;

    public static Integer getTransferMode(DuplicateHandlerEnum duplicateHandlerEnum,
                                          NotExistPathHandlerEnum notExistPathHandlerEnum) {
        if (duplicateHandlerEnum == null) {
            // 默认覆盖
            duplicateHandlerEnum = OVERWRITE;
        }
        if (notExistPathHandlerEnum == null) {
            // 默认直接创建
            notExistPathHandlerEnum = CREATE_DIR;
        }
        if (OVERWRITE == duplicateHandlerEnum && STEP_FAIL == notExistPathHandlerEnum) {
            return 1;
        } else if (OVERWRITE == duplicateHandlerEnum && CREATE_DIR == notExistPathHandlerEnum) {
            return 2;
        } else if (GROUP_BY_IP == duplicateHandlerEnum && CREATE_DIR == notExistPathHandlerEnum) {
            return 3;
        } else {
            return 1;
        }
    }

    public Integer getNotExistPathHandler() {
        if (transferMode == 1) {
            return STEP_FAIL.getValue();
        } else {
            return CREATE_DIR.getValue();
        }
    }
}
