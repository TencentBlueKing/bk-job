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

package com.tencent.bk.job.manage.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileDestinationV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import lombok.Data;

import java.util.List;

/**
 * 文件分发步骤
 *
 * @since 17/11/2020 20:37
 */
@Data
public class EsbFileStepV3DTO {
    /**
     * 源文件列表
     */
    @JsonProperty("file_source_list")
    private List<EsbFileSourceV3DTO> fileSourceList;

    /**
     * 分发目标信息
     */
    @JsonProperty("file_destination")
    private EsbFileDestinationV3DTO fileDestination;

    /**
     * 超时
     */
    private Long timeout;

    /**
     * 源机器上传限速
     */
    @JsonProperty("source_speed_limit")
    private Long sourceSpeedLimit;

    /**
     * 目标机器下载限速
     */
    @JsonProperty("destination_speed_limit")
    private Long destinationSpeedLimit;

    /**
     * 传输模式
     */
    @JsonProperty("transfer_mode")
    private Integer transferMode;
}
