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

package com.tencent.bk.job.manage.task;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.dao.TenantHostDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台任务：清理DB中的不属于任何业务的无效主机
 */
@Slf4j
@Service
public class ClearNotInCmdbHostsService {


    private final IBizCmdbClient bizCmdbClient;
    private final TenantHostDAO tenantHostDAO;

    @Autowired
    public ClearNotInCmdbHostsService(IBizCmdbClient bizCmdbClient,
                                      TenantHostDAO tenantHostDAO) {
        this.bizCmdbClient = bizCmdbClient;
        this.tenantHostDAO = tenantHostDAO;
    }

    /**
     * 分批检查并删除在CMDB中不存在的主机
     *
     * @param tenantId 租户ID
     */
    public void clearHostNotInCmdb(String tenantId) {
        // 2.分批查询、核验、删除
        long batchSize = 500;
        long start = 0;
        int deletedHostTotalNum = 0;
        List<Long> hostIdList;
        do {
            hostIdList = tenantHostDAO.listHostIds(tenantId, start, batchSize);
            List<ApplicationHostDTO> hosts = bizCmdbClient.listHostsByHostIds(tenantId, hostIdList);
            Set<Long> validHostIds = hosts.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toSet());
            List<Long> hostIdsToBeDelete = hostIdList.stream()
                .filter(hostId -> !validHostIds.contains(hostId))
                .collect(Collectors.toList());
            int deletedHostNum = tenantHostDAO.batchDeleteHostById(tenantId, hostIdsToBeDelete);
            deletedHostTotalNum += deletedHostNum;
            log.info("{} host deleted:{}", deletedHostNum, hostIdsToBeDelete);
            start += batchSize;
        } while (!hostIdList.isEmpty());
        log.info("clearHostNotInCmdb finished, {} hosts deleted", deletedHostTotalNum);
    }

}
