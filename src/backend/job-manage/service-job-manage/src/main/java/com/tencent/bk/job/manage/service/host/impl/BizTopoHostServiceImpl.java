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

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.service.host.BizHostService;
import com.tencent.bk.job.manage.service.host.BizTopoHostService;
import com.tencent.bk.job.manage.service.impl.topo.BizTopoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BizTopoHostServiceImpl implements BizTopoHostService {

    private final BizTopoService bizTopoService;
    private final BizHostService bizHostService;

    @Autowired
    public BizTopoHostServiceImpl(BizTopoService bizTopoService,
                                  BizHostService bizHostService) {
        this.bizTopoService = bizTopoService;
        this.bizHostService = bizHostService;
    }

    @Override
    public List<ApplicationHostDTO> listHostByNode(Long bizId, BizTopoNode node) {
        List<Long> moduleIds = bizTopoService.findAllModuleIdsOfNodes(bizId, Collections.singletonList(node));
        return bizHostService.getHostsByModuleIds(moduleIds);
    }

    @Override
    public List<ApplicationHostDTO> listHostByNodes(Long bizId, List<BizTopoNode> nodes) {
        List<Long> moduleIds = bizTopoService.findAllModuleIdsOfNodes(bizId, nodes);
        return bizHostService.getHostsByModuleIds(moduleIds);
    }
}
