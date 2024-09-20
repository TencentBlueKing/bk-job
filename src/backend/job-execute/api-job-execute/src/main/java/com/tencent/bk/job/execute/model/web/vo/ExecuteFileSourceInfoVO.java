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
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.ValidFilePath;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.web.validation.WebFileSourceDTOGroupSequenceProvider;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel("步骤源文件信息")
@GroupSequenceProvider(WebFileSourceDTOGroupSequenceProvider.class)
public class ExecuteFileSourceInfoVO {

    @ApiModelProperty(value = "文件类型 1-服务器文件 2-本地文件 3-文件源文件")
    @CheckEnum(
        enumClass = TaskFileTypeEnum.class,
        message = "{validation.constraints.InvalidSourceFileType_illegal.message}"
    )
    private Integer fileType;

    @ApiModelProperty("文件路径")
    @NotEmpty(message = "{validation.constraints.InvalidSourceFileList_empty.message}")
    @ValidFilePath
    private List<String> fileLocation;

    @ApiModelProperty(value = "文件 Hash 值 仅本地文件有")
    @NotBlankField(
        message = "{validation.constraints.InvalidFileHashValue_empty.message}",
        groups = ValidationGroups.FileSource.LocalFile.class
    )
    private String fileHash;

    @ApiModelProperty(value = "文件大小 仅本地文件有")
    @NotBlankField(
        message = "{validation.constraints.InvalidFileSize_empty.message}",
        groups = ValidationGroups.FileSource.LocalFile.class
    )
    private String fileSize;

    @ApiModelProperty(value = "主机列表")
    @NotNull(
        message = "{validation.constraints.InvalidSourceFileHost_empty.message}",
        groups = ValidationGroups.FileSource.ServerFile.class)
    private TaskTargetVO host;

    @ApiModelProperty(value = "主机账号")
    @JsonProperty("account")
    @NotNull(
        message = "{validation.constraints.AccountId_empty.message}",
        groups = ValidationGroups.FileSource.ServerFile.class
    )
    private Long accountId;

    @ApiModelProperty(value = "主机账号名称")
    private String accountName;

    @ApiModelProperty(value = "文件源ID")
    @NotNull(
        message = "{validation.constraints.InvalidFileSourceIdOrCode_empty.message}",
        groups = ValidationGroups.FileSource.FileSourceFile.class
    )
    private Integer fileSourceId;
}
