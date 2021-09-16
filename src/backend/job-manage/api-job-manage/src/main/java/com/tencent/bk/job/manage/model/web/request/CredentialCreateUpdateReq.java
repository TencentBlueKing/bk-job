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

import com.tencent.bk.job.manage.common.consts.CredentialTypeEnum;
import com.tencent.bk.job.manage.model.credential.CommonCredential;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import static com.tencent.bk.job.manage.common.consts.CredentialTypeEnum.APP_ID_SECRET_KEY;
import static com.tencent.bk.job.manage.common.consts.CredentialTypeEnum.PASSWORD;
import static com.tencent.bk.job.manage.common.consts.CredentialTypeEnum.SECRET_KEY;
import static com.tencent.bk.job.manage.common.consts.CredentialTypeEnum.USERNAME_PASSWORD;

@Data
@ApiModel("凭据创建/更新请求")
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

    public CommonCredential toCommonCredential() {
        CommonCredential credential = new CommonCredential();
        credential.setType(type.name());
        if (type == SECRET_KEY) {
            credential.setSecretKey(value1);
        } else if (type == PASSWORD) {
            credential.setPassword(value1);
        } else if (type == APP_ID_SECRET_KEY) {
            credential.setAccessKey(value1);
            credential.setSecretKey(value2);
        } else if (type == USERNAME_PASSWORD) {
            credential.setUsername(value1);
            credential.setPassword(value2);
        } else {
            return null;
        }
        return credential;
    }
}
