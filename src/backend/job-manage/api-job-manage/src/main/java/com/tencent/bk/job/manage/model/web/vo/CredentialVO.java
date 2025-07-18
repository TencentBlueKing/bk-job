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

package com.tencent.bk.job.manage.model.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("凭证")
@Data
public class CredentialVO {
    /**
     * 主键Id
     */
    @ApiModelProperty("主键Id")
    private String id;
    /**
     * 名称
     */
    @ApiModelProperty("名称")
    private String name;
    /**
     * 类型
     */
    @ApiModelProperty("类型")
    private String type;
    /**
     * 描述
     */
    @ApiModelProperty("描述")
    private String description;
    /**
     * 值1
     */
    @ApiModelProperty("值1")
    private String value1;
    /**
     * 值2
     */
    @ApiModelProperty("值2")
    private String value2;
    /**
     * 值3
     */
    @ApiModelProperty("值3")
    private String value3;
    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    private String creator;
    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    /**
     * 最后修改人
     */
    @ApiModelProperty("最后修改人")
    private String lastModifyUser;
    /**
     * 最后修改时间
     */
    @ApiModelProperty("最后修改时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    @ApiModelProperty("是否可以管理")
    private Boolean canManage;

    @ApiModelProperty("是否可以使用")
    private Boolean canUse;
}
