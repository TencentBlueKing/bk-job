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

package com.tencent.bk.job.ticket.model.web.req;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.ticket.consts.CredentialTypeEnum;
import com.tencent.bk.job.ticket.model.credential.AccessKeySecretKey;
import com.tencent.bk.job.ticket.model.credential.Password;
import com.tencent.bk.job.ticket.model.credential.SecretKey;
import com.tencent.bk.job.ticket.model.credential.UsernamePassword;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import static com.tencent.bk.job.ticket.consts.CredentialTypeEnum.*;

@Data
@ApiModel("凭据创建请求")
public class CredentialCreateUpdateReq {

    @ApiModelProperty(value = "ID,更新凭据的时候需要传入，新建时不需要", required = false)
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
    private CredentialTypeEnum type;
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

    public String getSerializedValue() {
        if (type == SECRET_KEY) {
            SecretKey secretKey = new SecretKey(value1);
            return JsonUtils.toJson(secretKey);
        } else if (type == PASSWORD) {
            Password password = new Password(value1);
            return JsonUtils.toJson(password);
        } else if (type == APP_ID_SECRET_KEY) {
            AccessKeySecretKey accessKeySecretKey = new AccessKeySecretKey(value1, value2);
            return JsonUtils.toJson(accessKeySecretKey);
        } else if (type == USERNAME_PASSWORD) {
            UsernamePassword usernamePassword = new UsernamePassword(value1, value2);
            return JsonUtils.toJson(usernamePassword);
        } else {
            return null;
        }
    }
}
