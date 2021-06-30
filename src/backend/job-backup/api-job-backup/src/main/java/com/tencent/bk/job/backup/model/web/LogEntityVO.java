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

package com.tencent.bk.job.backup.model.web;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @since 23/7/2020 15:57
 */
@Data
@ApiModel("作业日志信息")
public class LogEntityVO {

    /**
     * 日志类型
     *
     * @see com.tencent.bk.job.backup.constant.LogEntityTypeEnum
     */
    @ApiModelProperty("日志类型 1-普通文字 2-错误 3-普通链接 4-关联模版 5-关联执行方案 6-提示输入密码 7-提示重新输入密码")
    private Integer type;

    /**
     * 日志产生时间
     */
    @ApiModelProperty("日志时间戳")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long timestamp;

    /**
     * 日志内容
     */
    @ApiModelProperty("日志消息")
    private String content;

    /**
     * 日志关联模版 ID
     */
    @ApiModelProperty("该行关联模版 ID")
    private Long templateId;

    /**
     * 日志关联执行方案 ID
     */
    @ApiModelProperty("该行关联执行方案 ID")
    private Long planId;

    /**
     * 链接文字
     */
    @ApiModelProperty("链接文字")
    private String linkText;

    /**
     * 链接地址
     */
    @ApiModelProperty("链接地址")
    private String linkUrl;
}
