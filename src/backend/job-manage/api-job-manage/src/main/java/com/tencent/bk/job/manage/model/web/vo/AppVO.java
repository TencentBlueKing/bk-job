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

package com.tencent.bk.job.manage.model.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务VO
 */
@NoArgsConstructor
@ApiModel("业务")
@Data
public class AppVO {
    @ApiModelProperty(value = "业务ID", hidden = true)
    @DeprecatedAppLogic
    private Long id;
    @ApiModelProperty("资源范围类型")
    private String scopeType;
    @ApiModelProperty("资源范围ID")
    private String scopeId;
    @ApiModelProperty("业务名称")
    private String name;
    @ApiModelProperty("业务类型")
    @DeprecatedAppLogic
    private Integer type;
    @ApiModelProperty("是否有权限")
    private Boolean hasPermission;
    @ApiModelProperty("是否收藏")
    private Boolean favor;
    @ApiModelProperty("收藏时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long favorTime;

    public AppVO(Long id,
                 String scopeType,
                 String scopeId,
                 String name,
                 Integer type,
                 Boolean hasPermission,
                 Boolean favor,
                 Long favorTime) {
        this.id = id;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.name = name;
        this.type = type;
        this.hasPermission = hasPermission;
        this.favor = favor;
        this.favorTime = favorTime;
    }
}
