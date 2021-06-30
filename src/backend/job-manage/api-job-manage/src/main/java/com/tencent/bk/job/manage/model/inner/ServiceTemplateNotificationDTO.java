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

package com.tencent.bk.job.manage.model.inner;

import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @since 29/4/2020 17:22
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTemplateNotificationDTO {
    /**
     * 触发者
     * 用于获取【任务执行人】角色对应用户
     */
    private String triggerUser;

    /**
     * 业务Id
     * 用于获取【资源所属者】角色对应用户
     */
    private Long appId;

    /**
     * 触发通知的资源类型
     * 用于获取【资源所属者】角色对应用户
     *
     * @see com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum
     */
    private Integer resourceType;

    /**
     * 资源Id
     * 用于获取【资源所属者】角色对应用户
     */
    private String resourceId;

    /**
     * 要通知的用户与角色
     */
    private UserRoleInfoDTO receiverInfo;

    /**
     * 激活的通知渠道
     * 从前端提交数据获取的渠道Code
     */
    private List<String> activeChannels;

    /**
     * 通知模板唯一标识
     * 定时任务执行前通知传入值：NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_EXECUTE
     */
    private String templateCode;

    /**
     * 通知模板中需要被替换的变量取值表
     */
    private Map<String, String> variablesMap;
}
