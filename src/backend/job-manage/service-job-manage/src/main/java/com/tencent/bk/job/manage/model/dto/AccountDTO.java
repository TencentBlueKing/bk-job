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

import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbAccountV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"password", "dbPassword"})
public class AccountDTO {
    private Long id;
    /**
     * 账号名称
     */
    private String account;
    /**
     * 账号别名
     */
    private String alias;
    /**
     * 账号类型
     */
    private AccountTypeEnum type;
    /**
     * 账号用途
     */
    private AccountCategoryEnum category;
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 授权给
     */
    private String grantees;
    /**
     * 描述
     */
    private String remark;
    /**
     * 操作系统
     */
    private String os;
    /**
     * 系统账号密码
     */
    private transient String password;
    /**
     * DB账号对应的端口号
     */
    private Integer dbPort;
    /**
     * DB账号对应的密码
     */
    private transient String dbPassword;
    /**
     * DB账号依赖的系统账号
     */
    private Long dbSystemAccountId;
    /**
     * 创建者
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

    public ServiceAccountDTO toServiceAccountDTO() {
        ServiceAccountDTO serviceAccountDTO = new ServiceAccountDTO();
        serviceAccountDTO.setId(id);
        serviceAccountDTO.setAppId(appId);
        serviceAccountDTO.setAccount(account);
        serviceAccountDTO.setPassword(password);
        serviceAccountDTO.setAlias(alias);
        serviceAccountDTO.setCategory(category.getValue());
        serviceAccountDTO.setGrantees(grantees);
        serviceAccountDTO.setType(type.getType());
        serviceAccountDTO.setOs(os);
        serviceAccountDTO.setRemark(remark);
        if (dbSystemAccountId != null && dbSystemAccountId > 0) {
            ServiceAccountDTO dbSystemAccount = new ServiceAccountDTO();
            dbSystemAccount.setId(dbSystemAccountId);
            serviceAccountDTO.setDbSystemAccount(dbSystemAccount);
        }
        serviceAccountDTO.setDbPassword(dbPassword);
        serviceAccountDTO.setDbPort(dbPort);
        return serviceAccountDTO;

    }

    public EsbAccountV3DTO toEsbAccountV3DTO() {
        EsbAccountV3DTO esbAccount = new EsbAccountV3DTO();
        esbAccount.setId(id);
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(appId, esbAccount);
        esbAccount.setAccount(account);
        esbAccount.setAlias(alias);
        esbAccount.setCategory(category.getValue());
        esbAccount.setType(type.getType());
        esbAccount.setDbSystemAccountId(dbSystemAccountId);
        esbAccount.setOs(os);
        esbAccount.setCreator(creator);
        esbAccount.setCreateTime(createTime);
        esbAccount.setLastModifyUser(lastModifyUser);
        esbAccount.setLastModifyTime(lastModifyTime);
        return esbAccount;
    }


}
