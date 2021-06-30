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

package com.tencent.bk.job.common.esb.model.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 源文件定义-ESB
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbFileSourceDTO {
    /**
     * 文件源类型
     *
     * @see com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum
     */
    @JsonProperty("file_type")
    private Integer fileType;

    /**
     * 从文件源分发的文件源Id，非文件源类型的为空
     */
    @JsonProperty("file_source_id")
    private Integer fileSourceId;

    /**
     * 文件列表
     */
    private List<String> files;

    /**
     * 账号
     */
    private String account;

    /**
     * ip列表
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("ip_list")
    private List<EsbIpDTO> ipList;

    /**
     * 动态分组ID列表
     */
    @JsonProperty("custom_query_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> dynamicGroupIdList;

    @JsonProperty("target_server")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private EsbServerDTO targetServer;
}
