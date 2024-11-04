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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampDeserializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@ApiModel("执行方案信息")
public class TaskPlanBasicInfoVO {

    /**
     * 执行方案 ID
     */
    @ApiModelProperty(value = "执行方案 ID")
    private Long id;

    /**
     * 执行方案名称
     */
    @ApiModelProperty(value = "执行方案名称")
    private String name;

    /**
     * 模版 ID
     */
    @ApiModelProperty(value = "模版 ID")
    private Long templateId;


    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者")
    private String creator;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long createTime;

    /**
     * 最后修改人
     */
    @ApiModelProperty(value = "最后更新者")
    private String lastModifyUser;

    /**
     * 最后修改时间
     */
    @ApiModelProperty(value = "最后更新时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long lastModifyTime;
}
