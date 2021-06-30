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

package com.tencent.bk.job.ticket.model.dto;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.ticket.consts.CredentialTypeEnum;
import com.tencent.bk.job.ticket.model.credential.AccessKeySecretKey;
import com.tencent.bk.job.ticket.model.credential.Password;
import com.tencent.bk.job.ticket.model.credential.SecretKey;
import com.tencent.bk.job.ticket.model.credential.UsernamePassword;
import com.tencent.bk.job.ticket.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.ticket.model.web.resp.CredentialVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CredentialDTO {
    /**
     * 主键Id
     */
    private String id;
    /**
     * 业务Id
     */
    private Long appId;
    /**
     * 名称
     */
    private String name;
    /**
     * 类型
     */
    private String type;
    /**
     * 描述
     */
    private String description;
    /**
     * 值JSON串
     */
    private String value;
    /**
     * 创建人
     */
    private String creator;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 最后修改人
     */
    private String lastModifyUser;
    /**
     * 最后修改时间
     */
    private Long lastModifyTime;

    public CredentialVO toVO() {
        CredentialVO credentialVO = new CredentialVO();
        credentialVO.setId(id);
        credentialVO.setName(name);
        credentialVO.setType(type);
        credentialVO.setDescription(description);
        credentialVO.setValue1("******");
        credentialVO.setValue2("******");
        credentialVO.setValue3("******");
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            UsernamePassword usernamePassword = JsonUtils.fromJson(value, UsernamePassword.class);
            credentialVO.setValue1(usernamePassword.getUsername());
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            AccessKeySecretKey accessKeySecretKey = JsonUtils.fromJson(value, AccessKeySecretKey.class);
            credentialVO.setValue1(accessKeySecretKey.getAccessKey());
        }
        credentialVO.setCreator(creator);
        credentialVO.setCreateTime(createTime);
        credentialVO.setLastModifyUser(lastModifyUser);
        credentialVO.setLastModifyTime(lastModifyTime);
        return credentialVO;
    }

    public ServiceCredentialDTO toServiceCredentialDTO() {
        ServiceCredentialDTO serviceCredentialDTO = new ServiceCredentialDTO();
        serviceCredentialDTO.setId(id);
        serviceCredentialDTO.setAppId(appId);
        serviceCredentialDTO.setName(name);
        serviceCredentialDTO.setType(type);
        serviceCredentialDTO.setValue(value);
        return serviceCredentialDTO;
    }

    public String getFirstValue() {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            UsernamePassword usernamePassword = JsonUtils.fromJson(this.value, UsernamePassword.class);
            return usernamePassword.getUsername();
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
            Password password = JsonUtils.fromJson(this.value, Password.class);
            return password.getPassword();
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
            SecretKey secretKey = JsonUtils.fromJson(this.value, SecretKey.class);
            return secretKey.getSecretKey();
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            AccessKeySecretKey accessKeySecretKey = JsonUtils.fromJson(this.value, AccessKeySecretKey.class);
            return accessKeySecretKey.getAccessKey();
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }

    public void setFirstValue(String val) {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            UsernamePassword usernamePassword = JsonUtils.fromJson(this.value, UsernamePassword.class);
            usernamePassword.setUsername(val);
            this.value = JsonUtils.toJson(usernamePassword);
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
            Password password = JsonUtils.fromJson(this.value, Password.class);
            password.setPassword(val);
            this.value = JsonUtils.toJson(password);
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
            SecretKey secretKey = JsonUtils.fromJson(this.value, SecretKey.class);
            secretKey.setSecretKey(val);
            this.value = JsonUtils.toJson(secretKey);
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            AccessKeySecretKey accessKeySecretKey = JsonUtils.fromJson(this.value, AccessKeySecretKey.class);
            accessKeySecretKey.setAccessKey(val);
            this.value = JsonUtils.toJson(accessKeySecretKey);
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }

    public String getSecondValue() {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            UsernamePassword usernamePassword = JsonUtils.fromJson(this.value, UsernamePassword.class);
            return usernamePassword.getPassword();
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
            return null;
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
            return null;
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            AccessKeySecretKey accessKeySecretKey = JsonUtils.fromJson(this.value, AccessKeySecretKey.class);
            return accessKeySecretKey.getSecretKey();
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }

    public void setSecondValue(String val) {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            UsernamePassword usernamePassword = JsonUtils.fromJson(this.value, UsernamePassword.class);
            usernamePassword.setPassword(val);
            this.value = JsonUtils.toJson(usernamePassword);
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            AccessKeySecretKey accessKeySecretKey = JsonUtils.fromJson(this.value, AccessKeySecretKey.class);
            accessKeySecretKey.setSecretKey(val);
            this.value = JsonUtils.toJson(accessKeySecretKey);
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }
}
