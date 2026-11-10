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

package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.constant.IamV4RoleId;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源类型 → 资源创建者应被授予的角色。
 *
 * <p>v4 没有 v3 的资源实例注册接口，创建者权限改由角色授权承载：把该资源类型对应的
 * 单资源角色连同新建实例一起授予创建者，即得到「该实例上的全部操作」，与 v3 创建者权限等价。</p>
 */
public class IamV4CreatorRoleMapper {

    private static final Map<String, String> ROLE_BY_V4_RESOURCE_TYPE = new HashMap<>();

    static {
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.SCRIPT, IamV4RoleId.SCRIPT_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.TEMPLATE, IamV4RoleId.JOB_TEMPLATE_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.PLAN, IamV4RoleId.JOB_PLAN_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.TAG, IamV4RoleId.TAG_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.CRON, IamV4RoleId.CRON_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.FILE_SOURCE, IamV4RoleId.FILE_SOURCE_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.TICKET, IamV4RoleId.TICKET_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.ACCOUNT, IamV4RoleId.ACCOUNT_MANAGER);
        ROLE_BY_V4_RESOURCE_TYPE.put(IamV4ResourceTypeId.PUBLIC_SCRIPT, IamV4RoleId.PUBLIC_SCRIPT_MANAGER);
    }

    /**
     * 按 v4 资源类型 ID 取创建者角色，无对应角色时返回 null。
     *
     * <p>无对应角色即「该资源类型不支持创建者授权」：业务 / 拓扑 / 主机 / 容器等资源不由 Job 创建，
     * 不存在创建者授权场景。</p>
     */
    public static String getCreatorRole(String v4ResourceTypeId) {
        return v4ResourceTypeId == null ? null : ROLE_BY_V4_RESOURCE_TYPE.get(v4ResourceTypeId);
    }

    private IamV4CreatorRoleMapper() {
    }
}
