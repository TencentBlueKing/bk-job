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

package com.tencent.bk.job.common.iam.constant;

import com.tencent.bk.sdk.iam.constants.SystemId;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 18/6/2020 11:31
 */
@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {

    // 业务/业务集下的资源
    /**
     * 业务
     */
    BUSINESS(SystemId.CMDB, ResourceTypeId.BIZ, null, true),
    /**
     * 业务集
     */
    BUSINESS_SET(SystemId.CMDB, ResourceTypeId.BUSINESS_SET, null, true),
    /**
     * 脚本
     */
    SCRIPT(SystemId.JOB, ResourceTypeId.SCRIPT, BUSINESS, true),
    /**
     * 作业模版
     */
    TEMPLATE(SystemId.JOB, ResourceTypeId.TEMPLATE, BUSINESS, true),
    /**
     * 执行方案
     */
    PLAN(SystemId.JOB, ResourceTypeId.PLAN, TEMPLATE, true),
    /**
     * 定时任务
     */
    CRON(SystemId.JOB, ResourceTypeId.CRON, BUSINESS, true),
    /**
     * 账号
     */
    ACCOUNT(SystemId.JOB, ResourceTypeId.ACCOUNT, BUSINESS, true),
    /**
     * 标签
     */
    TAG(SystemId.JOB, ResourceTypeId.TAG, BUSINESS, true),
    /**
     * 主机
     */
    HOST(SystemId.CMDB, ResourceTypeId.HOST, BUSINESS, true),
    /**
     * 动态分组
     */
    DYNAMIC_GROUP(SystemId.CMDB, ResourceTypeId.DYNAMIC_GROUP, BUSINESS, true),
    /**
     * 文件源
     */
    FILE_SOURCE(SystemId.JOB, ResourceTypeId.FILE_SOURCE, BUSINESS, true),
    /**
     * 凭证
     */
    TICKET(SystemId.JOB, ResourceTypeId.TICKET, BUSINESS, true),

    // 非业务/业务集资源
    /**
     * 公共脚本
     */
    PUBLIC_SCRIPT(SystemId.JOB, ResourceTypeId.PUBLIC_SCRIPT, null, false),
    /**
     * 运营视图
     */
    DASHBOARD_VIEW(SystemId.JOB, ResourceTypeId.DASHBOARD_VIEW, null, false);

    private final String systemId;

    private final String id;

    private final ResourceTypeEnum parent;

    // 是否为业务/业务集下的资源
    private final boolean scopeResource;

    public static ResourceTypeEnum getByResourceTypeId(String resourceTypeId) {
        if (resourceTypeId == null) {
            return null;
        }
        for (ResourceTypeEnum resourceType : values()) {
            if (resourceType.getId().equals(resourceTypeId)) {
                return resourceType;
            }
        }
        return null;
    }

}
