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

import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyBlackUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.EsbUserInfoDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyBlackUserInfoDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotifyUserService {

    private final NotifyBlackUserInfoDAO notifyBlackUserInfoDAO;
    private final EsbUserInfoDAO esbUserInfoDAO;

    @Autowired
    public NotifyUserService(NotifyBlackUserInfoDAO notifyBlackUserInfoDAO,
                             EsbUserInfoDAO esbUserInfoDAO) {
        this.notifyBlackUserInfoDAO = notifyBlackUserInfoDAO;
        this.esbUserInfoDAO = esbUserInfoDAO;
    }

    public List<NotifyBlackUserInfoVO> listNotifyBlackUsers(Integer start, Integer pageSize) {
        return notifyBlackUserInfoDAO.listNotifyBlackUserInfo(start, pageSize);
    }

    private void saveBlackUsersToDB(String[] users, String creator, List<String> resultList) {
        for (String user : users) {
            if (StringUtils.isBlank(user)) {
                continue;
            }
            notifyBlackUserInfoDAO.insertNotifyBlackUserInfo(
                new NotifyBlackUserInfoDTO(
                    null,
                    user,
                    creator,
                    System.currentTimeMillis()
                ));
            resultList.add(user);
        }
    }

    public List<String> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req) {
        String[] users = req.getUsersStr().split(NotifyConsts.SEPERATOR_COMMA);
        return saveNotifyBlackUsers(username, users);
    }

    public List<String> saveNotifyBlackUsers(String username, String[] users) {
        val resultList = new ArrayList<String>();
        notifyBlackUserInfoDAO.deleteAllNotifyBlackUser();
        saveBlackUsersToDB(users, username, resultList);
        return resultList;
    }

    public Set<String> filterBlackUser(Set<String> userSet) {
        // 过滤黑名单内用户
        Set<String> blackUserSet =
            notifyBlackUserInfoDAO.listNotifyBlackUserInfo().stream()
                .map(NotifyBlackUserInfoDTO::getUsername).collect(Collectors.toSet());
        log.debug(String.format("sendUserChannelNotify:blackUserSet:%s", String.join(",", blackUserSet)));
        val removedBlackUserSet = userSet.stream().filter(blackUserSet::contains).collect(Collectors.toSet());
        userSet = userSet.stream().filter(it -> !blackUserSet.contains(it)).collect(Collectors.toSet());
        log.debug(String.format("sendUserChannelNotify:%d black users are removed, removed users=[%s]",
            removedBlackUserSet.size(), String.join(",", removedBlackUserSet)));
        return userSet;
    }

    private void filterBlackUsers(List<UserVO> userVOList) {
        //过滤黑名单内用户
        Set<String> blackUserSet =
            notifyBlackUserInfoDAO.listNotifyBlackUserInfo().stream()
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
        List<EsbUserInfoDTO> esbUserInfoDTOList = searchUserByPrefix(prefixStr);
        List<UserVO> userVOList = esbUserInfoDTOList.stream().map(it ->
            new UserVO(it.getUsername(), it.getDisplayName(), it.getLogo(), true)
        ).collect(Collectors.toList());
        if (excludeBlackUsers) {
            filterBlackUsers(userVOList);
        }
        return calcFinalListByOffsetAndLimit(userVOList, offset, limit);
    }

    private List<EsbUserInfoDTO> searchUserByPrefix(String prefixStr) {
        // 从数据库查
        if (prefixStr.contains(NotifyConsts.SEPERATOR_COMMA)) {
            // 前端回显，传全量
            List<String> userNames = Arrays.asList(prefixStr.split(NotifyConsts.SEPERATOR_COMMA));
            while (userNames.contains("")) {
                userNames.remove("");
            }
            return esbUserInfoDAO.listEsbUserInfo(userNames);
        } else {
            return esbUserInfoDAO.listEsbUserInfo(prefixStr, -1L);
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
