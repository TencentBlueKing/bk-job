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

    /**
     * 业务
     */
    BUSINESS(SystemId.CMDB, ResourceId.APP, null),
    /**
     * 脚本
     */
    SCRIPT(SystemId.JOB, ResourceId.SCRIPT, BUSINESS),
    /**
     * 作业模版
     */
    TEMPLATE(SystemId.JOB, ResourceId.TEMPLATE, BUSINESS),
    /**
     * 执行方案
     */
    PLAN(SystemId.JOB, ResourceId.PLAN, TEMPLATE),
    /**
     * 定时任务
     */
    CRON(SystemId.JOB, ResourceId.CRON, BUSINESS),
    /**
     * 账号
     */
    ACCOUNT(SystemId.JOB, ResourceId.ACCOUNT, BUSINESS),
    /**
     * 公共脚本
     */
    PUBLIC_SCRIPT(SystemId.JOB, ResourceId.PUBLIC_SCRIPT, null),
    /**
     * 运营视图
     */
    DASHBOARD_VIEW(SystemId.JOB, ResourceId.DASHBOARD_VIEW, null),
    /**
     * 标签
     */
    TAG(SystemId.JOB, ResourceId.TAG, BUSINESS),
    /**
     * 主机
     */
    HOST(SystemId.CMDB, ResourceId.HOST, BUSINESS),
    /**
     * 动态分组
     */
    DYNAMIC_GROUP(SystemId.CMDB, ResourceId.DYNAMIC_GROUP, BUSINESS),
    /**
     * 文件源
     */
    FILE_SOURCE(SystemId.JOB, ResourceId.FILE_SOURCE, BUSINESS),
    /**
     * 凭证
     */
    TICKET(SystemId.JOB, ResourceId.TICKET, BUSINESS);

    private final String systemId;

    private final String id;

    private final ResourceTypeEnum parent;

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
