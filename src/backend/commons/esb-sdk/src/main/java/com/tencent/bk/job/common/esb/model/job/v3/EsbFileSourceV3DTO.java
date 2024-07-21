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

package com.tencent.bk.job.common.esb.model.job.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.constants.EsbTaskFileTypeEnum;
import com.tencent.bk.job.common.esb.validate.EsbFileSourceV3DTOGroupSequenceProvider;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.ValidFilePath;
import com.tencent.bk.job.common.validation.ValidationConstants;
import com.tencent.bk.job.common.validation.ValidationGroups;
import lombok.Data;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 源文件定义-ESB
 */
@Data
@GroupSequenceProvider(EsbFileSourceV3DTOGroupSequenceProvider.class)
public class EsbFileSourceV3DTO {
    /**
     * 文件列表
     */
    @JsonProperty("file_list")
    @NotEmpty(message = "{validation.constraints.InvalidSourceFileList_empty.message}")
    @ValidFilePath
    private List<String> files;

    /**
     * 账号
     */
    @NotNull(
        message = "{validation.constraints.AccountIdOrAlias_empty.message}",
        groups = ValidationGroups.FileSource.ServerFile.class
    )
    @Valid
    private EsbAccountV3BasicDTO account;

    @JsonProperty("server")
    @NotNull(
        message = "{validation.constraints.InvalidSourceFileHost_empty.message}",
        groups = ValidationGroups.FileSource.ServerFile.class
    )
    @Valid
    private EsbServerV3DTO server;

    /**
     * 文件源类型，不传默认为服务器文件
     *
     * @see com.tencent.bk.job.common.esb.constants.EsbTaskFileTypeEnum
     */
    @JsonProperty("file_type")
    @CheckEnum(
        enumClass = EsbTaskFileTypeEnum.class,
        message = "{validation.constraints.InvalidSourceFileType_illegal.message}"
    )
    private Integer fileType;

    /**
     * 从文件源分发的文件源Id，非文件源类型可不传
     */
    @JsonProperty("file_source_id")
    @NotNull(
        message = "{validation.constraints.InvalidFileSourceId_empty.message}",
        groups = ValidationGroups.FileSource.FileSourceId.class
    )
    @Min(
        value = ValidationConstants.COMMON_MIN_1,
        message = "{validation.constraints.InvalidFileSourceId_empty.message}",
        groups = ValidationGroups.FileSource.FileSourceId.class
    )
    private Integer fileSourceId;

    /**
     * 从文件源分发的文件源标识，非文件源类型可不传
     */
    @JsonProperty("file_source_code")
    @NotBlankField(
        message = "{validation.constraints.InvalidFileSourceCode_empty.message}",
        groups = ValidationGroups.FileSource.FileSourceId.class
    )
    private String fileSourceCode;
}
