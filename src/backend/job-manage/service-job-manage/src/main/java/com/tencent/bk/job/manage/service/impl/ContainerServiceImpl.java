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

import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.model.container.KubeTopologyDTO;
import com.tencent.bk.job.common.cc.model.req.ListKubeContainerByTopoReq;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.query.ContainerQuery;
import com.tencent.bk.job.manage.service.ContainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ContainerServiceImpl implements ContainerService {

    private final BizCmdbClient bizCmdbClient;

    @Autowired
    public ContainerServiceImpl(BizCmdbClient bizCmdbClient) {
        this.bizCmdbClient = bizCmdbClient;
    }


    @Override
    public KubeTopologyDTO getBizKubeCacheTopo(long bizId) {
        return bizCmdbClient.getBizKubeCacheTopo(bizId);
    }

    @Override
    public PageData<ContainerDetailDTO> listPageKubeContainerByTopo(ContainerQuery query) {
        return bizCmdbClient.listPageKubeContainerByTopo(query.toListKubeContainerByTopoReq());
    }

    @Override
    public List<ContainerDetailDTO> listKubeContainerByTopo(ContainerQuery query) {
        ListKubeContainerByTopoReq req = query.toListKubeContainerByTopoReq();
        return bizCmdbClient.listKubeContainerByTopo(req);
    }

    @Override
    public List<ContainerDetailDTO> listKubeContainerByIds(Long bizId, List<Long> containerIds) {
        return bizCmdbClient.listKubeContainerByIds(bizId, containerIds);
    }

    @Override
    public List<ContainerDetailDTO> listKubeContainerByUIds(Long bizId, List<String> containerUIds) {
        return bizCmdbClient.listKubeContainerByUIds(bizId, containerUIds);
    }
}
