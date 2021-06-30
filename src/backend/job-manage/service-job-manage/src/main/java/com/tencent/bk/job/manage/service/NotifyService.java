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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.model.vo.NotifyChannelVO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.inner.*;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.notify.*;

import java.util.List;
import java.util.Set;


/**
 * 消息通知服务
 */
public interface NotifyService {

    List<TriggerPolicyVO> listAppDefaultNotifyPolicies(String username, Long appId);


    Long saveAppDefaultNotifyPolicies(String username, Long appId, NotifyPoliciesCreateUpdateReq createUpdateReq);

    Long saveAppDefaultNotifyPoliciesToLocal(String username, Long appId, String triggerUser,
                                             NotifyPoliciesCreateUpdateReq createUpdateReq);

    Long saveAppDefaultNotifyPolicies(String username, Long appId, NotifyPoliciesCreateUpdateReq createUpdateReq,
                                      boolean checkAuth);

    List<TriggerTypeVO> listTriggerType(String username);


    List<ResourceTypeVO> listResourceType(String username);


    List<RoleVO> listRole(String username);

    /**
     * 获取通知角色列表
     *
     * @return 通知角色列表
     */
    List<AppRoleDTO> listRoles();


    List<ExecuteStatusVO> listExecuteStatus(String username);

    /**
     * 获取所有的通知渠道
     *
     * @return
     */
    List<NotifyEsbChannelDTO> listAllNotifyChannel();

    List<NotifyChannelVO> listAvailableNotifyChannel(String username);

    Integer setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req);

    List<UserVO> listUsers(
        String username,
        String prefixStr,
        Long offset,
        Long limit,
        Boolean excludeBlackUsers
    );

    List<NotifyBlackUserInfoVO> listNotifyBlackUsers(String username, Integer start, Integer pageSize);

    List<String> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req);

    Integer sendSimpleNotification(ServiceNotificationDTO notification);

    Set<String> findUserByResourceRoles(Long appId, String triggerUser, Integer resourceType, String resourceIdStr,
                                        Set<String> roleSet);

    PageTemplateVO getPageTemplate(String username);

    Integer sendNotificationsToUsers(ServiceUserNotificationDTO serviceUserNotificationDTO);

    Integer sendNotificationsToUsersByChannel(ServiceUserNotificationDTO serviceUserNotificationDTO,
                                              List<String> channelTypeList);

    Integer sendNotificationsToAdministrators(ServiceNotificationMessage serviceNotificationMessage);

    Integer sendTemplateNotification(ServiceTemplateNotificationDTO templateNotificationDTO);

    Integer triggerTemplateNotification(ServiceTriggerTemplateNotificationDTO triggerTemplateNotification);
}

