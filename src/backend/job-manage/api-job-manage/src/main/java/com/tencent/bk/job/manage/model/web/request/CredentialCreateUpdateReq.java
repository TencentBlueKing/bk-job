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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.common.model.dto.CommonCredential;
import com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum.APP_ID_SECRET_KEY;
import static com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum.PASSWORD;
import static com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum.SECRET_KEY;
import static com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum.USERNAME_PASSWORD;

@Data
@Schema(description = "凭证创建/更新请求")
public class CredentialCreateUpdateReq {

    /**
     * 凭证ID
     */
    @Schema(description = "凭证 ID", hidden = true)
    private String id;
    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
    /**
     * 类型
     */
    @Schema(description = "类型")
    private CredentialTypeEnum type;
    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;
    /**
     * 值1
     */
    @Schema(description = "值1")
    private String value1;
    /**
     * 值2
     */
    @Schema(description = "值2")
    private String value2;
    /**
     * 值3
     */
    @Schema(description = "值3")
    private String value3;

    /**
     * 凭证敏感字段(密码/SecretKey)加密算法
     * 若有值表示敏感字段是密文传输，将使用该算法解密；如果没有值则表示明文传输
     */
    @Schema(description = "加密算法")
    private String algorithm;

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
