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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.common.util.json.SkipLogFields;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel("账号创建、更新请求")
@ToString(exclude = {"password", "dbPassword"})
public class AccountCreateUpdateReq {

    @ApiModelProperty(value = "ID,更新账号的时候需要传入，新建账号不需要")
    private Long id;
    /**
     * 帐号名称
     */
    @ApiModelProperty(value = "帐号名称", required = true)
    private String account;

    @ApiModelProperty(value = "账号类型，新建的时候需要传入；1-Linux，2-Windows，9-Mysql，10-Oracle，11-DB2")
    private Integer type;

    @ApiModelProperty(value = "账号用途，新建的时候需要传入；1-系统账号，2-数据库账号")
    private Integer category;

    /**
     * 所属用户
     */
    @ApiModelProperty(value = "所属用户")
    private List<String> grantees;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 系统类型，Linux / Windows
     */
    @ApiModelProperty(value = "系统类型")
    private String os;

    /**
     * 别名，当重名时会让用户填写，不允许修改，并且最后会与os合并在一起生成一个标识性的
     */
    @ApiModelProperty(value = "别名")
    private String alias;

    @ApiModelProperty(value = "系统账号的密码(Windows)")
    @SkipLogFields
    private String password;

    @ApiModelProperty(value = "DB端口,创建/更新DB账号的时候必传")
    @Range(min = 0, max = 65535, message = "{validation.constraints.InvalidPort.message}")
    private Integer dbPort;

    @ApiModelProperty(value = "DB账号关联的系统账号,创建/更新DB账号的时候必传")
    private Long dbSystemAccountId;

    @ApiModelProperty(value = "DB账号的密码,创建/更新DB账号的时候必传")
    @SkipLogFields
    private String dbPassword;
}
