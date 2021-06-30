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

package com.tencent.bk.job.manage.model.inner;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ApiModel("账号")
@Getter
@Setter
@ToString(exclude = {"password", "dbPassword"})
public class ServiceAccountDTO {

    /**
     * 账号 ID
     */
    @ApiModelProperty("账号ID")
    private Long id;

    /**
     * 账号名称
     */
    @ApiModelProperty("账号名称")
    private String account;

    /**
     * 账号别名
     */
    @ApiModelProperty("账号别名")
    private String alias;

    /**
     * 业务 ID
     */
    @ApiModelProperty("业务ID")
    private Long appId;

    /**
     * 系统账号密码
     */
    @ApiModelProperty("系统账号密码")
    private String password;

    /**
     * 账号类型
     */
    @ApiModelProperty("账号类型")
    private Integer type;

    /**
     * 账号用途
     */
    @ApiModelProperty("账号用途")
    private Integer category;

    /**
     * 授权用户
     */
    @ApiModelProperty("授权给")
    private String grantees;

    /**
     * 账号描述
     */
    @ApiModelProperty("账号描述")
    private String remark;

    /**
     * 操作系统
     */
    @ApiModelProperty("操作系统")
    private String os;

    /**
     * DB账号对应的端口号
     */
    @ApiModelProperty("DB账号对应的端口号")
    private Integer dbPort;
    /**
     * DB账号对应的密码
     */
    @ApiModelProperty("DB账号对应的密码")
    private String dbPassword;
    /**
     * DB账号依赖的系统账号
     */
    @ApiModelProperty("DB账号依赖的系统账号")
    private ServiceAccountDTO dbSystemAccount;
}
