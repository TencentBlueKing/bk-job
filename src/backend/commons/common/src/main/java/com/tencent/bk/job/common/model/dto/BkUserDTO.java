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

package com.tencent.bk.job.common.model.dto;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 作业平台通用的用户DTO
 */
@Getter
@Setter
@ToString
@Slf4j
public class BkUserDTO {
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 用户 id
     */
    private String username;
    /**
     * 用户展示名称
     */
    private String displayName;
    /**
     * 用户头像
     */
    private String logo;
    /**
     * 手机
     */
    private String phone;
    /**
     * 用户email
     */
    private String email;
    /**
     * 用户所在时区
     */
    private String timeZone;

    /**
     * 用户微信
     */
    private String wxUserId;

    /**
     * 用户语言，枚举值：zh-cn / en
     */
    private String language;

    /**
     * 当前所在环境是否开启了多租户
     */
    private Boolean tenantEnabled;

    /**
     * 用户所属租户 ID
     */
    private String tenantId;

    /**
     * 是否为系统租户
     */
    private Boolean systemTenant;

    /**
     * 获取用户的完整账号名称
     */
    public String getFullName() {
        return displayName + ":" + username + "@" + tenantId;
    }

    public void setTenantInfo(boolean tenantEnabled, String tenantId) {
        this.tenantEnabled = tenantEnabled;
        this.tenantId = tenantId;
        this.systemTenant = TenantIdConstants.SYSTEM_TENANT_ID.equals(tenantId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BkUserDTO bkUserDTO = (BkUserDTO) o;
        return Objects.equals(username, bkUserDTO.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    public boolean validate() {
        if (StringUtils.isEmpty(username)) {
            log.warn("Empty username");
            return false;
        }
        if (StringUtils.isEmpty(tenantId)) {
            log.warn("Empty tenantId");
            return false;
        }
        return true;
    }
}
