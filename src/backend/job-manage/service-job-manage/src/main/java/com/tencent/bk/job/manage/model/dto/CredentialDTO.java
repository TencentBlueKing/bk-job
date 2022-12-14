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

package com.tencent.bk.job.manage.model.dto;

import com.tencent.bk.job.common.model.dto.CommonCredential;
import com.tencent.bk.job.manage.common.consts.CredentialTypeEnum;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.manage.model.web.vo.CredentialVO;
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
    private CommonCredential credential;
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
            credentialVO.setValue1(credential.getUsername());
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            credentialVO.setValue1(credential.getAccessKey());
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
        serviceCredentialDTO.setCredential(credential);
        return serviceCredentialDTO;
    }

    public String getFirstValue() {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            return credential.getUsername();
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
            return credential.getPassword();
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
            return credential.getSecretKey();
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            return credential.getAccessKey();
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }

    public void setFirstValue(String val) {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            this.credential.setUsername(val);
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
            this.credential.setPassword(val);
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
            this.credential.setSecretKey(val);
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            this.credential.setAccessKey(val);
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }

    public String getSecondValue() {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            return credential.getPassword();
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
            return null;
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
            return null;
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            return credential.getSecretKey();
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }

    public void setSecondValue(String val) {
        if (type.equals(CredentialTypeEnum.USERNAME_PASSWORD.name())) {
            this.credential.setPassword(val);
        } else if (type.equals(CredentialTypeEnum.APP_ID_SECRET_KEY.name())) {
            this.credential.setSecretKey(val);
        } else if (type.equals(CredentialTypeEnum.PASSWORD.name())) {
        } else if (type.equals(CredentialTypeEnum.SECRET_KEY.name())) {
        } else {
            throw new RuntimeException("Not support type:" + type);
        }
    }
}
