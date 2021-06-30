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

package com.tencent.bk.job.manage.dao.notify.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.notify.EsbAppRoleDAO;
import com.tencent.bk.job.manage.service.AppRoleService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Slf4j
@Repository
public class EsbAppRoleDAOMemCacheImpl implements EsbAppRoleDAO {

    private static final Logger logger = LoggerFactory.getLogger(EsbAppRoleDAOMemCacheImpl.class);
    private final AppRoleService roleService;

    private LoadingCache<String, List<AppRoleDTO>> esbAppRoleCache = CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(10, TimeUnit.MINUTES).
            build(new CacheLoader<String, List<AppRoleDTO>>() {
                      @Override
                      public List<AppRoleDTO> load(String lang) throws Exception {
                          logger.info("esbAppRoleCache lang=" + lang);
                          val result = roleService.listAppRoles(lang);
                          logger.info(String.format("result.size=%d", result.size()));
                          return result;
                      }
                  }
            );

    @Autowired
    public EsbAppRoleDAOMemCacheImpl(AppRoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public AppRoleDTO getEsbAppRoleById(DSLContext dslContext, String id) {
        List<AppRoleDTO> appRoleDTOList = null;
        try {
            String lang = JobContextUtil.getUserLang();
            logger.info(String.format("Current Lang:%s", lang));
            appRoleDTOList = esbAppRoleCache.get(lang);
            for (AppRoleDTO appRoleDTO : appRoleDTOList) {
                if (appRoleDTO.getId().equals(id)) {
                    return appRoleDTO;
                }
            }
        } catch (ExecutionException e) {
            log.warn("fail to getEsbAppRoleById", e);
        }
        return null;
    }

    @Override
    public List<AppRoleDTO> listEsbAppRole(DSLContext dslContext) {
        try {
            String lang = JobContextUtil.getUserLang();
            logger.info(String.format("Current Lang:%s", lang));
            return esbAppRoleCache.get(lang);
        } catch (Exception e) {
            logger.error("Fail to get appRole from cache", e);
            return null;
        }
    }
}
