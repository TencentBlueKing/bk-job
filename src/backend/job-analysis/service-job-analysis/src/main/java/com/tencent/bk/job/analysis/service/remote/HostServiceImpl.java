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

package com.tencent.bk.job.analysis.service.remote;

import com.tencent.bk.job.analysis.client.HostResourceClient;
import com.tencent.bk.job.analysis.service.HostService;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByHostReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class HostServiceImpl implements HostService {
    private final HostResourceClient hostResourceClient;

    @Autowired
    public HostServiceImpl(HostResourceClient hostResourceClient) {
        this.hostResourceClient = hostResourceClient;
    }

    @Override
    public List<ServiceHostStatusDTO> getHostStatusByNode(Long appId, List<BizTopoNode> treeNodeList) {
        return hostResourceClient.getHostStatusByNode(appId, new ServiceGetHostStatusByNodeReq(treeNodeList)).getData();
    }

    @Override
    public List<ServiceHostStatusDTO> getHostStatusByDynamicGroup(Long appId, List<String> dynamicGroupIdList) {
        return hostResourceClient.getHostStatusByDynamicGroup(appId,
            new ServiceGetHostStatusByDynamicGroupReq(dynamicGroupIdList)).getData();
    }

    @Override
    public List<ServiceHostStatusDTO> getHostStatusByHost(Long appId,
                                                          List<HostDTO> hostList) {
        return hostResourceClient.getHostStatusByHost(appId,
            new ServiceGetHostStatusByHostReq(hostList)).getData();
    }
}
