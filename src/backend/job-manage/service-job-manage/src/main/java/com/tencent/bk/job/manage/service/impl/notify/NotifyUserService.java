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

package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.notify.NotifyConsts;
import com.tencent.bk.job.manage.dao.notify.NotifyBlackUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyBlackUserInfoDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import com.tencent.bk.job.manage.service.UserCacheService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotifyUserService {

    private final NotifyBlackUserInfoDAO notifyBlackUserInfoDAO;
    private final UserCacheService userCacheService;

    @Autowired
    public NotifyUserService(NotifyBlackUserInfoDAO notifyBlackUserInfoDAO,
                             UserCacheService userCacheService) {
        this.notifyBlackUserInfoDAO = notifyBlackUserInfoDAO;
        this.userCacheService = userCacheService;
    }

    public List<NotifyBlackUserInfoVO> listNotifyBlackUsers(Integer start, Integer pageSize) {
        String tenantId = JobContextUtil.getTenantId();
        return notifyBlackUserInfoDAO.listNotifyBlackUserInfo(tenantId, start, pageSize);
    }

    private void saveBlackUsersToDB(Collection<BkUserDTO> users,
                                    String creator,
                                    List<String> resultList,
                                    String tenantId) {
        for (BkUserDTO user : users) {
            notifyBlackUserInfoDAO.insertNotifyBlackUserInfo(
                new NotifyBlackUserInfoDTO(
                    null,
                    tenantId,
                    user.getUsername(),
                    user.getDisplayName(),
                    creator,
                    System.currentTimeMillis()
                ));
            resultList.add(user.getDisplayName());
        }
    }

    public List<String> saveNotifyBlackUsers(String operator, NotifyBlackUsersReq req) {
        Collection<String> users = Arrays.asList(req.getUsersStr().split(NotifyConsts.SEPERATOR_COMMA));
        return saveNotifyBlackUsers(operator, users);
    }

    public List<String> saveNotifyBlackUsers(String operator, Collection<String> usernames) {
        String tenantId = JobContextUtil.getTenantId();
        List<BkUserDTO> users = userCacheService.listUsersByUsernames(usernames);
        val resultList = new ArrayList<String>();
        notifyBlackUserInfoDAO.deleteAllNotifyBlackUser(tenantId);
        saveBlackUsersToDB(users, operator, resultList, tenantId);
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

    private void filterBlackUsers(List<UserVO> userVOList) {
        String tenantId = JobContextUtil.getTenantId();
        // 通过uuid过滤黑名单内用户
        Set<String> blackUserSet =
            notifyBlackUserInfoDAO.listNotifyBlackUserInfo(tenantId).stream()
                .map(NotifyBlackUserInfoDTO::getUsername).collect(Collectors.toSet());
        log.debug(String.format("listUsers:blackUserSet:%s", String.join(",", blackUserSet)));
        userVOList.forEach(it -> {
            if (blackUserSet.contains(it.getEnglishName())) {
                it.setEnable(false);
            }
        });
    }

    public List<UserVO> listUsers(
        String prefixStr,
        Long offset,
        Long limit,
        Boolean excludeBlackUsers
    ) {
        if (null == prefixStr) {
            prefixStr = "";
        }
        if (null == offset || offset < 0) {
            offset = 0L;
        }
        if (null == limit || limit <= 0) {
            limit = -1L;
        }
        List<BkUserDTO> userList = searchUserByPrefix(prefixStr);
        List<UserVO> userVOList = userList.stream().map(it ->
            new UserVO(it.getUsername(), it.getDisplayName(), it.getLogo(), true)
        ).collect(Collectors.toList());
        if (excludeBlackUsers) {
            filterBlackUsers(userVOList);
        }
        return calcFinalListByOffsetAndLimit(userVOList, offset, limit);
    }

    private List<BkUserDTO> searchUserByPrefix(String prefixStr) {
        String tenantId = JobContextUtil.getTenantId();
        // 从数据库查
        if (prefixStr.contains(NotifyConsts.SEPERATOR_COMMA)) {
            // 前端回显，传全量
            List<String> displayNames = Arrays.asList(prefixStr.split(NotifyConsts.SEPERATOR_COMMA));
            while (displayNames.contains("")) {
                displayNames.remove("");
            }
            return userCacheService.listUsersByDisplayNames(tenantId, displayNames);
        } else {
            return userCacheService.listUsersByDisplayNamePrefix(tenantId, prefixStr, -1L);
        }
    }

    private List<UserVO> calcFinalListByOffsetAndLimit(List<UserVO> userVOList, Long offset, Long limit) {
        if (offset >= userVOList.size()) {
            return new ArrayList<>();
        }
        if (limit != -1L) {
            long stopIndex = offset + limit;
            if (stopIndex >= userVOList.size()) {
                stopIndex = userVOList.size();
            }
            return userVOList.subList(offset.intValue(), (int) stopIndex);
        } else {
            return userVOList.subList(offset.intValue(), userVOList.size());
        }
    }
}
