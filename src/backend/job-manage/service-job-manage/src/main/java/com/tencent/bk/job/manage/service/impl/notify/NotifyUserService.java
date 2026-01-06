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

package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.notify.NotifyConsts;
import com.tencent.bk.job.manage.dao.notify.NotifyBlackUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyBlackUserInfoDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotifyUserService {

    private final NotifyBlackUserInfoDAO notifyBlackUserInfoDAO;
    private final IUserApiClient userApiClient;

    @Autowired
    public NotifyUserService(NotifyBlackUserInfoDAO notifyBlackUserInfoDAO,
                             IUserApiClient userApiClient) {
        this.notifyBlackUserInfoDAO = notifyBlackUserInfoDAO;
        this.userApiClient = userApiClient;
    }

    public List<NotifyBlackUserInfoVO> listNotifyBlackUsers(Integer start, Integer pageSize) {
        String tenantId = JobContextUtil.getTenantId();
        return notifyBlackUserInfoDAO.listNotifyBlackUserInfo(tenantId, start, pageSize);
    }

    private void saveBlackUsersToDB(Collection<SimpleUserInfo> users,
                                    String creator,
                                    List<String> resultList,
                                    String tenantId) {
        for (SimpleUserInfo user : users) {
            notifyBlackUserInfoDAO.insertNotifyBlackUserInfo(
                new NotifyBlackUserInfoDTO(
                    null,
                    tenantId,
                    user.getBkUsername(),
                    user.getDisplayName(),
                    creator,
                    System.currentTimeMillis()
                ));
            resultList.add(user.getDisplayName());
        }
    }

    public List<String> saveNotifyBlackUsers(String operator, NotifyBlackUsersReq req) {

        Collection<String> users = Collections.emptyList();
        if (StringUtils.isNotEmpty(req.getUsersStr())) {
            users = Arrays.asList(req.getUsersStr().split(NotifyConsts.SEPERATOR_COMMA));
        }
        return saveNotifyBlackUsers(operator, users);
    }

    public List<String> saveNotifyBlackUsers(String creatorUsername, Collection<String> usernames) {
        String tenantId = JobContextUtil.getTenantId();
        String creator = creatorUsername;
        List<SimpleUserInfo> operators = userApiClient.listUsersByUsernames(tenantId, Arrays.asList(creatorUsername));
        if (CollectionUtils.isNotEmpty(operators)
            && operators.get(0) != null
            && StringUtils.isNotBlank(operators.get(0).getDisplayName())) {
            creator = operators.get(0).getDisplayName();
        }
        List<SimpleUserInfo> users = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(usernames)) {
            users = userApiClient.listUsersByUsernames(
                tenantId,
                usernames);
        }
        val resultList = new ArrayList<String>();
        notifyBlackUserInfoDAO.deleteAllNotifyBlackUser(tenantId);
        saveBlackUsersToDB(users, creator, resultList, tenantId);
        return resultList;
    }

    public Set<String> filterBlackUser(Set<String> userSet, String tenantId) {
        // 过滤租户内黑名单用户
        Set<String> blackUserSet =
            notifyBlackUserInfoDAO.listNotifyBlackUserInfo(tenantId).stream()
                .map(NotifyBlackUserInfoDTO::getUsername).collect(Collectors.toSet());
        log.debug(String.format("sendUserChannelNotify:blackUserSet:%s", String.join(",", blackUserSet)));
        val removedBlackUserSet = userSet.stream().filter(blackUserSet::contains).collect(Collectors.toSet());
        userSet = userSet.stream().filter(it -> !blackUserSet.contains(it)).collect(Collectors.toSet());
        log.debug(String.format("sendUserChannelNotify:%d black users are removed, removed users=[%s]",
            removedBlackUserSet.size(), String.join(",", removedBlackUserSet)));
        return userSet;
    }
}
