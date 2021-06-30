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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampDeserializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @since 8/11/2019 15:32
 */
@ApiModel("业务账号")
@Data
public class AccountVO {
    /**
     * 主键
     */
    @ApiModelProperty("主键")
    private Long id;
    /**
     * 帐号名称
     */
    @ApiModelProperty("帐号名称")
    private String account;

    @ApiModelProperty("账号类型")
    private Integer type;

    @ApiModelProperty("账号类型描述")
    private String typeName;

    @ApiModelProperty("账号用途")
    private Integer category;

    @ApiModelProperty("账号用途描述")
    private String categoryName;

    /**
     * 业务 ID
     */
    @ApiModelProperty("业务ID")
    private Long appId;
    /**
     * 所属用户
     */
    @ApiModelProperty("所属用户")
    private List<String> ownerUsers;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;

    /**
     * 系统类型，Linux / Windows
     */
    @ApiModelProperty("系统类型")
    private String os;

    /**
     * 别名，当重名时会让用户填写，不允许修改，并且最后会与os合并在一起生成一个标识性的
     */
    @ApiModelProperty("别名")
    private String alias;

    @ApiModelProperty("系统账号的密码")
    private String password;

    @ApiModelProperty("DB端口")
    private Integer dbPort;

    @ApiModelProperty("DB账号关联的系统账号")
    private Long dbSystemAccountId;

    @ApiModelProperty("DB账号的密码")
    private String dbPassword;

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
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long lastModifyTime;
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
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long createTime;

    @ApiModelProperty("是否可以管理")
    private Boolean canManage;

    @ApiModelProperty("是否可以使用")
    private Boolean canUse;
}
