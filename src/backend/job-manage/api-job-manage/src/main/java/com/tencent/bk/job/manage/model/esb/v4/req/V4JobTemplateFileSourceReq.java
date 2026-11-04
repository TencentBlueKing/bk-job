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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.model.esb.v4.req.validator.V4JobTemplateFileSourceReqGroupSequenceProvider;
import com.tencent.bk.job.manage.model.esb.v4.req.validator.V4JobTemplateValidationGroups;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * OpenAPI V4 作业模板源文件。
 */
@Getter
@Setter
@GroupSequenceProvider(V4JobTemplateFileSourceReqGroupSequenceProvider.class)
public class V4JobTemplateFileSourceReq {

    /**
     * 文件路径列表。本地文件填 generate_local_file_upload_url 返回的路径。
     */
    @JsonProperty("file_list")
    @NotEmpty(message = "{validation.constraints.InvalidTemplateFileList_empty.message}")
    private List<String> fileList;

    /**
     * 源文件类型。1 服务器文件、2 本地文件、3 文件源文件。
     */
    @JsonProperty("file_type")
    @NotNull(message = "{validation.constraints.InvalidTemplateFileType.message}")
    @CheckEnum(enumClass = TaskFileTypeEnum.class,
        message = "{validation.constraints.InvalidTemplateFileType.message}")
    private Integer fileType;

    /**
     * 文件源 ID。仅文件源文件使用，可在业务的【文件源】管理页获取。
     */
    @JsonProperty("file_source_id")
    @NotNull(message = "{validation.constraints.InvalidTemplateFileSourceId.message}",
        groups = V4JobTemplateValidationGroups.FileType.FileSource.class)
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateFileSourceId.message}",
        groups = V4JobTemplateValidationGroups.FileType.FileSource.class)
    private Integer fileSourceId;

    @JsonProperty("account")
    @NotNull(message = "{validation.constraints.InvalidTemplateAccount_empty.message}",
        groups = V4JobTemplateValidationGroups.FileType.Server.class)
    @Valid
    private V4JobTemplateAccountReq account;

    @JsonProperty("execute_target")
    @NotNull(message = "{validation.constraints.InvalidTemplateExecuteTarget_empty.message}",
        groups = V4JobTemplateValidationGroups.FileType.Server.class)
    @Valid
    private V4JobTemplateExecuteTargetReq executeTarget;
}
