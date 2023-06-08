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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.FilePathValidateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @since 16/10/2019 15:45
 */
@Data
@ApiModel("步骤源文件信息")
@Slf4j
public class TaskFileSourceInfoVO {

    @ApiModelProperty(value = "文件 ID 新建填 0")
    private Long id;

    @ApiModelProperty(value = "文件类型 1-服务器文件 2-本地文件 3-文件源文件")
    private Integer fileType;

    @ApiModelProperty("文件路径")
    private List<String> fileLocation;

    @ApiModelProperty(value = "文件 Hash 值 仅本地文件有")
    private String fileHash;

    @ApiModelProperty(value = "文件大小 仅本地文件有")
    private String fileSize;

    @ApiModelProperty(value = "主机列表")
    private TaskTargetVO host;

    @ApiModelProperty(value = "主机账号")
    private Long account;

    @ApiModelProperty(value = "主机账号名称")
    private String accountName;

    @ApiModelProperty(value = "文件源ID，来自文件源的文件需要传入")
    private Integer fileSourceId;

    public void validate(boolean isCreate) throws InvalidParamException {
        if (fileType == null || fileType <= 0 || fileType > 3) {
            log.warn("Invalid file type!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        switch (fileType) {
            case 1:
                if (CollectionUtils.isEmpty(fileLocation)) {
                    log.warn("Empty file location!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                for (String file : fileLocation) {
                    if (!FilePathValidateUtil.validateFileSystemAbsolutePath(file)) {
                        log.warn("fileLocation is illegal!");
                        throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                    }
                }
                host.validate(isCreate);
                if (account == null || account <= 0) {
                    log.warn("Invalid host account!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                break;
            case 2:
                try {
                    Long.valueOf(fileSize);
                } catch (NumberFormatException e) {
                    log.warn("Invalid file size!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                break;
            case 3:
                if (fileSourceId == null || fileSourceId <= 0) {
                    log.warn("Invalid fileSourceId!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                break;
            default:
                log.warn("Invalid file type!");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }
}
