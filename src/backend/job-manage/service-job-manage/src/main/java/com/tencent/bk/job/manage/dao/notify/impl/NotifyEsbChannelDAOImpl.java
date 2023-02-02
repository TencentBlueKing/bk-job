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
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.service.PaaSService;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyEsbChannelDAOImpl implements NotifyEsbChannelDAO {

    private static final Logger logger = LoggerFactory.getLogger(NotifyEsbChannelDAOImpl.class);
    private final PaaSService paaSService;
    private final LoadingCache<String, List<NotifyEsbChannelDTO>> esbChannelCache = CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(10, TimeUnit.MINUTES).
            build(new CacheLoader<String, List<NotifyEsbChannelDTO>>() {
                      @Override
                      public List<NotifyEsbChannelDTO> load(@NonNull String searchKey) {
                          logger.info("esbChannelCache searchKey=" + searchKey);
                          List<NotifyEsbChannelDTO> channelDtoList;
                          try {
                              //新增渠道默认为已启用
                              channelDtoList =
                                  paaSService.getAllChannelList("", "100").stream().map(it -> new NotifyEsbChannelDTO(
                                      it.getType(),
                                      it.getLabel(),
                                      it.isActive(),
                                      true,
                                      it.getIcon(),
                                      LocalDateTime.now()
                                  )).collect(Collectors.toList());
                              logger.info(String.format("result.size=%d", channelDtoList.size()));
                              return channelDtoList;
                          } catch (IOException e) {
                              logger.error("updateNotifyEsbChannel: Fail to fetch remote notifyChannel, return", e);
                              return null;
                          }
                      }
                  }
            );

    public NotifyEsbChannelDAOImpl(PaaSService paaSService) {
        this.paaSService = paaSService;
    }

    @Override
    public List<NotifyEsbChannelDTO> listNotifyEsbChannel(DSLContext dslContext) {
        try {
            String lang = JobContextUtil.getUserLang();
            return esbChannelCache.get(lang);
        } catch (ExecutionException | UncheckedExecutionException e) {
            String errorMsg = "Fail to load EsbChannel from cache";
            logger.error(errorMsg, e);
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new InternalException(errorMsg, e, ErrorCode.INTERNAL_ERROR);
            }
        }
    }
}
