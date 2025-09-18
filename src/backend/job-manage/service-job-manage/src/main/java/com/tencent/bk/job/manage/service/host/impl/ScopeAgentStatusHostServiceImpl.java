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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.ScopeAgentStatusHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScopeAgentStatusHostServiceImpl implements ScopeAgentStatusHostService {

    private final ApplicationService applicationService;
    private final CurrentTenantHostDAO currentTenantHostDAO;
    private final NoTenantHostDAO noTenantHostDAO;
    private final HostDetailService hostDetailService;

    @Autowired
    public ScopeAgentStatusHostServiceImpl(ApplicationService applicationService,
                                           CurrentTenantHostDAO currentTenantHostDAO,
                                           NoTenantHostDAO noTenantHostDAO,
                                           HostDetailService hostDetailService) {
        this.applicationService = applicationService;
        this.currentTenantHostDAO = currentTenantHostDAO;
        this.noTenantHostDAO = noTenantHostDAO;
        this.hostDetailService = hostDetailService;
    }

    @Override
    public PageData<ApplicationHostDTO> listHostsByAgentStatus(String username,
                                                               Long appId,
                                                               Integer status,
                                                               Long start,
                                                               Long pageSize) {
        // 查出业务
        ApplicationDTO appInfo = applicationService.getAppByAppId(appId);
        List<Long> bizIds;
        if (appInfo.isBiz()) {
            bizIds = Collections.singletonList(Long.valueOf(appInfo.getScope().getId()));
            return queryCurrentTenantHosts(bizIds, status, start, pageSize);
        } else if (appInfo.isBizSet()) {
            // 业务集
            bizIds = appInfo.getSubBizIds();
            return queryCurrentTenantHosts(bizIds, status, start, pageSize);
        } else if (appInfo.isAllBizSet()) {
            // 全业务
            return queryCurrentTenantHosts(null, status, start, pageSize);
        } else if (appInfo.isAllTenantSet()) {
            // 全租户
            return queryNoTenantHosts(status, start, pageSize);
        } else {
            throw new InternalException("Illegal appInfo:" + appInfo, ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 查询当前租户下的主机
     *
     * @param bizIds   业务ID集合
     * @param status   Agent状态
     * @param start    数据起始位置
     * @param pageSize 数据分页大小
     * @return 主机分页数据
     */
    private PageData<ApplicationHostDTO> queryCurrentTenantHosts(List<Long> bizIds,
                                                                 Integer status,
                                                                 Long start,
                                                                 Long pageSize) {
        List<ApplicationHostDTO> hosts = currentTenantHostDAO.listHostInfoBySearchContents(
            bizIds,
            null,
            null,
            null,
            status,
            start,
            pageSize
        );
        Long count = currentTenantHostDAO.countHostInfoBySearchContents(
            bizIds,
            null,
            null,
            null,
            status
        );
        String tenantId = JobContextUtil.getTenantId();
        hostDetailService.fillDetailForApplicationHosts(tenantId, hosts);
        return new PageData<>(start.intValue(), pageSize.intValue(), count, hosts);
    }

    /**
     * 查询所有租户下的主机
     *
     * @param status   Agent状态
     * @param start    数据起始位置
     * @param pageSize 数据分页大小
     * @return 主机分页数据
     */
    private PageData<ApplicationHostDTO> queryNoTenantHosts(Integer status,
                                                            Long start,
                                                            Long pageSize) {
        List<ApplicationHostDTO> hosts = noTenantHostDAO.listHostInfoBySearchContents(
            null,
            null,
            null,
            null,
            status,
            start,
            pageSize
        );
        Long count = noTenantHostDAO.countHostInfoBySearchContents(
            null,
            null,
            null,
            null,
            status
        );
        Map<String, List<ApplicationHostDTO>> tenantHostMap = hosts.stream()
            .collect(Collectors.groupingBy(ApplicationHostDTO::getTenantId));
        hostDetailService.fillDetailForTenantHosts(tenantHostMap);
        return new PageData<>(start.intValue(), pageSize.intValue(), count, hosts);
    }
}
