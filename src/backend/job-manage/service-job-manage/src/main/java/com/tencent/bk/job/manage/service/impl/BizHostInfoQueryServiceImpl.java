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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.gse.service.BizHostInfoQueryService;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.manage.service.host.HostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("jobManageBizHostInfoQueryService")
public class BizHostInfoQueryServiceImpl implements BizHostInfoQueryService {

    private final HostService hostService;

    @Autowired
    public BizHostInfoQueryServiceImpl(HostService hostService) {
        this.hostService = hostService;
    }

    @Override
    public Map<Long, Long> queryBizIdsByHostId(Collection<Long> hostIds) {
        List<ApplicationHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ApplicationHostDTO::getHostId, ApplicationHostDTO::getBizId);
    }

    @Override
    public Map<Long, String> queryAgentIdsByHostId(Collection<Long> hostIds) {
        List<ApplicationHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ApplicationHostDTO::getHostId, ApplicationHostDTO::getAgentId);
    }

    @Override
    public Map<Long, String> queryCloudIpsByHostId(Collection<Long> hostIds) {
        List<ApplicationHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ApplicationHostDTO::getHostId, ApplicationHostDTO::getCloudIp);
    }

    private List<ApplicationHostDTO> queryHosts(Collection<Long> hostIds) {
        Set<HostDTO> hostDTOSet = hostIds.stream().map(HostDTO::fromHostId).collect(Collectors.toSet());
        return hostService.listHosts(hostDTOSet);
    }

}
