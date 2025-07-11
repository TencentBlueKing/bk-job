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

package com.tencent.bk.job.common.esb.model.job.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.util.ListUtil;
import lombok.Data;

import java.util.List;

/**
 * 源文件定义-ESB
 */
@Data
public class EsbFileSourceV3DTO {
    /**
     * 文件列表
     */
    @JsonProperty("file_list")
    private List<String> files;

    /**
     * 账号
     */
    private EsbAccountV3BasicDTO account;

    @JsonProperty("server")
    private EsbServerV3DTO server;

    /**
     * 文件源类型，不传默认为服务器文件
     *
     * @see com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum
     */
    @JsonProperty("file_type")
    private Integer fileType;

    /**
     * 从文件源分发的文件源Id，非文件源类型可不传
     */
    @JsonProperty("file_source_id")
    private Integer fileSourceId;

    /**
     * 从文件源分发的文件源标识，非文件源类型可不传
     */
    @JsonProperty("file_source_code")
    private String fileSourceCode;

    @JsonIgnore
    public List<String> getTrimmedFiles() {
        return ListUtil.trimStringList(files);
    }
}
