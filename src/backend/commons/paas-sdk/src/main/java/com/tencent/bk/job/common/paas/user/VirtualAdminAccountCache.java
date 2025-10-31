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

package com.tencent.bk.job.common.paas.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InternalUserManageException;
import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VirtualAdminAccountCache implements IVirtualAdminAccountProvider {
    private final IUserApiClient userApiClient;
    private static final String BK_VIRTUAL_LOGIN_NAME_ADMIN = "bk_admin";

    private final LoadingCache<String, String> adminUsernameCache = CacheBuilder.newBuilder()
        .maximumSize(2000).expireAfterWrite(1, TimeUnit.HOURS)
        .build(new CacheLoader<String, String>() {
            @Override
            public String load(@NonNull String tenantId) {
                log.info("Get admin username of tenantId={}", tenantId);
                return getVirtualAdminFromApi(tenantId);
            }
        });

    public VirtualAdminAccountCache(IUserApiClient userApiClient) {
        this.userApiClient = userApiClient;
    }

    @Override
    public String getVirtualAdminUsername(String tenantId) {
        try {
            return adminUsernameCache.get(tenantId);
        } catch (ExecutionException | UncheckedExecutionException e) {
            String errorMsg = "Fail to load virtual user admin from cache";
            log.error(errorMsg, e);
            Throwable cause = e.getCause();
            if (cause instanceof InternalUserManageException) {
                throw (InternalUserManageException) cause;
            }

            // 不是用户管理返回了空的username，则重试一次
            try {
                log.info("LoadingCache get virtual admin failed, try to fetch userMgrApi directly");
                return getVirtualAdminFromApi(tenantId);
            } catch (Exception ex) {
                String msg = String.format("Fail to get virtual admin of tenant=%s from userMgrApi twice", tenantId);
                log.error(msg, ex);
                throw new InternalException(errorMsg, e, ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    private String getVirtualAdminFromApi(String tenantId) {
        List<SimpleUserInfo> virtualUserList = userApiClient.batchGetVirtualUserByLoginName(
            tenantId,
            BK_VIRTUAL_LOGIN_NAME_ADMIN);
        if (virtualUserList == null
            || virtualUserList.isEmpty()
            || StringUtils.isEmpty(virtualUserList.get(0).getBkUsername())) {
            String errMsg = String.format("Get admin username of tenantId=%s is empty", tenantId);
            log.error(errMsg);
            throw new InternalUserManageException(
                errMsg,
                ErrorCode.BK_USER_VIRTUAL_ADMIN_EMPTY,
                new Object[]{tenantId});
        }
        String username = virtualUserList.get(0).getBkUsername();
        log.info("Admin username of tenantId={} is {}", tenantId, username);
        return username;
    }
}
